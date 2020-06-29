package org.smartregister.child.provider;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.smartregister.AllConstants;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.domain.RegisterActionParams;
import org.smartregister.child.domain.RepositoryHolder;
import org.smartregister.child.task.GrowthMonitoringAsyncTask;
import org.smartregister.child.task.VaccinationAsyncTask;
import org.smartregister.child.util.Constants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.RecyclerViewProvider;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.ImageUtils;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.service.AlertService;
import org.smartregister.util.DateUtil;
import org.smartregister.util.OpenSRPImageLoader;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.contract.SmartRegisterClients;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;
import org.smartregister.view.viewholder.OnClickFormLauncher;

import java.text.MessageFormat;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 28/02/2019.
 */
public class ChildRegisterProvider implements RecyclerViewProvider<ChildRegisterProvider.RegisterViewHolder> {

    private final LayoutInflater inflater;
    private Set<org.smartregister.configurableviews.model.View> visibleColumns;

    private View.OnClickListener onClickListener;
    private View.OnClickListener paginationClickListener;

    private Context context;
    private CommonRepository commonRepository;
    private WeightRepository weightRepository;
    private HeightRepository heightRepository;
    private VaccineRepository vaccineRepository;
    private AlertService alertService;
    protected boolean isOutOfCatchment = false;

    public ChildRegisterProvider(Context context, RepositoryHolder repositoryHolder, Set visibleColumns,
                                 View.OnClickListener onClickListener, View.OnClickListener paginationClickListener,
                                 AlertService alertService) {

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.visibleColumns = visibleColumns;

        this.onClickListener = onClickListener;
        this.paginationClickListener = paginationClickListener;

        this.context = context;
        this.commonRepository = repositoryHolder.getCommonRepository();
        this.weightRepository = repositoryHolder.getWeightRepository();
        this.heightRepository = repositoryHolder.getHeightRepository();
        this.vaccineRepository = repositoryHolder.getVaccineRepository();
        this.alertService = alertService;
    }

    public static void fillValue(TextView v, String value) {
        if (v != null) v.setText(value);

    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;
        if (visibleColumns.isEmpty()) {
            populatePatientColumn(pc, client, viewHolder);
        }
    }

    @Override
    public void getFooterView(RecyclerView.ViewHolder viewHolder, int currentPageCount, int totalPageCount, boolean hasNext,
                              boolean hasPrevious) {
        FooterViewHolder footerViewHolder = (FooterViewHolder) viewHolder;
        footerViewHolder.pageInfoView.setText(MessageFormat.format(context.getString(R.string.str_page_info), currentPageCount, totalPageCount));

        footerViewHolder.nextPageView.setVisibility(hasNext ? View.VISIBLE : View.INVISIBLE);
        footerViewHolder.previousPageView.setVisibility(hasPrevious ? View.VISIBLE : View.INVISIBLE);

        footerViewHolder.nextPageView.setOnClickListener(paginationClickListener);
        footerViewHolder.previousPageView.setOnClickListener(paginationClickListener);
    }

    @Override
    public SmartRegisterClients updateClients(FilterOption villageFilter, ServiceModeOption serviceModeOption,

                                              FilterOption searchFilter, SortOption sortOption) {
        return null;
    }

    @Override
    public void onServiceModeSelected(ServiceModeOption serviceModeOption) {//Implement Abstract Method
    }

    @Override
    public OnClickFormLauncher newFormLauncher(String formName, String entityId, String metaData) {
        return null;
    }

    @Override
    public LayoutInflater inflater() {
        return inflater;
    }

    @Override
    public RegisterViewHolder createViewHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.child_register_list_row, parent, false);

        if (ChildLibrary.getInstance().getProperties().hasProperty(Constants.PROPERTY.HOME_RECORD_WEIGHT_ENABLED)) {

            view.findViewById(R.id.record_weight_wrapper).setVisibility(ChildLibrary.getInstance().getProperties()
                    .getPropertyBoolean(Constants.PROPERTY.HOME_RECORD_WEIGHT_ENABLED) ? View.VISIBLE : View.GONE);
        }

        view.findViewById(R.id.child_next_appointment_wrapper).setVisibility(ChildLibrary.getInstance().getProperties()
                .getPropertyBoolean(Constants.PROPERTY.HOME_NEXT_VISIT_DATE_ENABLED) ? View.VISIBLE : View.GONE);

        return new RegisterViewHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder createFooterHolder(ViewGroup parent) {
        View view = inflater.inflate(R.layout.smart_register_pagination, parent, false);
        return new FooterViewHolder(view);
    }

    @Override
    public boolean isFooterViewHolder(RecyclerView.ViewHolder viewHolder) {
        return viewHolder instanceof FooterViewHolder;
    }

    private void populatePatientColumn(CommonPersonObjectClient pc, SmartRegisterClient client,
                                       final RegisterViewHolder viewHolder) {

        String firstName = Utils.getValue(pc.getColumnmaps(), Constants.KEY.FIRST_NAME, true);
        String lastName = Utils.getValue(pc.getColumnmaps(), Constants.KEY.LAST_NAME, true);
        String childName = Utils.getName(firstName, lastName);


        fillValue(viewHolder.childOpensrpID, Utils.getValue(pc.getColumnmaps(), Constants.KEY.ZEIR_ID, false));

        String motherFirstName = Utils.getValue(pc.getColumnmaps(), Constants.KEY.MOTHER_FIRST_NAME, true);
        if (StringUtils.isBlank(childName) && StringUtils.isNotBlank(motherFirstName)) {
            childName = String.format(context.getString(R.string.child_name), motherFirstName.trim());
        }

        fillValue(viewHolder.patientName, WordUtils.capitalize(childName));

        String motherName = Utils.getValue(pc.getColumnmaps(), Constants.KEY.MOTHER_FIRST_NAME, true) + " " +
                Utils.getValue(pc, Constants.KEY.MOTHER_LAST_NAME, true);
        if (StringUtils.isNotBlank(motherName)) {
            motherName = String.format(context.getString(R.string.mother_name), motherName.trim());
        }

        fillValue(viewHolder.childMotherName, motherName);


        String durationString = "";
        String dobString = Utils.getValue(pc.getColumnmaps(), Constants.KEY.DOB, false);
        DateTime birthDateTime = Utils.dobStringToDateTime(dobString);
        if (birthDateTime != null) {
            try {
                String duration = DateUtil.getDuration(birthDateTime);
                if (duration != null) {
                    durationString = duration;
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        fillValue(viewHolder.childAge, durationString);

        fillValue(viewHolder.childCardNumnber, Utils.getValue(pc.getColumnmaps(), Constants.KEY.EPI_CARD_NUMBER, false));


        String gender = Utils.getValue(pc.getColumnmaps(), AllConstants.ChildRegistrationFields.GENDER, true);

        final ImageView profilePic = viewHolder.imageView.findViewById(R.id.child_profilepic);
        renderProfileImage(pc.entityId(), gender, profilePic);

        attachGoToImmunizationPage(viewHolder.childProfileInfoLayout, client);


        View recordGrowthMonitoring = viewHolder.recordGrowth;
        recordGrowthMonitoring.setBackground(context.getResources().getDrawable(R.drawable.record_growth_bg));
        recordGrowthMonitoring.setTag(client);
        recordGrowthMonitoring.setOnClickListener(onClickListener);
        recordGrowthMonitoring.setVisibility(View.INVISIBLE);
        recordGrowthMonitoring.setTag(R.id.record_action, Constants.RECORD_ACTION.GROWTH);

        View recordVaccination = viewHolder.recordVaccination;
        recordVaccination.setTag(client);
        recordVaccination.setOnClickListener(onClickListener);
        recordVaccination.setVisibility(View.INVISIBLE);
        recordVaccination.setTag(R.id.record_action, Constants.RECORD_ACTION.VACCINATION);


        String lostToFollowUp = Utils.getValue(pc.getColumnmaps(), Constants.KEY.LOST_TO_FOLLOW_UP, false);
        String inactive = Utils.getValue(pc.getColumnmaps(), Constants.CHILD_STATUS.INACTIVE, false);

        if (show()) {
            try {

                RegisterActionParams params = new RegisterActionParams();
                params.setConvertView(viewHolder.registerColumns);
                params.setEntityId(pc.entityId());
                params.setLostToFollowUp(lostToFollowUp);
                params.setDobString(dobString);
                params.setInactive(inactive);
                params.setSmartRegisterClient(client);
                params.setUpdateOutOfCatchment(isOutOfCatchment);
                params.setOnClickListener(onClickListener);
                params.setProfileInfoView(viewHolder.childProfileInfoLayout);

                initiateViewUpdateTasks(params);

            } catch (Exception e) {
                Timber.e(e);
            }
        }

    }

    @VisibleForTesting
    protected void initiateViewUpdateTasks(@NonNull RegisterActionParams params) {
        Utils.startAsyncTask(new GrowthMonitoringAsyncTask(params, commonRepository, weightRepository, heightRepository, context), null);
        Utils.startAsyncTask(new VaccinationAsyncTask(params, commonRepository, vaccineRepository, alertService, context), null);
    }

    private void renderProfileImage(String entityId, String gender, ImageView profilePic) {
        int defaultImageResId = ImageUtils.profileImageResourceByGender(gender);
        profilePic.setImageResource(defaultImageResId);

        if (entityId != null && show()) { //image already in local storage most likely ):
            //set profile image by passing the client id.If the image doesn't exist in the image repository then download
            // and save locally
            profilePic.setTag(R.id.entity_id, entityId);
            updateImageViewWithPicture(entityId, profilePic);
        }

    }

    @VisibleForTesting
    protected void updateImageViewWithPicture(String entityId, ImageView profilePic) {
        DrishtiApplication.getCachedImageLoaderInstance()
                .getImageByClientId(entityId, OpenSRPImageLoader.getStaticImageListener(profilePic, 0, 0));
    }

    private void attachGoToImmunizationPage(View view, SmartRegisterClient client) {
        view.setOnClickListener(onClickListener);
        view.setTag(client);
        view.setTag(R.id.record_action, Constants.RECORD_ACTION.NONE);
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private boolean show() {

        return !ChildRegisterProvider.class.equals(this.getClass()) ||
                !ChildLibrary.getInstance().context().allSharedPreferences().fetchIsSyncInitial() ||
                !SyncStatusBroadcastReceiver.getInstance().isSyncing();

    }

    public static class RegisterViewHolder extends RecyclerView.ViewHolder {
        private TextView patientName;
        private TextView childOpensrpID;
        private TextView childMotherName;
        private TextView childAge;
        private TextView childCardNumnber;
        private View childProfileInfoLayout;
        private ImageView imageView;
        private View recordGrowth;
        private View recordVaccination;
        private View registerColumns;

        RegisterViewHolder(View itemView) {
            super(itemView);
            patientName = itemView.findViewById(R.id.child_name);
            childOpensrpID = itemView.findViewById(R.id.child_zeir_id);
            childMotherName = itemView.findViewById(R.id.child_mothername);
            childAge = itemView.findViewById(R.id.child_age);
            childCardNumnber = itemView.findViewById(R.id.child_card_number);
            imageView = itemView.findViewById(R.id.child_profilepic);
            childProfileInfoLayout = itemView.findViewById(R.id.child_profile_info_layout);
            recordGrowth = itemView.findViewById(R.id.record_growth);
            recordVaccination = itemView.findViewById(R.id.record_vaccination);
            registerColumns = itemView.findViewById(R.id.register_columns);

        }
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {
        private TextView pageInfoView;
        private Button nextPageView;
        private Button previousPageView;

        FooterViewHolder(View view) {
            super(view);
            nextPageView = view.findViewById(R.id.btn_next_page);
            previousPageView = view.findViewById(R.id.btn_previous_page);
            pageInfoView = view.findViewById(R.id.txt_page_info);
        }
    }

}
