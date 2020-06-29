package org.smartregister.child.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.widget.NestedScrollView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.smartregister.child.R;
import org.smartregister.child.activity.BaseChildDetailTabbedActivity;
import org.smartregister.child.contract.IChildDetails;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.child.view.WidgetFactory;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.domain.Photo;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.growthmonitoring.domain.Height;
import org.smartregister.growthmonitoring.domain.HeightWrapper;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.domain.WeightWrapper;
import org.smartregister.growthmonitoring.fragment.EditGrowthDialogFragment;
import org.smartregister.growthmonitoring.util.AppProperties;
import org.smartregister.growthmonitoring.util.HeightUtils;
import org.smartregister.growthmonitoring.util.WeightUtils;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceType;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.fragment.ServiceEditDialogFragment;
import org.smartregister.immunization.fragment.VaccinationEditDialogFragment;
import org.smartregister.immunization.util.ImageUtils;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.immunization.view.ImmunizationRowGroup;
import org.smartregister.immunization.view.ServiceRowGroup;
import org.smartregister.util.DateUtil;
import org.smartregister.view.customcontrols.CustomFontTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static org.smartregister.child.util.Utils.createGroupNameTextView;
import static org.smartregister.immunization.util.VaccinatorUtils.dpToPx;
import static org.smartregister.util.Utils.convertDateFormat;

/**
 * Created by ndegwamartin on 06/03/2019.
 */
public class ChildUnderFiveFragment extends Fragment {
    private static final String DIALOG_TAG = "ChildImmunoActivity_DIALOG_TAG";
    private static Boolean monitorGrowth = false;
    private CommonPersonObjectClient childDetails;
    private Map<String, String> detailsMap;
    private LinearLayout fragmentContainer;
    private View heightWidgetLayout;
    private Boolean curVaccineMode;
    private Boolean curServiceMode;
    private LinearLayout serviceGroupCanvasLL;
    private LinearLayout extraVaccinesLayout;
    private boolean showRecurringServices = true;
    private List<Map.Entry<String, String>> extraVaccines;

    public ChildUnderFiveFragment() {
        // Required empty public constructor
    }

    public static ChildUnderFiveFragment newInstance(Bundle bundle) {
        Bundle args = bundle;
        ChildUnderFiveFragment fragment = new ChildUnderFiveFragment();
        if (args == null) {
            args = new Bundle();
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        monitorGrowth = GrowthMonitoringLibrary.getInstance().getAppProperties().hasProperty(AppProperties.KEY.MONITOR_GROWTH) && GrowthMonitoringLibrary.getInstance().getAppProperties().getPropertyBoolean(AppProperties.KEY.MONITOR_GROWTH);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (getActivity() instanceof IChildDetails) {
            childDetails = ((IChildDetails) getActivity()).getChildDetails();
            detailsMap = childDetails.getColumnmaps();
        }

        View underFiveFragment = inflater.inflate(R.layout.child_under_five_fragment, container, false);
        fragmentContainer = underFiveFragment.findViewById(R.id.container);
        serviceGroupCanvasLL = fragmentContainer.findViewById(R.id.services);
        extraVaccinesLayout = fragmentContainer.findViewById(R.id.extra_vaccines);

        if (monitorGrowth) {
            heightWidgetLayout = underFiveFragment.findViewById(R.id.height_widget_layout);
            heightWidgetLayout.setVisibility(View.VISIBLE);
        }

        Utils.refreshDataCaptureStrategyBanner(this.getActivity(), ((BaseChildDetailTabbedActivity) this.getActivity()).getOpenSRPContext().allSharedPreferences().fetchCurrentLocality());

        updateExtraVaccinesView();

        return underFiveFragment;
    }

    public void setDetailsMap(Map<String, String> detailsMap) {
        this.detailsMap = detailsMap;
        if (getActivity() instanceof IChildDetails) {
            ((IChildDetails) getActivity()).setChildDetails(this.detailsMap);
        }
    }


    public void showRecurringServices(boolean showRecurringServices) {
        this.showRecurringServices = showRecurringServices;
    }

    public void loadGrowthMonitoringView(List<Weight> weightList, List<Height> heightList, boolean editGrowthMonitoringMode) {

        if (fragmentContainer != null) {
            createGrowthLayout(weightList, heightList, fragmentContainer, editGrowthMonitoringMode);
        }
    }

    private void createGrowthLayout(List<Weight> weights, List<Height> heights, LinearLayout fragmentContainer,
                                    boolean editMode) {
        WidgetFactory widgetFactory = new WidgetFactory();
        ArrayList<View.OnClickListener> listeners = new ArrayList<>();

        ArrayList<Boolean> weightEditMode = new ArrayList<>();
        List<Weight> weightList = getWeights(weights);

        // Sort the weights
        sortTheWeightsInDescendingOrder(weightList);

        LinkedHashMap<Long, Pair<String, String>> weightMap = updateWeightMap(editMode, weightEditMode, listeners, weightList);


        if (weightMap.size() > 0) {
            widgetFactory.createWeightWidget(getActivity().getLayoutInflater(), fragmentContainer, weightMap, listeners, weightEditMode);
        }

        if (monitorGrowth) {
            ArrayList<Boolean> heightEditMode = new ArrayList<>();
            List<Height> heightList = getHeights(heights);

            // Sort the heights
            sortTheHeightsInDescendingOrder(heightList);
            ArrayList<View.OnClickListener> heightListeners = new ArrayList<>();

            LinkedHashMap<Long, Pair<String, String>> heightMap =
                    updateHeightMap(editMode, heightEditMode, heightListeners, heightList);
            if (heightMap.size() > 0) {
                widgetFactory.createHeightWidget(getActivity().getLayoutInflater(), fragmentContainer, heightMap, heightListeners, heightEditMode);
            }
        }

        ((NestedScrollView) ((View) fragmentContainer.getParent()).findViewById(R.id.scrollView)).smoothScrollTo(0, 0);
    }

    private void sortTheWeightsInDescendingOrder(List<Weight> weightList) {
        Collections.sort(weightList, new Comparator<Weight>() {
            @Override
            public int compare(Weight o1, Weight o2) {
                return (o1 != null && o2 != null && o2.getDate() != null) ? o2.getDate().compareTo(o1.getDate()) : 0;
            }
        });
    }

    private void sortTheHeightsInDescendingOrder(List<Height> heightList) {
        Collections.sort(heightList, new Comparator<Height>() {
            @Override
            public int compare(Height o1, Height o2) {
                return (o1 != null && o2 != null && o2.getDate() != null) ? o2.getDate().compareTo(o1.getDate()) : 0;
            }
        });
    }

    private LinkedHashMap<Long, Pair<String, String>> updateWeightMap(boolean editMode, ArrayList<Boolean> weightEditMode, ArrayList<View.OnClickListener> listeners, List<Weight> weightList) {
        LinkedHashMap<Long, Pair<String, String>> weightMap = new LinkedHashMap<>();
        for (int i = 0; i < weightList.size(); i++) {
            Weight weight = weightList.get(i);
            String formattedAge = "";
            if (weight.getDate() != null) {
                Date weightDate = weight.getDate();
                String birthDate = Utils.getValue(detailsMap, Constants.KEY.DOB, false);
                Date birth = Utils.dobStringToDate(birthDate);
                if (birth != null) {
                    long timeDiff = Math.abs(weightDate.getTime() - birth.getTime());
                    Timber.v("%s", timeDiff);
                    if (timeDiff >= 0) {
                        formattedAge = DateUtil.getDuration(timeDiff, Locale.ENGLISH);
                        Timber.v(formattedAge);
                    }
                }
            }

            weightMap.put(weight.getId() - 1, Pair.create(formattedAge, Utils.kgStringSuffix(Utils.formatNumber(String.valueOf(weight.getKg())))));

            boolean lessThanThreeMonthsEventCreated = WeightUtils.lessThanThreeMonths(weight);
            weightEditMode.add(lessThanThreeMonthsEventCreated && editMode && !formattedAge.startsWith("0"));

            final long weightTaken = weight.getDate().getTime();
            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGrowthMonitoringDialog(weightTaken);
                }
            };
            listeners.add(onClickListener);
        }


        return weightMap;
    }

    private LinkedHashMap<Long, Pair<String, String>> updateHeightMap(boolean editMode, ArrayList<Boolean> heightEditMode,
                                                                      ArrayList<View.OnClickListener> listeners,
                                                                      List<Height> heightList) {
        LinkedHashMap<Long, Pair<String, String>> heightMap = new LinkedHashMap<>();
        for (int i = 0; i < heightList.size(); i++) {
            Height height = heightList.get(i);
            String formattedAge = "";
            if (height.getDate() != null) {
                Date heightDate = height.getDate();
                Date birth = getBirthDate();
                if (birth != null) {
                    long timeDiff = Math.abs(heightDate.getTime() - birth.getTime());
                    Timber.v("%s", timeDiff);
                    if (timeDiff >= 0) {
                        formattedAge = DateUtil.getDuration(timeDiff, Locale.ENGLISH);
                        Timber.v(formattedAge);
                    }
                }
            }

            heightMap.put(height.getId() - 1, Pair.create(formattedAge, Utils.cmStringSuffix(height.getCm())));

            boolean lessThanThreeMonthsEventCreated = HeightUtils.lessThanThreeMonths(height);
            heightEditMode.add(lessThanThreeMonthsEventCreated && editMode && !formattedAge.startsWith("0"));

            final long heightTaken = height.getDate().getTime();
            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGrowthMonitoringDialog(heightTaken);
                }
            };
            listeners.add(onClickListener);


        }

        return heightMap;
    }

    @NotNull
    private List<Weight> getWeights(List<Weight> weights) {
        List<Weight> weightList = new ArrayList<>();
        List<Weight> formattedWeights = getChildSpecificWeights(weights);
        if (!formattedWeights.isEmpty()) {
            if (formattedWeights.size() <= 5) {
                weightList = formattedWeights;
            } else {
                weightList = formattedWeights.subList(0, 5);
            }
        }

        return weightList;
    }

    @NotNull
    private List<Height> getHeights(List<Height> heights) {
        List<Height> heightList = new ArrayList<>();
        List<Height> formattedHeights = getChildSpecificHeights(heights);
        if (!formattedHeights.isEmpty()) {
            if (heights.size() <= 5) {
                heightList = heights;
            } else {
                heightList = heights.subList(0, 5);
            }
        }

        return heightList;
    }

    private List<Height> getChildSpecificHeights(List<Height> heights) {
        List<Height> heightList = new ArrayList<>();
        for (Height height : heights) {
            if (height.getBaseEntityId().equals(childDetails.getCaseId())) {
                heightList.add(height);
            }
        }

        return heightList;
    }

    private List<Weight> getChildSpecificWeights(List<Weight> weights) {
        List<Weight> weightList = new ArrayList<>();
        for (Weight height : weights) {
            if (height.getBaseEntityId().equals(childDetails.getCaseId())) {
                weightList.add(height);
            }
        }

        return weightList;
    }

    public void updateVaccinationViews(List<Vaccine> vaccines, List<Alert> alertList, boolean editVaccineMode) {
        boolean showVaccine = curVaccineMode == null || !curVaccineMode.equals(editVaccineMode);
        if (fragmentContainer != null && showVaccine) {

            List<Vaccine> vaccineList = new ArrayList<>();
            if (vaccines != null && !vaccines.isEmpty()) {
                vaccineList = vaccines;
            }

            LinearLayout vaccineGroupCanvasLL = fragmentContainer.findViewById(R.id.immunizations);
            vaccineGroupCanvasLL.removeAllViews();
            vaccineGroupCanvasLL.addView(getSectionTitle(R.color.text_black, R.string.immunizations));

            boolean addedBcg2Vaccine = false;
            List<VaccineGroup> supportedVaccines = VaccinatorUtils.getSupportedVaccines(getActivity());
            for (VaccineGroup vaccineGroup : supportedVaccines) {

                if (!addedBcg2Vaccine) {
                    addedBcg2Vaccine = VaccinateActionUtils.addBcg2SpecialVaccine(getActivity(), vaccineGroup, vaccineList);
                }

                ImmunizationRowGroup curGroup = new ImmunizationRowGroup(getActivity(), editVaccineMode);
                curGroup.setData(vaccineGroup, childDetails, vaccineList, alertList);
                curGroup.setOnVaccineUndoClickListener(new ImmunizationRowGroup.OnVaccineUndoClickListener() {
                    @Override
                    public void onUndoClick(ImmunizationRowGroup vaccineGroup, VaccineWrapper vaccine) {
                        addVaccinationDialogFragment(Arrays.asList(vaccine), vaccineGroup);
                    }
                });

                TextView groupNameTextView = createGroupNameTextView(getActivity(), vaccineGroup.name);
                vaccineGroupCanvasLL.addView(groupNameTextView);
                vaccineGroupCanvasLL.addView(curGroup);
            }

            curVaccineMode = editVaccineMode;
        }
    }

    private void addVaccinationDialogFragment(List<VaccineWrapper> vaccineWrappers, ImmunizationRowGroup vaccineGroup) {
        if (getActivity() != null) {
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            Date dob = getBirthDate();
            if (dob == null) {
                dob = Calendar.getInstance().getTime();
            }

            List<Vaccine> vaccineList =
                    ImmunizationLibrary.getInstance().vaccineRepository().findByEntityId(childDetails.entityId());
            if (vaccineList == null) {
                vaccineList = new ArrayList<>();
            }

            VaccinationEditDialogFragment vaccinationDialogFragment = VaccinationEditDialogFragment
                    .newInstance(getActivity(), dob, vaccineList, vaccineWrappers, vaccineGroup, true);
            vaccinationDialogFragment.show(ft, DIALOG_TAG);
        }
    }

    private Date getBirthDate() {
        String birthDate = Utils.getValue(childDetails.getColumnmaps(), Constants.KEY.DOB, false);
        return Utils.dobStringToDate(birthDate);
    }

    public void updateServiceViews(Map<String, List<ServiceType>> serviceTypeMap, List<ServiceRecord> services,
                                   List<Alert> alertList, boolean editServiceMode) {
        boolean showService = curServiceMode == null || !curServiceMode.equals(editServiceMode);
        if (fragmentContainer != null && showService) {

            List<ServiceRecord> serviceRecords = new ArrayList<>();
            if (services != null && !services.isEmpty()) {
                serviceRecords = services;
            }

            serviceGroupCanvasLL.removeAllViews();
            serviceGroupCanvasLL.addView(getSectionTitle(R.color.black, R.string.recurring));

            try {
                for (String type : serviceTypeMap.keySet()) {
                    ServiceRowGroup curGroup = new ServiceRowGroup(getActivity(), editServiceMode);
                    curGroup.setData(childDetails, serviceTypeMap.get(type), serviceRecords, alertList);
                    curGroup.setOnServiceUndoClickListener(new ServiceRowGroup.OnServiceUndoClickListener() {
                        @Override
                        public void onUndoClick(ServiceRowGroup serviceRowGroup, ServiceWrapper service) {
                            addServiceDialogFragment(service, serviceRowGroup);
                        }
                    });

                    TextView groupNameTextView = createGroupNameTextView(getActivity(), type);
                    serviceGroupCanvasLL.addView(groupNameTextView);
                    serviceGroupCanvasLL.addView(curGroup);
                }
            } catch (Exception e) {
                Timber.e(Log.getStackTraceString(e));
            }

            curServiceMode = editServiceMode;
        }
    }

    public void updateExtraVaccinesView() {
        if (!getExtraVaccines().isEmpty() && getActivity() != null) {
            extraVaccinesLayout.setVisibility(View.VISIBLE);
            extraVaccinesLayout.removeAllViews();
            extraVaccinesLayout.addView(getSectionTitle(R.color.black, R.string.extra_vaccines));
            for (Map.Entry<String, String> vaccine : extraVaccines) {
                RelativeLayout immunizationRow = (RelativeLayout) getActivity().getLayoutInflater().inflate(R.layout.view_immunization_row_card, null);
                TextView label = immunizationRow.findViewById(R.id.name_tv);
                label.setMaxWidth(dpToPx(getActivity(), 240f));
                label.setText(vaccine.getKey());

                Button statusButton = immunizationRow.findViewById(R.id.status_iv);
                statusButton.setBackgroundResource(org.smartregister.immunization.R.drawable.vaccine_card_background_green);
                statusButton.setVisibility(View.VISIBLE);

                TextView dateTextView = immunizationRow.findViewById(R.id.status_text_tv);
                dateTextView.setText(convertDateFormat(vaccine.getValue(), true));

                extraVaccinesLayout.addView(immunizationRow);
            }

        }
    }

    private CustomFontTextView getSectionTitle(int textColorResource, int textResource) {
        CustomFontTextView title = new CustomFontTextView(getActivity());
        title.setAllCaps(true);
        title.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
        title.setTextColor(getResources().getColor(textColorResource));
        title.setText(getString(textResource));
        return title;
    }

    private void addServiceDialogFragment(ServiceWrapper serviceWrapper, ServiceRowGroup serviceRowGroup) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragmentByTag = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (fragmentByTag != null) {
            transaction.remove(fragmentByTag);
        }
        transaction.addToBackStack(null);

        String dobString = Utils.getValue(detailsMap, Constants.KEY.DOB, false);
        DateTime dateTime = Utils.dobStringToDateTime(dobString);
        if (dateTime == null) {
            dateTime = DateTime.now();
        }

        List<ServiceRecord> serviceRecordList =
                ImmunizationLibrary.getInstance().recurringServiceRecordRepository().findByEntityId(childDetails.entityId());
        if (serviceRecordList == null) {
            serviceRecordList = new ArrayList<>();
        }

        ServiceEditDialogFragment serviceEditDialogFragment =
                ServiceEditDialogFragment.newInstance(dateTime, serviceRecordList, serviceWrapper, serviceRowGroup, true);
        serviceEditDialogFragment.show(transaction, DIALOG_TAG);
    }

    public void showGrowthMonitoringDialog(long growthRecordPosition) {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            fragmentTransaction.remove(prev);
        }
        fragmentTransaction.addToBackStack(null);

        String childName = constructChildName();
        String gender = Utils.getValue(detailsMap, Constants.KEY.GENDER, true);
        String motherFirstName = Utils.getValue(detailsMap, Constants.KEY.MOTHER_FIRST_NAME, true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = "B/o " + motherFirstName.trim();
        }
        String openSrpId = Utils.getValue(detailsMap, Constants.KEY.ZEIR_ID, false);
        String duration = "";
        String dobString = Utils.getValue(detailsMap, Constants.KEY.DOB, false);
        DateTime dateTime = Utils.dobStringToDateTime(dobString);

        Date dob = null;
        if (dateTime != null) {
            duration = DateUtil.getDuration(dateTime);
            dob = dateTime.toDate();
        }

        if (dob == null) {
            dob = Calendar.getInstance().getTime();
        }

        Photo photo = getProfilePhotoByClient();

        WeightWrapper weightWrapper = getWeightWrapper(growthRecordPosition, childName, gender, openSrpId, duration, photo);

        HeightWrapper heightWrapper = null;

        if (GrowthMonitoringLibrary.getInstance().getAppProperties().hasProperty(AppProperties.KEY.MONITOR_GROWTH) && GrowthMonitoringLibrary.getInstance().getAppProperties().getPropertyBoolean(AppProperties.KEY.MONITOR_GROWTH)) {
            heightWrapper = getHeightWrapper(growthRecordPosition, childName, gender, openSrpId, duration, photo);
        }

        EditGrowthDialogFragment editWeightDialogFragment = EditGrowthDialogFragment.newInstance(dob, weightWrapper, heightWrapper);
        editWeightDialogFragment.show(fragmentTransaction, DIALOG_TAG);

    }

    @NotNull
    private WeightWrapper getWeightWrapper(long weightPosition, String childName, String gender, String openSrpId,
                                           String duration, Photo photo) {
        WeightWrapper weightWrapper = new WeightWrapper();
        weightWrapper.setId(childDetails.entityId());

        List<Weight> weightList =
                GrowthMonitoringLibrary.getInstance().weightRepository().findByEntityId(childDetails.entityId());

        Weight weight = getWeight(weightList, weightPosition);

        if (!weightList.isEmpty()) {
            weightWrapper.setWeight(weight.getKg());
            weightWrapper.setUpdatedWeightDate(new DateTime(weight.getDate()), false);
            weightWrapper.setDbKey(weight.getId());
        }

        weightWrapper.setGender(gender);
        weightWrapper.setPatientName(childName);
        weightWrapper.setPatientNumber(openSrpId);
        weightWrapper.setPatientAge(duration);
        weightWrapper.setPhoto(photo);
        weightWrapper.setPmtctStatus(Utils.getValue(childDetails.getColumnmaps(), BaseChildDetailTabbedActivity.PMTCT_STATUS_LOWER_CASE, false));
        return weightWrapper;
    }

    private Weight getWeight(List<Weight> weights, long weightPosition) {
        Weight displayWeight = new Weight();
        for (Weight weight : weights) {
            if (Utils.isSameDay(weightPosition, weight.getDate().getTime(), null)) {
                displayWeight = weight;
            }
        }

        return displayWeight;
    }

    @NotNull
    private HeightWrapper getHeightWrapper(long heightPosition, String childName, String gender, String openSrpId,
                                           String duration, Photo photo) {
        HeightWrapper heightWrapper = new HeightWrapper();
        heightWrapper.setId(childDetails.entityId());

        List<Height> heightList =
                GrowthMonitoringLibrary.getInstance().heightRepository().findByEntityId(childDetails.entityId());
        Height height = getHeight(heightList, heightPosition);

        if (!heightList.isEmpty()) {
            heightWrapper.setHeight(height.getCm());
            heightWrapper.setUpdatedHeightDate(new DateTime(height.getDate()), false);
            heightWrapper.setDbKey(height.getId());
        }

        heightWrapper.setGender(gender);
        heightWrapper.setPatientName(childName);
        heightWrapper.setPatientNumber(openSrpId);
        heightWrapper.setPatientAge(duration);
        heightWrapper.setPhoto(photo);
        heightWrapper.setPmtctStatus(
                Utils.getValue(childDetails.getColumnmaps(), BaseChildDetailTabbedActivity.PMTCT_STATUS_LOWER_CASE, false));
        return heightWrapper;
    }

    private Height getHeight(List<Height> heights, long heightPosition) {
        Height displayHeight = new Height();
        for (Height height : heights) {
            if (Utils.isSameDay(heightPosition, height.getDate().getTime(), null)) {
                displayHeight = height;
            }
        }

        return displayHeight;
    }

    protected Photo getProfilePhotoByClient() {
        return ImageUtils.profilePhotoByClient(childDetails);
    }

    private String constructChildName() {
        String firstName = Utils.getValue(detailsMap, Constants.KEY.FIRST_NAME, true);
        String lastName = Utils.getValue(detailsMap, Constants.KEY.LAST_NAME, true);
        return Utils.getName(firstName, lastName).trim();
    }

    /**
     * Do not show the recurring services
     */
    public void hideOrShowRecurringServices() {
        if (this.showRecurringServices) {
            serviceGroupCanvasLL.setVisibility(View.VISIBLE);
        } else {
            serviceGroupCanvasLL.setVisibility(View.GONE);
        }
    }

    public void setExtraVaccines(List<Map.Entry<String, String>> extraVaccines) {
        this.extraVaccines = extraVaccines;
    }

    public List<Map.Entry<String, String>> getExtraVaccines() {
        return extraVaccines;
    }
}
