package org.smartregister.child.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.api.constants.Gender;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.adapter.ViewPagerAdapter;
import org.smartregister.child.contract.IChildDetails;
import org.smartregister.child.dao.ChildDao;
import org.smartregister.child.enums.Status;
import org.smartregister.child.fragment.BaseChildRegistrationDataFragment;
import org.smartregister.child.fragment.ChildUnderFiveFragment;
import org.smartregister.child.listener.OnSaveDynamicVaccinesListener;
import org.smartregister.child.listener.StatusChangeListener;
import org.smartregister.child.task.LaunchAdverseEventFormTask;
import org.smartregister.child.task.LoadAsyncTask;
import org.smartregister.child.task.SaveAdverseEventTask;
import org.smartregister.child.task.SaveDynamicVaccinesTask;
import org.smartregister.child.task.SaveRegistrationDetailsTask;
import org.smartregister.child.task.SaveServiceTask;
import org.smartregister.child.task.SaveVaccinesTask;
import org.smartregister.child.task.UndoServiceTask;
import org.smartregister.child.task.UpdateOfflineAlertsTask;
import org.smartregister.child.toolbar.ChildDetailsToolbar;
import org.smartregister.child.util.ChildAppProperties;
import org.smartregister.child.util.ChildDbUtils;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Alert;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.growthmonitoring.domain.Height;
import org.smartregister.growthmonitoring.domain.HeightWrapper;
import org.smartregister.growthmonitoring.domain.Weight;
import org.smartregister.growthmonitoring.domain.WeightWrapper;
import org.smartregister.growthmonitoring.listener.GrowthMonitoringActionListener;
import org.smartregister.growthmonitoring.repository.HeightRepository;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.util.WeightUtils;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.db.VaccineRepo;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.immunization.listener.ServiceActionListener;
import org.smartregister.immunization.listener.VaccinationActionListener;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.util.ImageUtils;
import org.smartregister.immunization.util.RecurringServiceUtils;
import org.smartregister.immunization.util.VaccinateActionUtils;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.immunization.view.ImmunizationRowGroup;
import org.smartregister.immunization.view.ServiceRowGroup;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.DateUtil;
import org.smartregister.util.FormUtils;
import org.smartregister.util.OpenSRPImageLoader;
import org.smartregister.util.PermissionUtils;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static org.smartregister.util.Utils.getValue;

/**
 * Created by raihan on 1/03/2017.
 */

public abstract class BaseChildDetailTabbedActivity extends BaseChildActivity
        implements IChildDetails, VaccinationActionListener, GrowthMonitoringActionListener, StatusChangeListener, ServiceActionListener, OnSaveDynamicVaccinesListener {

    public static final String DIALOG_TAG = "ChildDetailActivity_DIALOG_TAG";
    public static final String PMTCT_STATUS_LOWER_CASE = "pmtct_status";
    protected static final int REQUEST_CODE_GET_JSON = 3432;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String TAG = BaseChildDetailTabbedActivity.class.getCanonicalName();
    protected static Menu overflow;
    private static Gender gender;
    protected ViewPager viewPager;
    protected TextView saveButton;
    protected Map<String, String> detailsMap;
    private ChildDetailsToolbar childDetailsToolbar;
    private TabLayout tabLayout;
    private BaseChildRegistrationDataFragment childDataFragment;
    private ChildUnderFiveFragment childUnderFiveFragment;
    private File currentFile;
    private String locationId = "";
    private ImageView profileImageIV;
    private boolean monitorGrowth = false;
    private List<VaccineWrapper> editImmunizationCacheMap = new ArrayList<>();
    private List<ServiceHolder> editServicesList = new ArrayList<>();
    private List<ServiceHolder> removeServicesList = new ArrayList<>();
    private List<Long> dbKeysForDelete = new ArrayList<>();
    private VaccineRepository vaccineRepository;
    private List<Map.Entry<String, String>> extraChildVaccines;

    public static void updateOptionsMenu(@NonNull List<Vaccine> vaccineList, @NonNull List<ServiceRecord> serviceRecordList,
                                         @NonNull List<Weight> weightList, @Nullable List<Alert> alertList) {
        boolean showVaccineList = false;
        for (int i = 0; i < vaccineList.size(); i++) {
            Vaccine vaccine = vaccineList.get(i);
            boolean check = VaccinateActionUtils.lessThanThreeMonths(vaccine);
            if (check) {
                showVaccineList = true;
                break;
            }
        }

        boolean showServiceList = false;
        for (ServiceRecord serviceRecord : serviceRecordList) {
            boolean check = VaccinateActionUtils.lessThanThreeMonths(serviceRecord);
            if (check) {
                showServiceList = true;
                break;

            }
        }

        boolean showWeightEdit = false;

        if (weightList.size() > 1) {//Dissallow editing when only birth weight exists

            for (int i = 0; i < weightList.size(); i++) {
                Weight weight = weightList.get(i);
                showWeightEdit = WeightUtils.lessThanThreeMonths(weight);
                if (showWeightEdit) {
                    break;
                }
            }
        }

        hideDisplayImmunizationMenu(showVaccineList, showServiceList, showWeightEdit);
    }

    private static void hideDisplayImmunizationMenu(boolean showVaccineList, boolean showServiceList, boolean showWeightEdit) {
        overflow.findItem(R.id.immunization_data).setEnabled(showVaccineList);
        overflow.findItem(R.id.recurring_services_data).setEnabled(showServiceList);
        overflow.findItem(R.id.weight_data).setEnabled(showWeightEdit);
    }

    public static Menu getOverflow() {
        return overflow;
    }

    public static void setOverflow(Menu overflow) {
        BaseChildDetailTabbedActivity.overflow = overflow;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        monitorGrowth = GrowthMonitoringLibrary.getInstance().getAppProperties().hasProperty(org.smartregister.growthmonitoring.util.AppProperties.KEY.MONITOR_GROWTH) && GrowthMonitoringLibrary.getInstance().getAppProperties().getPropertyBoolean(org.smartregister.growthmonitoring.util.AppProperties.KEY.MONITOR_GROWTH);
        super.onCreate(savedInstanceState);

        initLoadChildDetails();

        setContentView(getContentView());

        setExtraChildVaccines(ChildDao.getChildExtraVaccines(childDetails.entityId()));

        childDataFragment = getChildRegistrationDataFragment();
        childDataFragment.setArguments(this.getIntent().getExtras());

        childUnderFiveFragment = new ChildUnderFiveFragment();
        childUnderFiveFragment.setArguments(this.getIntent().getExtras());
        childUnderFiveFragment.showRecurringServices(true);
        childUnderFiveFragment.setExtraVaccines(getExtraChildVaccines());

        childDetailsToolbar = findViewById(R.id.child_detail_toolbar);

        saveButton = childDetailsToolbar.findViewById(R.id.save);
        saveButton.setVisibility(View.INVISIBLE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetOptionsMenu();
                processEditedServices();
            }
        });

        childDetailsToolbar.showOverflowMenu();

        setSupportActionBar(childDetailsToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tabLayout = findViewById(R.id.tabs);

        viewPager = findViewById(R.id.viewpager);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //Todo
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0 && saveButton.getVisibility() == View.VISIBLE) {
                    resetOptionsMenu();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //Overriden
            }
        });
        setupViewPager(viewPager);

        childDetailsToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        setActivityTitle();
        tabLayout.setupWithViewPager(viewPager);
        setupViews();
        vaccineRepository = ImmunizationLibrary.getInstance().vaccineRepository();
    }

    @Nullable
    private Bundle initLoadChildDetails() {
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            locationId = extras.getString(Constants.INTENT_KEY.LOCATION_ID);
            String caseId = extras.getString(Constants.INTENT_KEY.BASE_ENTITY_ID);
            childDetails = ChildDbUtils.fetchCommonPersonObjectClientByBaseEntityId(caseId);

            Utils.putAll(childDetails.getColumnmaps(), ChildDbUtils.fetchChildFirstGrowthAndMonitoring(caseId));
            detailsMap = childDetails.getColumnmaps();
        }
        return extras;
    }

    protected abstract BaseChildRegistrationDataFragment getChildRegistrationDataFragment();

    private void resetOptionsMenu() {
        childDetailsToolbar.showOverflowMenu();
        invalidateOptionsMenu();

        saveButton.setVisibility(View.INVISIBLE);
    }

    protected void processEditedServices() {

        //Services
        for (ServiceHolder serviceHolder : editServicesList) {
            saveService(serviceHolder.wrapper, serviceHolder.view);
        }
        editServicesList.clear();

        //Remove service
        for (ServiceHolder serviceHolder : removeServicesList) {
            undoService(serviceHolder.wrapper, serviceHolder.view);
        }
        removeServicesList.clear();

        //Vaccinations
        for (VaccineWrapper vaccineWrapper : editImmunizationCacheMap) {
            saveVaccine(vaccineWrapper);
        }

        for (int i = 0; i < dbKeysForDelete.size(); i++) {
            vaccineRepository.deleteVaccine(dbKeysForDelete.get(i));
        }

        //clean up
        editImmunizationCacheMap.clear();
        dbKeysForDelete.clear();
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(childDataFragment, getString(R.string.registration_data));
        adapter.addFragment(childUnderFiveFragment, getString(R.string.under_five_history));
        viewPager.setAdapter(adapter);
    }

    protected void setActivityTitle() {
        ((TextView) childDetailsToolbar.findViewById(R.id.title)).setText(getActivityTitle());
    }

    public void setupViews() {
        profileImageIV = findViewById(R.id.profile_image_iv);
        if (!ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_IMAGES_ENABLED)) {
            profileImageIV.setOnClickListener(null);
            findViewById(R.id.profile_image_edit_icon).setVisibility(View.GONE);
        } else {
            findViewById(R.id.profile_image_edit_icon).setVisibility(View.VISIBLE);
            profileImageIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PermissionUtils.isPermissionGranted(BaseChildDetailTabbedActivity.this,
                            new String[]{Manifest.permission.CAMERA}, PermissionUtils.CAMERA_PERMISSION_REQUEST_CODE)) {
                        dispatchTakePictureIntent();
                    }
                }
            });
        }

        DrawerLayout mDrawerLayout = findViewById(getDrawerLayoutId());
        if (mDrawerLayout != null &&
                (ChildLibrary.getInstance().getProperties().hasProperty(ChildAppProperties.KEY.DETAILS_SIDE_NAVIGATION_ENABLED) &&
                        !ChildLibrary.getInstance().getProperties()
                                .getPropertyBoolean(ChildAppProperties.KEY.DETAILS_SIDE_NAVIGATION_ENABLED))) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        renderProfileWidget(detailsMap);
        updateGenderViews();
    }

    private void dispatchTakePictureIntent() {
        if (PermissionUtils.isPermissionGranted(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionUtils.CAMERA_PERMISSION_REQUEST_CODE)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Timber.e(ex, "BaseChildDetailTabbedActivity --> dispatchTakePictureIntent");
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {

                    //We need this for backward compatibility
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                        StrictMode.setVmPolicy(builder.build());
                    }

                    currentFile = photoFile;
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        }
    }

    public void renderProfileWidget(Map<String, String> childDetails) {
        TextView profilename = findViewById(R.id.name);
        TextView profileOpenSrpId = findViewById(R.id.idforclient);
        TextView profileage = findViewById(R.id.ageforclient);
        String name = "";
        String childId = "";
        String dobString;
        String formattedAge = "";
        if (isDataOk()) {
            name = getValue(childDetails, Constants.KEY.FIRST_NAME, true) + " " +
                    getValue(childDetails, Constants.KEY.LAST_NAME, true);
            childId = getValue(childDetails, Constants.KEY.ZEIR_ID, false);
            if (StringUtils.isNotBlank(childId)) {
                childId = childId.replace("-", "");
            }
            dobString = getValue(childDetails, Constants.KEY.DOB, false);
            Date dob = Utils.dobStringToDate(dobString);
            if (dob != null) {
                long timeDiff = Calendar.getInstance().getTimeInMillis() - dob.getTime();

                if (timeDiff >= 0) {
                    formattedAge = DateUtil.getDuration(timeDiff);
                }
            }
        }

        profileage.setText(" " + formattedAge);
        profileOpenSrpId.setText(" " + childId);
        profilename.setText(name);

        Gender gender = isDataOk() ? Utils.getGenderEnum(childDetails) : Gender.UNKNOWN;

        updateProfilePicture(gender);
    }

    private void updateGenderViews() {

        Gender gender = isDataOk() ? Utils.getGenderEnum(childDetails.getColumnmaps()) : Gender.UNKNOWN;

        int[] colors = updateGenderViews(gender);
        int normalShade = colors[1];
        ColorDrawable colorDrawable = new ColorDrawable(getResources().getColor(normalShade));
        childDetailsToolbar.setBackground(colorDrawable);
        findViewById(R.id.advanced_data_capture_strategy_wrapper).setBackground(colorDrawable);
        tabLayout.setTabTextColors(getResources().getColor(R.color.dark_grey), getResources().getColor(normalShade));
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(normalShade));
        try {
            Field field = TabLayout.class.getDeclaredField("mTabStrip");
            field.setAccessible(true);
            Object object = field.get(tabLayout);
            Class<?> c = Class.forName("com.google.android.material.snackbar.widget.TabLayout$SlidingTabStrip");
            Method method = c.getDeclaredMethod("setSelectedIndicatorColor", int.class);
            method.setAccessible(true);
            method.invoke(object, getResources().getColor(normalShade)); //now its ok
        } catch (Exception e) {
            try {
                Timber.i(e, "BaseChildDetailTabbedActivity --> No field mTabStrip in class Landroid/support/design/widget/TabLayout");
            } catch (Exception ex) {

            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (isStoragePermissionGranted()) {
            return File.createTempFile(imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */);
        }

        return null;
    }

    private void updateProfilePicture(Gender gender) {
        BaseChildDetailTabbedActivity.gender = gender;
        if (isDataOk() && childDetails.entityId() != null) { //image already in local storage most likely ):
            //set profile image by passing the client id.If the image doesn't exist in the image repository then download and save locally
            profileImageIV.setTag(org.smartregister.R.id.entity_id, childDetails.entityId());
            DrishtiApplication.getCachedImageLoaderInstance().getImageByClientId(childDetails.entityId(), OpenSRPImageLoader.getStaticImageListener(profileImageIV, ImageUtils.profileImageResourceByGender(gender), ImageUtils.profileImageResourceByGender(gender)));

        }

    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                Timber.tag(TAG).v("Permission is granted");
                return true;
            } else {

                Timber.tag(TAG).v("Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Timber.tag(TAG).v("Permission is granted");
            return true;
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.child_detail_activity_simple_tabs;
    }

    @Override
    protected int getToolbarId() {
        return R.id.child_detail_toolbar;
    }

    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_child_detail_settings, menu);
        overflow = menu;

        //Defaults
        overflow.findItem(R.id.immunization_data).setEnabled(false);
        overflow.findItem(R.id.recurring_services_data).setEnabled(false);
        overflow.findItem(R.id.weight_data).setEnabled(false);

        if (ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_NFC_CARD_ENABLED)) {
            overflow.findItem(R.id.write_to_card).setVisible(true);
            overflow.findItem(R.id.register_card).setVisible(true);
        }

        Utils.startAsyncTask(new LoadAsyncTask(detailsMap, childDetails, this, childDataFragment, childUnderFiveFragment, overflow), null);//Loading data here because we affect state of the menu item

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method launches dynamic vaccines form. The form has a list of vaccines selected from the multiselect
     * widget plus the date the vaccine(s) were administered.
     *
     * @param dynamicVaccinesForm  name of the form
     * @param multiSelectFieldName field containing list of extra vaccines
     */
    protected void launchDynamicVaccinesForm(String dynamicVaccinesForm, String multiSelectFieldName) {

        try {
            JSONObject jsonObject = FormUtils.getInstance(getContext()).getFormJson(dynamicVaccinesForm);
            jsonObject.put(Constants.KEY.ENTITY_ID, childDetails.getCaseId());
            if (jsonObject.has(ChildJsonFormUtils.METADATA)) {
                jsonObject.getJSONObject(ChildJsonFormUtils.METADATA).put(ChildJsonFormUtils.ENCOUNTER_LOCATION, locationId);
            }
            jsonObject.put(Constants.KEY.DYNAMIC_FIELD, multiSelectFieldName);
            jsonObject.put(Constants.KEY.ENTITY_ID, childDetails.entityId());
            startFormActivity(jsonObject.toString());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void startJsonForm(String formName, String entityId) {
        try {
            startJsonForm(formName, entityId, locationId);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AllSharedPreferences allSharedPreferences = getOpenSRPContext().allSharedPreferences();
        if (requestCode == REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(JsonFormConstants.JSON_FORM_KEY.JSON);
                Timber.d(jsonString);

                JSONObject form = new JSONObject(jsonString);
                String encounterType = form.getString(ChildJsonFormUtils.ENCOUNTER_TYPE);
                switch (encounterType) {
                    case Constants.EventType.DEATH:
                        confirmReportDeceased(jsonString);
                        break;
                    case Constants.EventType.BITRH_REGISTRATION:
                    case Constants.EventType.UPDATE_BITRH_REGISTRATION:
                        updateRegistration(jsonString);
                        break;
                    case Constants.EventType.AEFI:
                        Utils.startAsyncTask(new SaveAdverseEventTask(jsonString, locationId, childDetails.entityId(), allSharedPreferences.fetchRegisteredANM(), CoreLibrary.getInstance().context().getEventClientRepository()), null);
                        break;
                    case Constants.EventType.DYNAMIC_VACCINES:
                        Utils.startAsyncTask(new SaveDynamicVaccinesTask(this, jsonString, childDetails.entityId()), null);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                Timber.e(Log.getStackTraceString(e));
            }

        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            String imageLocation = currentFile.getAbsolutePath();

            ChildJsonFormUtils.saveImage(allSharedPreferences.fetchRegisteredANM(), childDetails.entityId(), imageLocation);
            updateProfilePicture(gender);
        }
    }

    protected void updateRegistration(String jsonString) {
        SaveRegistrationDetailsTask saveRegistrationDetailsTask = new SaveRegistrationDetailsTask(this);
        saveRegistrationDetailsTask.setJsonString(jsonString);
        Utils.startAsyncTask(saveRegistrationDetailsTask, null);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void setChildDetails(Map<String, String> detailsMap) {
        this.detailsMap = detailsMap;
        childDetails.setDetails(detailsMap);
        childDetails.setColumnmaps(detailsMap);
    }

    @Override
    protected Class onBackActivity() {
        return Utils.metadata().childImmunizationActivity;
    }

    @Override
    public void onSaveDynamicVaccine() {
        showProgressDialog();
        viewPager.setCurrentItem(1, true);
        setExtraChildVaccines(ChildDao.getChildExtraVaccines(childDetails.entityId()));
        childUnderFiveFragment.setExtraVaccines(getExtraChildVaccines());
        childUnderFiveFragment.updateExtraVaccinesView();
        hideProgressDialog();
    }

    protected void confirmReportDeceased(final String json) {

        final AlertDialog builder = new AlertDialog.Builder(this).setCancelable(false).create();

        LayoutInflater inflater = getLayoutInflater();
        View notificationsLayout = inflater.inflate(R.layout.notification_base, null);
        notificationsLayout.setVisibility(View.VISIBLE);

        ImageView notificationIcon = notificationsLayout.findViewById(R.id.noti_icon);
        notificationIcon.setTag("confirm_deceased_icon");
        notificationIcon.setImageResource(R.drawable.ic_deceased);
        notificationIcon.getLayoutParams().height = 165;

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) notificationIcon.getLayoutParams();
        params.setMargins(55, params.topMargin, params.rightMargin, params.bottomMargin);
        notificationIcon.setLayoutParams(params);

        TextView notificationMessage = notificationsLayout.findViewById(R.id.noti_message);
        notificationMessage.setText(getString(R.string.marked_as_deceased, getChildName()));
        notificationMessage.setTextColor(getResources().getColor(R.color.black));
        notificationMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);

        Button positiveButton = notificationsLayout.findViewById(R.id.noti_positive_button);
        positiveButton.setVisibility(View.VISIBLE);
        positiveButton.setText(getResources().getString(R.string.undo));
        positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
            }
        });

        Button negativeButton = notificationsLayout.findViewById(R.id.noti_negative_button);
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setText(getResources().getString(R.string.confirm_button_label));
        negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReportDeceasedJson(json);
                builder.dismiss();

                navigateToRegisterActivity();
            }
        });

        builder.setView(notificationsLayout);
        builder.show();
    }


    protected String getChildName() {

        return Utils.getName(getName(Constants.KEY.FIRST_NAME), getName(Constants.KEY.LAST_NAME));
    }

    protected String getName(String key) {
        String name = childDetails.getColumnmaps().get(key);

        return StringUtils.isBlank(name) ? "" : name;
    }

    private void saveReportDeceasedJson(String jsonString) {
        ChildJsonFormUtils.saveReportDeceased(this, jsonString, locationId, childDetails.entityId());

    }

    protected abstract void navigateToRegisterActivity();

    protected boolean launchAdverseEventForm() {
        LaunchAdverseEventFormTask task = new LaunchAdverseEventFormTask(this);
        task.execute();
        return true;
    }

    @Override
    public void updateStatus() {
        updateStatus(false);
    }

    public void updateStatus(boolean fromAsyncTask) {
        String status = getHumanFriendlyChildsStatus(detailsMap);
        showChildsStatus(status);

        boolean isChildActive = isActiveStatus(status);
        if (isChildActive) {
            updateOptionsMenu(true, true, true);

            if (!fromAsyncTask) {
                LoadAsyncTask loadAsyncTask = new LoadAsyncTask(detailsMap, childDetails, this, childDataFragment, childUnderFiveFragment, overflow);
                loadAsyncTask.setFromUpdateStatus(true);
                Utils.startAsyncTask(loadAsyncTask, null);
            }
        } else {
            updateOptionsMenu(false, false, false);
            hideDisplayImmunizationMenu(false, false, false);
        }
    }

    @Override
    public void updateStatus(Map<String, String> details) {
        detailsMap = details;
        updateStatus(false);
    }

    private void updateOptionsMenu(boolean canEditRegistrationData, boolean canReportDeceased, boolean canReportAdverseEvent) {
        overflow.findItem(R.id.registration_data).setEnabled(canEditRegistrationData);
        overflow.findItem(R.id.report_deceased).setEnabled(canReportDeceased);
        overflow.findItem(R.id.report_adverse_event).setEnabled(canReportAdverseEvent);
    }

    @Override
    public void updateClientAttribute(String attributeName, Object attributeValue) {
        try {
            ChildDbUtils.updateChildDetailsValue(attributeName, String.valueOf(attributeValue), childDetails.entityId());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.d("Permission callback called-------");

        if (grantResults.length == 0) {
            return;
        }
        if (requestCode == PermissionUtils.CAMERA_PERMISSION_REQUEST_CODE &&
                PermissionUtils.verifyPermissionGranted(permissions, grantResults, Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onVaccinateToday(ArrayList<VaccineWrapper> tags, View view) {
        if (tags != null && !tags.isEmpty()) {
            saveVaccine(tags, view);
            Utils.startAsyncTask(new UpdateOfflineAlertsTask(childDetails), null);
        }
    }

    @Override
    public void onVaccinateEarlier(ArrayList<VaccineWrapper> tags, View view) {
        if (tags != null && !tags.isEmpty()) {
            saveVaccine(tags, view);
            Utils.startAsyncTask(new UpdateOfflineAlertsTask(childDetails), null);
        }
    }

    @Override
    public void onUndoVaccination(VaccineWrapper vaccineWrapper, View view) {
        if (vaccineWrapper != null && vaccineWrapper.getDbKey() != null) {

            dbKeysForDelete.add(vaccineWrapper.getDbKey());

            vaccineWrapper.setUpdatedVaccineDate(null, false);
            vaccineWrapper.setDbKey(null);

            List<Vaccine> vaccineList = vaccineRepository.findByEntityId(childDetails.entityId());

            ArrayList<VaccineWrapper> wrappers = new ArrayList<>();
            wrappers.add(vaccineWrapper);
            updateVaccineGroupViews(view, wrappers, vaccineList, true);

            Utils.startAsyncTask(new UpdateOfflineAlertsTask(childDetails), null);
        }
    }

    private void updateVaccineGroupViews(View view, final ArrayList<VaccineWrapper> wrappers,
                                         final List<Vaccine> vaccineList, final boolean undo) {
        if (!(view instanceof ImmunizationRowGroup)) {
            return;
        }
        final ImmunizationRowGroup vaccineGroup = (ImmunizationRowGroup) view;
        vaccineGroup.setModalOpen(false);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (undo) {
                vaccineGroup.setVaccineList(vaccineList);
                vaccineGroup.updateWrapperStatus(wrappers);
            }
            vaccineGroup.updateViews(wrappers);

        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (undo) {
                        vaccineGroup.setVaccineList(vaccineList);
                        vaccineGroup.updateWrapperStatus(wrappers);
                    }
                    vaccineGroup.updateViews(wrappers);
                }
            });
        }
    }

    private void saveVaccine(List<VaccineWrapper> tags, final View view) {
        if (tags != null && !tags.isEmpty()) {
            if (tags.size() == 1) {
                editImmunizationCacheMap.add(tags.get(0));
                updateVaccineGroupViews(view);
            } else {
                VaccineWrapper[] arrayTags = tags.toArray(new VaccineWrapper[tags.size()]);
                SaveVaccinesTask backgroundTask = new SaveVaccinesTask(this);
                backgroundTask.setView(view);
                backgroundTask.execute(arrayTags);
            }
        }
    }

    public void saveVaccine(VaccineWrapper vaccineWrapper) {
        Vaccine vaccine = new Vaccine();
        if (vaccineWrapper.getDbKey() != null) {
            vaccine = ImmunizationLibrary.getInstance().vaccineRepository().find(vaccineWrapper.getDbKey());
        }
        vaccine.setBaseEntityId(childDetails.entityId());
        vaccine.setName(vaccineWrapper.getName());
        vaccine.setDate(vaccineWrapper.getUpdatedVaccineDate().toDate());
        vaccine.setUpdatedAt(vaccineWrapper.getUpdatedVaccineDate().toDate().getTime());
        vaccine.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
        if (StringUtils.isNotBlank(locationId)) {
            vaccine.setLocationId(locationId);
        }

        String lastChar = vaccine.getName().substring(vaccine.getName().length() - 1);
        if (StringUtils.isNumeric(lastChar)) {
            vaccine.setCalculation(Integer.valueOf(lastChar));
        } else {
            vaccine.setCalculation(-1);
        }
        Utils.addVaccine(ImmunizationLibrary.getInstance().vaccineRepository(), vaccine);
        vaccineWrapper.setDbKey(vaccine.getId());


        if (vaccineWrapper.getName().equalsIgnoreCase(VaccineRepo.Vaccine.bcg2.display())) {
            resetOptionsMenu();
        }
    }

    public void updateVaccineGroupViews(View view) {
        if (!(view instanceof ImmunizationRowGroup)) {
            return;
        }
        final ImmunizationRowGroup vaccineGroup = (ImmunizationRowGroup) view;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            vaccineGroup.updateViews();
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    vaccineGroup.updateViews();
                }
            });
        }
    }

    @Override
    public void onGrowthRecorded(WeightWrapper weightWrapper, HeightWrapper heightWrapper) {
        updateWeightWrapper(weightWrapper);

        if (monitorGrowth && heightWrapper.getHeight() != null) {
            updateHeightWrapper(heightWrapper);
        }

        Utils.startAsyncTask(new LoadAsyncTask(Status.EDIT_GROWTH, detailsMap, childDetails, this, childDataFragment, childUnderFiveFragment, overflow), null);
    }

    private void updateWeightWrapper(WeightWrapper weightWrapper) {
        if (weightWrapper != null && (weightWrapper.getWeight() != null)) {
            WeightRepository weightRepository = GrowthMonitoringLibrary.getInstance().weightRepository();
            Weight weight = new Weight();
            if (weightWrapper.getDbKey() != null) {
                weight = weightRepository.find(weightWrapper.getDbKey());
            }
            weight.setBaseEntityId(childDetails.entityId());
            weight.setKg(weightWrapper.getWeight());
            weight.setDate(weightWrapper.getUpdatedWeightDate().toDate());
            weight.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
            if (StringUtils.isNotBlank(locationId)) {
                weight.setLocationId(locationId);
            }

            Gender gender = isDataOk() ? Utils.getGenderEnum(childDetails.getColumnmaps()) : Gender.UNKNOWN;

            String dobString = getValue(childDetails.getColumnmaps(), Constants.KEY.DOB, false);
            Date dob = Utils.dobStringToDate(dobString);

            if (dob != null && gender != Gender.UNKNOWN) {
                weightRepository.add(dob, gender, weight);
            } else {
                weightRepository.add(weight);
            }

            weightWrapper.setDbKey(weight.getId());
        }
    }

    private void updateHeightWrapper(HeightWrapper heightWrapper) {
        if (heightWrapper != null && (heightWrapper.getHeight() != null)) {
            HeightRepository heightRepository = GrowthMonitoringLibrary.getInstance().heightRepository();
            Height height = new Height();
            if (heightWrapper.getDbKey() != null) {
                height = heightRepository.find(heightWrapper.getDbKey());
            }
            height.setBaseEntityId(childDetails.entityId());
            height.setCm(heightWrapper.getHeight());
            height.setDate(heightWrapper.getUpdatedHeightDate().toDate());
            height.setAnmId(getOpenSRPContext().allSharedPreferences().fetchRegisteredANM());
            if (StringUtils.isNotBlank(locationId)) {
                height.setLocationId(locationId);
            }

            Gender gender = isDataOk() ? Utils.getGenderEnum(childDetails.getColumnmaps()) : Gender.UNKNOWN;

            String dobString = getValue(childDetails.getColumnmaps(), Constants.KEY.DOB, false);
            Date dob = Utils.dobStringToDate(dobString);

            if (dob != null && gender != Gender.UNKNOWN) {
                heightRepository.add(dob, gender, height);
            } else {
                heightRepository.add(height);
            }

            heightWrapper.setDbKey(height.getId());
        }
    }

    protected String getReportDeceasedMetadata() {
        try {
            JSONObject form = new FormUtils(getContext()).getFormJson("report_deceased");
            if (form != null) {
                //inject zeir id into the form
                JSONObject stepOne = form.getJSONObject(ChildJsonFormUtils.STEP1);
                JSONArray jsonArray = stepOne.getJSONArray(ChildJsonFormUtils.FIELDS);

                //Date Birth
                JSONObject dateBirthJSONObject = ChildJsonFormUtils.getFieldJSONObject(jsonArray, Constants.JSON_FORM_KEY.DATE_BIRTH);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(com.vijay.jsonwizard.utils.FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN, Locale.ENGLISH);
                String dobString = getValue(childDetails.getColumnmaps(), Constants.KEY.DOB, true);
                Date dob = Utils.dobStringToDate(dobString);
                if (dob != null) {
                    dateBirthJSONObject.put(ChildJsonFormUtils.VALUE, simpleDateFormat.format(dob));
                }

                //Date Death
                JSONObject dateDeathJSONObject = ChildJsonFormUtils.getFieldJSONObject(jsonArray, Constants.JSON_FORM_KEY.DATE_DEATH);
                dateDeathJSONObject.put(JsonFormConstants.MIN_DATE, simpleDateFormat.format(dob));


            }
            return form == null ? null : form.toString();

        } catch (Exception e) {
            Timber.e(Log.getStackTraceString(e));
        }
        return "";
    }

    public boolean insertVaccinesGivenAsOptions(JSONObject question) throws JSONException {
        JSONObject omrsChoicesTemplate = question.getJSONObject("openmrs_choice_ids");
        JSONObject omrsChoices = new JSONObject();
        JSONArray choices = new JSONArray();
        List<Vaccine> vaccineList =
                ImmunizationLibrary.getInstance().vaccineRepository().findByEntityId(childDetails.entityId());

        boolean ok = false;
        if (vaccineList != null && vaccineList.size() > 0) {
            ok = true;
            for (int i = vaccineList.size() - 1; i >= 0; i--) {
                Vaccine curVaccine = vaccineList.get(i);
                String name = VaccinatorUtils.getTranslatedVaccineName(this,
                        curVaccine.getName()) + " (" + new SimpleDateFormat("dd-MM-yyyy",
                        Locale.ENGLISH).format(curVaccine.getDate()) + ")";
                choices.put(name.toUpperCase(Locale.getDefault()));

                Iterator<String> vaccineGroupNames = omrsChoicesTemplate.keys();
                while (vaccineGroupNames.hasNext()) {
                    String curGroupName = vaccineGroupNames.next();
                    if (curVaccine.getName().toLowerCase().contains(curGroupName.toLowerCase())) {
                        omrsChoices.put(name, omrsChoicesTemplate.getString(curGroupName));
                        break;
                    }
                }
            }
        }

        question.put("values", choices);
        question.put("openmrs_choice_ids", omrsChoices);

        return ok;
    }

    //Recurring Service
    @Override
    public void onGiveToday(ServiceWrapper serviceWrapper, View view) {
        if (serviceWrapper != null) {

            final ServiceRowGroup serviceRowGroup = (ServiceRowGroup) view;
            if (Looper.myLooper() == Looper.getMainLooper()) {

                serviceRowGroup.setServiceRecordList(serviceRowGroup.getServiceRecordList());
                serviceRowGroup.updateWrapperStatus(serviceWrapper);

                ArrayList<ServiceWrapper> arrayList = new ArrayList();
                arrayList.add(serviceWrapper);
                serviceRowGroup.updateViews(arrayList);

            }

            editServicesList.add(new ServiceHolder(serviceWrapper, view));
        }
    }

    @Override
    public void onGiveEarlier(ServiceWrapper serviceWrapper, View view) {
        if (serviceWrapper != null) {

            final ServiceRowGroup serviceRowGroup = (ServiceRowGroup) view;
            if (Looper.myLooper() == Looper.getMainLooper()) {

                serviceRowGroup.setServiceRecordList(serviceRowGroup.getServiceRecordList());
                serviceRowGroup.updateWrapperStatus(serviceWrapper);

                ArrayList<ServiceWrapper> arrayList = new ArrayList();
                arrayList.add(serviceWrapper);
                serviceRowGroup.updateViews(arrayList);

            }

            editServicesList.add(new ServiceHolder(serviceWrapper, view));
        }
    }

    @Override
    public void onUndoService(ServiceWrapper serviceWrapper, View view) {

        if (serviceWrapper != null) {

            //Create delete clone
            ServiceWrapper serviceWrapperClone = new ServiceWrapper();

            serviceWrapperClone.setId(serviceWrapper.getId());
            serviceWrapperClone.setDbKey(serviceWrapper.getDbKey());
            serviceWrapperClone.setServiceType(serviceWrapper.getServiceType());

            final ServiceRowGroup serviceRowGroup = (ServiceRowGroup) view;

            ArrayList<ServiceWrapper> wrappers = new ArrayList<>();
            wrappers.add(serviceWrapper);

            serviceWrapper.setUpdatedVaccineDate(null, false);
            serviceWrapper.setDbKey(null);

            RecurringServiceUtils.updateServiceGroupViews(view, wrappers, serviceRowGroup.getServiceRecordList(), serviceRowGroup.getAlertList(), true);

            removeServicesList.add(new ServiceHolder(serviceWrapperClone, view));
        }

    }

    private void undoService(ServiceWrapper serviceWrapper, View view) {

        Utils.startAsyncTask(new UndoServiceTask(serviceWrapper, view, this, childDetails), null);

    }

    private void saveService(ServiceWrapper serviceWrapper, final View view) {
        if (serviceWrapper == null) {
            return;
        }

        ServiceWrapper[] arrayTags = {serviceWrapper};
        SaveServiceTask backgroundTask = new SaveServiceTask(this, childDetails);

        backgroundTask.setView(view);
        Utils.startAsyncTask(backgroundTask, arrayTags);
    }

    @Override
    public CommonPersonObjectClient getChildDetails() {
        return childDetails;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Serializable serializable = savedInstanceState.getSerializable("child_details");
        if (serializable instanceof CommonPersonObjectClient) {
            this.childDetails = (CommonPersonObjectClient) serializable;
        }

    }

    @Override
    public void onRegistrationSaved(boolean isEdit) {
        if (isEdit) {//On edit mode refresh view
            Utils.startAsyncTask(new LoadAsyncTask(detailsMap, childDetails, this, childDataFragment, childUnderFiveFragment, overflow), null);//Loading data here because we affect state of the menu item

            //To Do optimize with
            // childDataFragment.refreshRecyclerViewData(detailsMap);

        }
    }

    public Context getContext() {
        return this;
    }

    public BaseChildRegistrationDataFragment getChildDataFragment() {
        return childDataFragment;
    }

    public ChildUnderFiveFragment getChildUnderFiveFragment() {
        return childUnderFiveFragment;
    }

    public void startFormActivity(String formData) {

        Form formParam = new Form();
        formParam.setWizard(true);
        formParam.setHideSaveLabel(true);
        formParam.setNextLabel("");

        startFormActivity(formData, formParam);

    }

    public void startFormActivity(String formData, Form formParam) {
        Intent intent = new Intent(getApplicationContext(), Utils.metadata().childFormActivity);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, formParam);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.JSON, formData);
        if (Boolean.parseBoolean(ChildLibrary.getInstance().getProperties()
                .getProperty(ChildAppProperties.KEY.MULTI_LANGUAGE_SUPPORT, "false"))) {
            intent.putExtra(JsonFormConstants.PERFORM_FORM_TRANSLATION, true);
        }
        startActivityForResult(intent, REQUEST_CODE_GET_JSON);
    }

    public List<Map.Entry<String, String>> getExtraChildVaccines() {
        return extraChildVaccines;
    }

    public void setExtraChildVaccines(List<Map.Entry<String, String>> extraChildVaccines) {
        this.extraChildVaccines = extraChildVaccines;
    }

    public class ServiceHolder {
        public View view;
        public ServiceWrapper wrapper;

        public ServiceHolder(ServiceWrapper wrapper, View view) {
            this.view = view;
            this.wrapper = wrapper;
        }
    }
}