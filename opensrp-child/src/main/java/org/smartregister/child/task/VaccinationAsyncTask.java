package org.smartregister.child.task;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.domain.RegisterActionParams;
import org.smartregister.child.util.ChildAppProperties;
import org.smartregister.child.util.Constants;
import org.smartregister.child.wrapper.VaccineViewRecordUpdateWrapper;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Alert;
import org.smartregister.domain.AlertStatus;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.GroupVaccineCount;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.service.AlertService;
import org.smartregister.util.Utils;
import org.smartregister.view.contract.SmartRegisterClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.joda.time.DateTimeConstants.MILLIS_PER_DAY;
import static org.smartregister.immunization.util.VaccinatorUtils.nextVaccineDue;
import static org.smartregister.immunization.util.VaccinatorUtils.receivedVaccines;
import static org.smartregister.util.Utils.getValue;

/**
 * Created by ndegwamartin on 05/03/2019.
 */
public class VaccinationAsyncTask extends AsyncTask<Void, Void, Void> {
    public final static String LINE_SEPARATOR = System.getProperty("line.separator");
    private final View convertView;
    private final String entityId;
    private final String dobString;
    private final String lostToFollowUp;
    private final String inactive;
    private List<Vaccine> vaccines = new ArrayList<>();
    private SmartRegisterClient client;
    private Map<String, Object> nv = null;
    private VaccineRepository vaccineRepository;
    private CommonRepository commonRepository;
    private Context context;
    private Boolean updateOutOfCatchment;
    private View.OnClickListener onClickListener;
    private AlertService alertService;
    private View childProfileInfoLayout;
    private boolean isLegacyAlerts = ChildLibrary.getInstance().getProperties().hasProperty(ChildAppProperties.KEY.HOME_ALERT_STYLE_LEGACY) && ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.HOME_ALERT_STYLE_LEGACY);
    private boolean upcomingLightBlueDisabled = ChildLibrary.getInstance().getProperties().hasProperty(ChildAppProperties.KEY.HOME_ALERT_UPCOMING_BLUE_DISABLED) && ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.HOME_ALERT_UPCOMING_BLUE_DISABLED);
    private Map<String, String> reverseLookupGroupMap;
    private Map<String, GroupVaccineCount> groupVaccineCountMap;
    protected String IS_GROUP_PARTIAL = "isGroupPartial";
    private Date lastVaccineDate = null;

    public VaccinationAsyncTask(RegisterActionParams recordActionParams, CommonRepository commonRepository,
                                VaccineRepository vaccineRepository, AlertService alertService, Context context) {

        this.convertView = recordActionParams.getConvertView();
        this.entityId = recordActionParams.getEntityId();
        this.dobString = recordActionParams.getDobString();
        this.lostToFollowUp = recordActionParams.getLostToFollowUp();
        this.inactive = recordActionParams.getInactive();
        this.client = recordActionParams.getSmartRegisterClient();
        this.updateOutOfCatchment = recordActionParams.getUpdateOutOfCatchment();
        this.onClickListener = recordActionParams.getOnClickListener();
        this.vaccineRepository = vaccineRepository;
        this.commonRepository = commonRepository;
        this.context = context;
        this.alertService = alertService;
        this.childProfileInfoLayout = recordActionParams.getProfileInfoView();
        this.reverseLookupGroupMap = ImmunizationLibrary.getInstance().getVaccineCacheMap().get(Constants.CHILD_TYPE).reverseLookupGroupMap;
        this.groupVaccineCountMap = ImmunizationLibrary.getInstance().getVaccineCacheMap().get(Constants.CHILD_TYPE).groupVaccineCountMap;

        // Add BCG 2 to Birth Vaccination group
        // This method handles the multiple loops
        this.reverseLookupGroupMap.put(Constants.VACCINE.BCG2, Constants.VACCINE_GROUP.BIRTH);
    }

    @Override
    protected Void doInBackground(Void... params) {

        executeInBackground();
        return null;
    }

    private void executeInBackground() {
        vaccines = vaccineRepository.findByEntityId(entityId);

        List<Alert> alerts = alertService.findByEntityId(entityId);

        Map<String, Date> receivedVaccines = receivedVaccines(vaccines);

        DateTime dateTime = Utils.dobStringToDateTime(dobString);
        List<Map<String, Object>> sch = VaccinatorUtils.generateScheduleList(Constants.KEY.CHILD, dateTime, receivedVaccines, alerts);
        List<String> receivedVaccinesList = new ArrayList<>();
        String key;

        for (Map.Entry<String, Date> entry : receivedVaccines.entrySet()) {

            key = entry.getKey();
            key = key.contains("/") ? key.substring(0, key.indexOf("/")) : key;
            key = key.trim().replaceAll(" ", "").toLowerCase();
            receivedVaccinesList.add(key);
            String groupVaccineMapKey = reverseLookupGroupMap.get(key) == null ? "" : reverseLookupGroupMap.get(key);
            if (groupVaccineMapKey != null) {
                GroupVaccineCount groupVaccineCount = groupVaccineCountMap.get(groupVaccineMapKey);
                if (groupVaccineCount != null) {
                    groupVaccineCount.setRemaining(groupVaccineCount.getRemaining() - 1);
                    groupVaccineCountMap.put(groupVaccineMapKey, groupVaccineCount);
                }
            }
        }

        if (vaccines.isEmpty()) {
            nv = nextVaccineDue(sch, ImmunizationLibrary.getVaccineCacheMap().get(Constants.CHILD_TYPE).vaccineRepo);
        }

        if (nv == null) {
            if (!vaccines.isEmpty()) {
                Vaccine vaccine = vaccines.get(vaccines.size() - 1);
                lastVaccineDate = vaccine.getDate();
            }

            nv = nextVaccineDue(sch, lastVaccineDate);
            if (nv != null && nv.containsKey(Constants.KEY.VACCINE)) {
                nv.put(IS_GROUP_PARTIAL, getIsGroupPartial(nv.get(Constants.KEY.VACCINE).toString().toLowerCase()));
            }

        }
    }

    private Boolean getIsGroupPartial(String vaccine) {
        GroupVaccineCount groupVaccineCount = groupVaccineCountMap.get(reverseLookupGroupMap.get(vaccine));
        return (groupVaccineCount != null) && groupVaccineCount.getGiven() != groupVaccineCount.getRemaining();
    }

    @Override
    protected void onPostExecute(Void param) {

        VaccineViewRecordUpdateWrapper wrapper = new VaccineViewRecordUpdateWrapper();
        wrapper.setVaccines(vaccines);
        wrapper.setLostToFollowUp(lostToFollowUp);
        wrapper.setInactive(inactive);
        wrapper.setClient(client);
        wrapper.setConvertView(convertView);
        wrapper.setNv(nv);
        updateRecordVaccination(wrapper);

    }

    private void updateRecordVaccination(VaccineViewRecordUpdateWrapper updateWrapper) {
        View recordVaccination = updateWrapper.getConvertView().findViewById(R.id.record_vaccination);
        recordVaccination.setVisibility(View.VISIBLE);

        TextView recordVaccinationText = updateWrapper.getConvertView().findViewById(R.id.record_vaccination_text);
        recordVaccinationText.setAllCaps(false);

        ImageView recordVaccinationCheck = updateWrapper.getConvertView().findViewById(R.id.record_vaccination_check);
        recordVaccinationCheck.setVisibility(View.GONE);

        ImageView recordVaccinationHarveyBall = updateWrapper.getConvertView().findViewById(R.id.record_vaccination_harvey_ball);
        recordVaccinationHarveyBall.setVisibility(View.GONE);

        ((LinearLayout) recordVaccinationCheck.getParent()).setOrientation(LinearLayout.HORIZONTAL);

        State state = State.WAITING;
        String groupName = "";

        Map<String, Object> nv = updateWrapper.getNv();
        DateTime dueDate = null;
        if (nv != null) {


            Object dueDateRawObject = nv.get(Constants.KEY.DATE);
            dueDate = dueDateRawObject != null && dueDateRawObject instanceof DateTime ? (DateTime) dueDateRawObject : null;

            if (nv.get(Constants.KEY.VACCINE) != null && nv.get(Constants.KEY.VACCINE) instanceof VaccineRepo.Vaccine) {
                VaccineRepo.Vaccine vaccine = (VaccineRepo.Vaccine) nv.get(Constants.KEY.VACCINE);
                groupName = org.smartregister.immunization.util.Utils.getGroupName(vaccine, Constants.CHILD_TYPE);
            }

            Alert alert = null;
            if (nv.get(Constants.KEY.ALERT) != null && nv.get(Constants.KEY.ALERT) instanceof Alert) {
                alert = (Alert) nv.get(Constants.KEY.ALERT);
            }

            if (alert == null) {
                state = lastVaccineDate != null && Math.abs(lastVaccineDate.getTime() - Calendar.getInstance().getTimeInMillis()) > MILLIS_PER_DAY ? getUpcomingState(dueDate) : State.NO_ALERT;
            } else if (AlertStatus.normal.equals(alert.status())) {
                state = State.DUE;
            } else if (AlertStatus.upcoming.equals(alert.status())) {
                state = getUpcomingState(dueDate);
            } else if (AlertStatus.urgent.equals(alert.status())) {
                state = State.OVERDUE;
            } else if (AlertStatus.expired.equals(alert.status())) {
                state = State.EXPIRED;
            }
        }

        TextView nextAppointmentDate = convertView.findViewById(R.id.child_next_appointment);

        if (nextAppointmentDate != null) {
            SimpleDateFormat UI_DF = new SimpleDateFormat(com.vijay.jsonwizard.utils.FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN, Locale.ENGLISH);

            if (dueDate != null) {
                String nextAppointment = UI_DF.format(dueDate.toDate());
                nextAppointmentDate.setText(nextAppointment);
                childProfileInfoLayout.setTag(R.id.next_appointment_date, nextAppointment);
            }
        }

        // Check for fully immunized child
        if (nv == null && updateWrapper.getVaccines() != null && !updateWrapper.getVaccines().isEmpty()) {
            state = State.FULLY_IMMUNIZED;
        }

        // Update active/inactive/lostToFollowup status
        if (updateWrapper.getLostToFollowUp().equals(Boolean.TRUE.toString())) {
            state = State.LOST_TO_FOLLOW_UP;
        }

        if (updateWrapper.getInactive().equals(Boolean.TRUE.toString())) {
            state = State.INACTIVE;
        }

        if (state.equals(State.FULLY_IMMUNIZED)) {
            recordVaccinationText.setText(R.string.fully_immunized_label);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            if (isLegacyAlerts) {
                recordVaccinationCheck.setImageResource(R.drawable.ic_action_check);
                recordVaccinationCheck.setVisibility(View.VISIBLE);
            } else {
                ((LinearLayout) recordVaccinationCheck.getParent()).setOrientation(LinearLayout.VERTICAL);
                recordVaccinationCheck.setImageResource(R.drawable.ic_harvey_100);
                recordVaccinationCheck.setVisibility(View.VISIBLE);

            }

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);

        } else if (state.equals(State.INACTIVE)) {
            recordVaccinationText.setText(R.string.inactive);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccinationCheck.setImageResource(R.drawable.ic_icon_status_inactive);
            recordVaccinationCheck.setVisibility(View.VISIBLE);

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);


        } else if (state.equals(State.LOST_TO_FOLLOW_UP)) {
            recordVaccinationText.setText(R.string.lost_to_follow_up_with_nl);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccinationCheck.setImageResource(R.drawable.ic_icon_status_losttofollowup);
            recordVaccinationCheck.setVisibility(View.VISIBLE);

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);

        } else if (state.equals(State.WAITING)) {
            recordVaccinationText.setText(R.string.waiting_label);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else if (state.equals(State.EXPIRED)) {
            recordVaccinationText.setText(R.string.expired_label);
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else if (state.equals(State.UPCOMING)) {
            recordVaccinationText
                    .setText(context.getString(R.string.upcoming_label) + LINE_SEPARATOR + org.smartregister.child.util.Utils.localizeStateKey(context, groupName));
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else if (state.equals(State.UPCOMING_NEXT_7_DAYS)) {
            recordVaccinationText
                    .setText(context.getString(R.string.upcoming_label) + LINE_SEPARATOR + org.smartregister.child.util.Utils.localizeStateKey(context, groupName));
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));
            if (!upcomingLightBlueDisabled) {
                recordVaccination.setBackground(context.getResources().getDrawable(R.drawable.due_vaccine_light_blue_bg));
            } else {
                recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            }
            recordVaccination.setEnabled(true);
        } else if (state.equals(State.DUE)) {

            recordVaccinationText.setText(getAlertMessage(state, groupName));
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.status_bar_text_almost_white));
            recordVaccination.setBackground(context.getResources().getDrawable(R.drawable.due_vaccine_blue_bg));
            recordVaccination.setEnabled(true);
            recordVaccinationText.setAllCaps(!isLegacyAlerts ? true : false);

            if (nv != null && nv.get(IS_GROUP_PARTIAL) != null && (Boolean) nv.get(IS_GROUP_PARTIAL) && !isLegacyAlerts && (lastVaccineDate != null && Math.abs(lastVaccineDate.getTime() - Calendar.getInstance().getTimeInMillis()) < MILLIS_PER_DAY)) {

                ((LinearLayout) recordVaccinationCheck.getParent()).setOrientation(LinearLayout.VERTICAL);
                recordVaccinationHarveyBall.setImageResource(R.drawable.ic_harvey_75);
                recordVaccinationHarveyBall.setVisibility(View.VISIBLE);
                recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
                recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));
                recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));


            }
        } else if (state.equals(State.OVERDUE)) {

            recordVaccinationText.setText(getAlertMessage(state, groupName));
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.status_bar_text_almost_white));
            recordVaccinationText.setAllCaps(!isLegacyAlerts ? true : false);

            recordVaccination.setBackground(context.getResources().getDrawable(R.drawable.due_vaccine_red_bg));
            recordVaccination.setEnabled(true);
        } else if (state.equals(State.NO_ALERT)) {
            if (StringUtils.isNotBlank(groupName) && (StringUtils.containsIgnoreCase(groupName, Constants.KEY.WEEK) || StringUtils.containsIgnoreCase(groupName, Constants.KEY.MONTH)) &&
                    !updateWrapper.getVaccines().isEmpty()) {
                Vaccine vaccine = updateWrapper.getVaccines().isEmpty() ? null : updateWrapper.getVaccines().get(updateWrapper.getVaccines().size() - 1);

                String previousStateKey = previousStateKey(vaccine);

                String alertStateKey = !TextUtils.isEmpty(previousStateKey) ? previousStateKey : groupName;

                recordVaccinationText.setText(getAlertMessage(state, alertStateKey));

                if (isLegacyAlerts) {
                    recordVaccinationCheck.setImageResource(R.drawable.ic_action_check);
                    recordVaccinationCheck.setVisibility(View.VISIBLE);
                } else {
                    ((LinearLayout) recordVaccinationCheck.getParent()).setOrientation(LinearLayout.VERTICAL);
                    recordVaccinationCheck.setImageResource(R.drawable.ic_harvey_100);
                    recordVaccinationCheck.setVisibility(View.VISIBLE);

                }
            } else {
                recordVaccinationText.setText(context.getString(R.string.upcoming_label) + LINE_SEPARATOR + org.smartregister.child.util.Utils.localizeStateKey(context, groupName));
            }

            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));
            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        } else {
            recordVaccinationText.setText("");
            recordVaccinationText.setTextColor(context.getResources().getColor(R.color.client_list_grey));

            recordVaccination.setBackgroundColor(context.getResources().getColor(R.color.white));
            recordVaccination.setEnabled(false);
        }

        //Update Out of Catchment
        if (updateOutOfCatchment) {
            updateViews(updateWrapper.getConvertView(), updateWrapper.getClient());
        }
    }

    private String previousStateKey(Vaccine vaccine) {
        return reverseLookupGroupMap.get(vaccine.getName().toLowerCase(Locale.ENGLISH).replace(" ", ""));
    }

    @NonNull
    private VaccinationAsyncTask.State getUpcomingState(DateTime dueDate) {

        State state;
        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);


            if (dueDate != null &&
                    dueDate.getMillis() >= (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)) &&
                    dueDate.getMillis() < (today.getTimeInMillis() + TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS))) {
                state = State.UPCOMING_NEXT_7_DAYS;
            } else {
                state = State.UPCOMING;
            }
        } catch (Exception e) {
            state = State.NO_ALERT;//fallback old behaviour
        }
        return state;
    }

    protected void updateViews(View catchmentView, SmartRegisterClient client) {

        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        if (commonRepository != null) {
            CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(pc.entityId());

            View recordVaccination = catchmentView.findViewById(R.id.record_vaccination);
            recordVaccination.setVisibility(View.VISIBLE);

            View moveToCatchment = catchmentView.findViewById(R.id.move_to_catchment);
            moveToCatchment.setVisibility(View.GONE);

            if (commonPersonObject == null) { //Out of area -- doesn't exist in local database

                catchmentView.findViewById(R.id.child_profile_info_layout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Utils.showShortToast(context, context.getString(R.string.show_vaccine_card_disabled));
                    }
                });

                TextView moveToCatchmentText = catchmentView.findViewById(R.id.move_to_catchment_text);
                moveToCatchmentText.setText(context.getString(R.string.move_to_catchment));

                //Use child relation id as mothers base entity id or look for mother base entity id
                String motherBaseEntityIdValue = getValue(pc.getColumnmaps(), Constants.KEY.MOTHER_BASE_ENTITY_ID, false);
                String motherBaseEntityId = StringUtils.isNoneBlank(motherBaseEntityIdValue) ? motherBaseEntityIdValue : getValue(pc.getColumnmaps(), Constants.KEY.RELATIONAL_ID, false);
                String fatherBaseEntityId = getValue(pc.getColumnmaps(), Constants.KEY.FATHER_BASE_ENTITY_ID, false);
                String entityId = pc.entityId();

                List<String> ids = new ArrayList<>();
                if(StringUtils.isNoneBlank(motherBaseEntityId) && StringUtils.isNoneBlank(entityId)) {
                    ids.add(motherBaseEntityId);
                    ids.add(entityId);
                }
                //Also include Father base entity Id to pull Father events
                if (ChildLibrary.getInstance().metadata().childRegister.getFatherRelationKey() != null && StringUtils.isNoneBlank(fatherBaseEntityId)) {
                    ids.add(fatherBaseEntityId);
                }

                moveToCatchment.setBackground(context.getResources().getDrawable(R.drawable.record_growth_bg));
                moveToCatchment.setTag(R.id.move_to_catchment_ids, ids);
                moveToCatchment.setClickable(true);
                moveToCatchment.setEnabled(true);
                moveToCatchment.setOnClickListener(onClickListener);

                moveToCatchment.setVisibility(View.VISIBLE);
                recordVaccination.setVisibility(View.GONE);
            }

        }
    }

    @VisibleForTesting
    enum State {
        DUE, OVERDUE, UPCOMING_NEXT_7_DAYS, UPCOMING, INACTIVE, LOST_TO_FOLLOW_UP, EXPIRED, WAITING, NO_ALERT,
        FULLY_IMMUNIZED
    }

    protected String getAlertMessage(State state, String stateKey) {

        String message;
        switch (state) {
            case DUE:
                String due = stateKey != null ? context.getString(R.string.n_period_due, org.smartregister.child.util.Utils.localizeStateKey(context, stateKey)) : context.getString(R.string.due);
                message = isLegacyAlerts ? context.getString(R.string.record_label) + LINE_SEPARATOR + org.smartregister.child.util.Utils.localizeStateKey(context, stateKey) : due;
                break;

            case OVERDUE:
                String overdue = stateKey != null ? context.getString(R.string.n_period_overdue, org.smartregister.child.util.Utils.localizeStateKey(context, stateKey)) : context.getString(R.string.overdue);
                message = isLegacyAlerts ? context.getString(R.string.record_label) + LINE_SEPARATOR + org.smartregister.child.util.Utils.localizeStateKey(context, stateKey) : overdue;
                break;

            case NO_ALERT:
                message = isLegacyAlerts ? org.smartregister.child.util.Utils.localizeStateKey(context, stateKey) : context.getString(R.string.done_today);
                break;

            default:
                message = context.getString(R.string.done);
                break;

        }

        return message;
    }

}