package org.smartregister.child.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import androidx.annotation.StringRes;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.opensrp.api.constants.Gender;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.child.R;
import org.smartregister.child.contract.ChildRegisterContract;
import org.smartregister.child.domain.ChildEventClient;
import org.smartregister.child.domain.UpdateRegisterParams;
import org.smartregister.child.interactor.ChildRegisterInteractor;
import org.smartregister.child.model.BaseChildRegisterModel;
import org.smartregister.child.toolbar.BaseToolbar;
import org.smartregister.child.toolbar.LocationSwitcherToolbar;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Utils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.FetchStatus;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.view.activity.MultiLanguageActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Base activity class for all other PATH activity classes. Implements: - A uniform navigation bar that is launched by
 * swiping from the left - Support for specifying which {@link BaseToolbar} to use
 * <p/>
 * This activity requires that the base view for any child activity be {@link DrawerLayout} Make sure include the navigation
 * view as the last element in the activity's root DrawerLayout like this:
 * <p/>
 * <include layout="@layout/nav_view_base"/>
 * <p/>
 * Created by Jason Rogena - jrogena@ona.io on 16/02/2017.
 */
public abstract class BaseActivity extends MultiLanguageActivity
        implements NavigationView.OnNavigationItemSelectedListener, SyncStatusBroadcastReceiver.SyncStatusListener,
        ChildRegisterContract.InteractorCallBack {

    public static final int REQUEST_CODE_GET_JSON = 3432;
    public static final String INACTIVE = "inactive";
    public static final String LOST_TO_FOLLOW_UP = "lost_to_follow_up";
    private static final String TAG = "BaseActivity";
    private BaseToolbar toolbar;
    private Menu menu;
    private Snackbar syncStatusSnackbar;
    private ProgressDialog progressDialog;
    //  private NavigationItemListener navigationItemListener;
    // private CustomNavigationBarListener customNavigationBarListener;
    private ArrayList<Notification> notifications;
    private BaseActivityToggle toggle;
    private ChildRegisterContract.Interactor interactor;
    private BaseChildRegisterModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        toolbar = findViewById(getToolbarId());
        setSupportActionBar(toolbar);

     /*   DrawerLayout drawer = (DrawerLayout) findViewById(getDrawerLayoutId());
        toggle = new BaseActivityToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
toggle.syncState();
       */

        interactor = new ChildRegisterInteractor();
        model = new BaseChildRegisterModel();

        //NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        //avigationView.setNavigationItemSelectedListener(this);

        notifications = new ArrayList<>();

        initializeProgressDialog();


        //  navigationItemListener = new NavigationItemListener(this, toolbar);
        //customNavigationBarListener = new CustomNavigationBarListener(this, toolbar);
    }

    /**
     * The layout resource file to user for this activity
     *
     * @return The resource id for the layout file to use
     */
    protected abstract int getContentView();

    /**
     * The id for the toolbar used in this activity
     *
     * @return The id for the toolbar used
     */
    protected abstract int getToolbarId();

    private void initializeProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getString(R.string.saving_dialog_title));
        progressDialog.setMessage(getString(R.string.please_wait_message));
    }

    @Override
    public void onSyncStart() {
        refreshSyncStatusViews(null);
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        refreshSyncStatusViews(fetchStatus);
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        refreshSyncStatusViews(fetchStatus);
    }

    /////////////////////////for custom navigation //////////////////////////////////////////////////////
    private void refreshSyncStatusViews(FetchStatus fetchStatus) {/*
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null && navigationView.getMenu() != null) {
            LinearLayout syncMenuItem = (LinearLayout) navigationView.findViewById(R.id.nav_sync);
            if (syncMenuItem != null) {
                if (SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                    ViewGroup rootView = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
                    if (syncStatusSnackbar != null) syncStatusSnackbar.dismiss();
                    syncStatusSnackbar = Snackbar.make(rootView, R.string.syncing,
                            Snackbar.LENGTH_LONG);
                    syncStatusSnackbar.show();
                    ((TextView) syncMenuItem.findViewById(R.id.nav_synctextview)).setText(R.string.syncing);
                } else {
                    if (fetchStatus != null) {
                        if (syncStatusSnackbar != null) syncStatusSnackbar.dismiss();
                        ViewGroup rootView = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
                        if (fetchStatus.equals(FetchStatus.fetchedFailed)) {
                            syncStatusSnackbar = Snackbar.make(rootView, R.string.sync_failed, Snackbar.LENGTH_INDEFINITE);
                            syncStatusSnackbar.setActionTextColor(getResources().getColor(R.color.snackbar_action_color));
                            syncStatusSnackbar.setAction(R.string.retry, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startSync();
                                }
                            });
                        } else if (fetchStatus.equals(FetchStatus.fetched)
                                || fetchStatus.equals(FetchStatus.nothingFetched)) {
                            syncStatusSnackbar = Snackbar.make(rootView, R.string.sync_complete, Snackbar.LENGTH_LONG);
                        } else if (fetchStatus.equals(FetchStatus.noConnection)) {
                            syncStatusSnackbar = Snackbar.make(rootView, R.string.sync_failed_no_internet, Snackbar
                            .LENGTH_LONG);
                        }
                        syncStatusSnackbar.show();
                    }

                    updateLastSyncText();
                }
            }
        }*/
    }

    public BaseToolbar getBaseToolbar() {
        return toolbar;
    }

    protected ActionBarDrawerToggle getDrawerToggle() {
        return toggle;
    }

    protected void openDrawer() {
        DrawerLayout drawer = findViewById(getDrawerLayoutId());
        drawer.openDrawer(Gravity.LEFT);
    }

    /**
     * The id for the base {@link DrawerLayout} for the activity
     *
     * @return
     */
    protected abstract int getDrawerLayoutId();

    private void updateLastSyncText() {/*
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null && navigationView.getMenu() != null) {
            TextView syncMenuItem = ((TextView) navigationView.findViewById(R.id.nav_synctextview));
            if (syncMenuItem != null) {
                String lastSync = getLastSyncTime();

                if (!TextUtils.isEmpty(lastSync)) {
                    lastSync = " " + String.format(getString(R.string.last_sync), lastSync);
                }
                syncMenuItem.setText(String.format(getString(R.string.sync_), lastSync));
            }
        }*/
    }

    private void initializeCustomNavbarLIsteners() {

/*
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        LinearLayout syncMenuItem = (LinearLayout) drawer.findViewById(R.id.nav_sync);
        syncMenuItem.setOnClickListener(customNavigationBarListener);

        LinearLayout addchild = (LinearLayout) drawer.findViewById(R.id.nav_register);
        addchild.setOnClickListener(customNavigationBarListener);

        LinearLayout outofcatchment = (LinearLayout) drawer.findViewById(R.id.nav_record_vaccination_out_catchment);
        outofcatchment.setOnClickListener(customNavigationBarListener);

        LinearLayout stockregister = (LinearLayout) drawer.findViewById(R.id.stock_control);
        stockregister.setOnClickListener(customNavigationBarListener);

        LinearLayout childregister = (LinearLayout) drawer.findViewById(R.id.child_register);
        childregister.setOnClickListener(customNavigationBarListener);

        LinearLayout hia2 = (LinearLayout) drawer.findViewById(R.id.hia2_reports);
        hia2.setOnClickListener(customNavigationBarListener);

        LinearLayout coverage = (LinearLayout) drawer.findViewById(R.id.coverage_reports);
        coverage.setOnClickListener(customNavigationBarListener);

        LinearLayout dropout = (LinearLayout) drawer.findViewById(R.id.dropout_reports);
        dropout.setOnClickListener(customNavigationBarListener);
*/
    }

    private String getLastSyncTime() {
        String lastSync = "";
        long milliseconds = ECSyncHelper.getInstance(this).getLastCheckTimeStamp();
        if (milliseconds > 0) {
            DateTime lastSyncTime = new DateTime(milliseconds);
            DateTime now = new DateTime(Calendar.getInstance());
            Minutes minutes = Minutes.minutesBetween(lastSyncTime, now);
            if (minutes.getMinutes() < 1) {
                Seconds seconds = Seconds.secondsBetween(lastSyncTime, now);
                lastSync = seconds.getSeconds() + "s";
            } else if (minutes.getMinutes() >= 1 && minutes.getMinutes() < 60) {
                lastSync = minutes.getMinutes() + "m";
            } else if (minutes.getMinutes() >= 60 && minutes.getMinutes() < 1440) {
                Hours hours = Hours.hoursBetween(lastSyncTime, now);
                lastSync = hours.getHours() + "h";
            } else {
                Days days = Days.daysBetween(lastSyncTime, now);
                lastSync = days.getDays() + "d";
            }
        }
        return lastSync;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (toolbar.getSupportedMenu() != 0) {
            this.menu = menu;
            getMenuInflater().inflate(toolbar.getSupportedMenu(), menu);
            toolbar.prepareMenu();
            return super.onCreateOptionsMenu(menu);
        } else {
            toolbar.prepareMenu();
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(toolbar.onMenuItemSelected(item));
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        //  return navigationItemListener.onNavigationItemSelected(item);
        return false;
    }

    private void startSync() {
        SyncServiceJob.scheduleJobImmediately(SyncServiceJob.TAG);
    }

    /**
     * Updates all gender affected views
     *
     * @param gender The gender to update the
     */
    protected int[] updateGenderViews(Gender gender) {
        int darkShade = R.color.gender_neutral_dark_green;
        int normalShade = R.color.gender_neutral_green;
        int lightSade = R.color.gender_neutral_light_green;

        if (gender.equals(Gender.FEMALE)) {
            darkShade = R.color.female_dark_pink;
            normalShade = R.color.female_pink;
            lightSade = R.color.female_light_pink;
        } else if (gender.equals(Gender.MALE)) {
            darkShade = R.color.male_dark_blue;
            normalShade = R.color.male_blue;
            lightSade = R.color.male_light_blue;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(darkShade));
        }
        toolbar.setBackground(new ColorDrawable(getResources().getColor(normalShade)));
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
        viewGroup.setBackground(new ColorDrawable(getResources().getColor(lightSade)));

        return new int[]{darkShade, normalShade, lightSade};
    }

    protected void startJsonForm(String formName, String entityId) {
        try {
            if (toolbar instanceof LocationSwitcherToolbar) {
                LocationSwitcherToolbar locationSwitcherToolbar = (LocationSwitcherToolbar) toolbar;
                String locationId =
                        Utils.context().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);

                startJsonForm(formName, entityId, locationId);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    protected void startJsonForm(String formName, String entityId, String locationId) throws Exception {
        ChildJsonFormUtils.startForm(this, REQUEST_CODE_GET_JSON, formName, entityId, locationId);
    }

    protected void showNotification(int message, int notificationIcon, int positiveButtonText,
                                    View.OnClickListener positiveButtonClick, int negativeButtonText,
                                    View.OnClickListener negativeButtonClick, Object tag) {
        String posBtnText = null;
        if (positiveButtonText != 0 && positiveButtonClick != null) {
            posBtnText = getString(positiveButtonText);
        }

        String negBtnText = null;
        if (negativeButtonText != 0 && negativeButtonClick != null) {
            negBtnText = getString(negativeButtonText);
        }

        showNotification(getString(message), getResources().getDrawable(notificationIcon), posBtnText, positiveButtonClick,
                negBtnText, negativeButtonClick, tag);
    }

    private void showNotification(String message, Drawable notificationIcon, String positiveButtonText,
                                  View.OnClickListener positiveButtonOnClick, String negativeButtonText,
                                  View.OnClickListener negativeButtonOnClick, Object tag) {
        Notification notification =
                new Notification(message, notificationIcon, positiveButtonText, positiveButtonOnClick, negativeButtonText,
                        negativeButtonOnClick, tag);

        // Add the notification as the last element in the notification list
        String notificationMessage = notification.message;
        if (notificationMessage == null) notificationMessage = "";
        for (Notification curNotification : notifications) {
            if (notificationMessage.equals(curNotification.message)) {
                notifications.remove(curNotification);
            }
        }
        notifications.add(notification);

        updateNotificationViews(notification);
    }

    private void updateNotificationViews(final Notification notification) {
        TextView notiMessage = findViewById(R.id.noti_message);
        notiMessage.setText(notification.message);
        Button notiPositiveButton = findViewById(R.id.noti_positive_button);
        notiPositiveButton.setTag(notification.tag);
        if (notification.positiveButtonText != null) {
            notiPositiveButton.setVisibility(View.VISIBLE);
            notiPositiveButton.setText(notification.positiveButtonText);
            notiPositiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (notifications.size() > 0) {
                        notifications.remove(notifications.size() - 1);
                    }

                    if (notification.positiveButtonOnClick != null) {
                        notification.positiveButtonOnClick.onClick(v);
                    }

                    // Show the second last notification
                    if (notifications.size() > 0) {
                        updateNotificationViews(notifications.get(notifications.size() - 1));
                    }
                }
            });
        } else {
            notiPositiveButton.setVisibility(View.GONE);
        }

        Button notiNegativeButton = findViewById(R.id.noti_negative_button);
        notiNegativeButton.setTag(notification.tag);
        if (notification.negativeButtonText != null) {
            notiNegativeButton.setVisibility(View.VISIBLE);
            notiNegativeButton.setText(notification.negativeButtonText);
            notiNegativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (notifications.size() > 0) {
                        notifications.remove(notifications.size() - 1);
                    }

                    if (notification.negativeButtonOnClick != null) {
                        notification.negativeButtonOnClick.onClick(v);
                    }

                    // Show the second last notification
                    if (notifications.size() > 0) {
                        updateNotificationViews(notifications.get(notifications.size() - 1));
                    }
                }
            });
        } else {
            notiNegativeButton.setVisibility(View.GONE);
        }

        ImageView notiIcon = findViewById(R.id.noti_icon);
        if (notification.notificationIcon != null) {
            notiIcon.setVisibility(View.VISIBLE);
            notiIcon.setImageDrawable(notification.notificationIcon);
        } else {
            notiIcon.setVisibility(View.GONE);
        }

        final LinearLayout notificationLL = findViewById(R.id.notification);

        Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                notificationLL.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        notificationLL.clearAnimation();
        notificationLL.startAnimation(slideDownAnimation);
    }

    protected void hideNotification() {
        final LinearLayout notification = findViewById(R.id.notification);
        if (notification.getVisibility() == View.VISIBLE) {
            Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    notification.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            notification.startAnimation(slideUpAnimation);
        }
    }

    public Context getOpenSRPContext() {
        return CoreLibrary.getInstance().context();
    }

    protected boolean isActiveStatus(CommonPersonObjectClient child) {
        String humanFriendlyStatus = getHumanFriendlyChildsStatus(child);
        return isActiveStatus(humanFriendlyStatus);
    }

    protected String getHumanFriendlyChildsStatus(CommonPersonObjectClient child) {
        Map<String, String> detailsMap = child.getColumnmaps();
        return getHumanFriendlyChildsStatus(detailsMap);
    }

    protected boolean isActiveStatus(String humanFriendlyStatus) {
        return getString(R.string.active).equals(humanFriendlyStatus);
    }

    protected String getHumanFriendlyChildsStatus(Map<String, String> detailsColumnMap) {
        String status = getString(R.string.active);
        if (detailsColumnMap.containsKey(INACTIVE) && detailsColumnMap.get(INACTIVE) != null &&
                detailsColumnMap.get(INACTIVE).equalsIgnoreCase(Boolean.TRUE.toString())) {
            status = getString(R.string.inactive);
        } else if (detailsColumnMap.containsKey(LOST_TO_FOLLOW_UP) && detailsColumnMap.get(LOST_TO_FOLLOW_UP) != null &&
                detailsColumnMap.get(LOST_TO_FOLLOW_UP).equalsIgnoreCase(Boolean.TRUE.toString())) {
            status = getString(R.string.lost_to_follow_up);
        }

        return status;
    }

    protected void showChildsStatus(String status) {
        LinearLayout linearLayout = findViewById(R.id.ll_inactive_status_bar_layout);
        boolean isStatusActive = getString(R.string.active).equals(status);

        if (linearLayout != null) {
            linearLayout.setVisibility((isStatusActive) ? View.GONE : View.VISIBLE);

            if (!isStatusActive) {
                TextView textView = findViewById(R.id.tv_inactive_status_bar_status_text);

                if (textView != null) {
                    textView.setText(String.format(getString(R.string.status_text), status));
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String jsonString = data.getStringExtra("json");


            UpdateRegisterParams updateRegisterParams = new UpdateRegisterParams();
            updateRegisterParams.setEditMode(false);

            saveForm(jsonString, updateRegisterParams);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(getDrawerLayoutId());
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSyncStatusBroadcastReceiver();
    }

    private void unregisterSyncStatusBroadcastReceiver() {
        SyncStatusBroadcastReceiver.getInstance().removeSyncStatusListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSyncStatusBroadcastReceiver();
    }

    private void registerSyncStatusBroadcastReceiver() {
        SyncStatusBroadcastReceiver.getInstance().addSyncStatusListener(this);
    }

    public void saveForm(String jsonString, UpdateRegisterParams updateRegisterParams) {

        try {

            if (updateRegisterParams.getFormTag() == null) {
                updateRegisterParams.setFormTag(ChildJsonFormUtils.formTag(Utils.getAllSharedPreferences()));
            }

            List<ChildEventClient> childEventClientList =
                    model.processRegistration(jsonString, updateRegisterParams.getFormTag());
            if (childEventClientList == null || childEventClientList.isEmpty()) {
                return;
            }
            interactor.saveRegistration(childEventClientList, jsonString, updateRegisterParams, this);

        } catch (Exception e) {
            Timber.e(Log.getStackTraceString(e));
        }
    }

    protected BaseToolbar getToolbar() {
        return toolbar;
    }

    public Menu getMenu() {
        return menu;
    }

    /**
     * The activity to go back to
     *
     * @return
     */
    protected abstract Class onBackActivity();

    public void processInThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(runnable).start();
        }
    }

    public void showProgressDialog() {
        showProgressDialog(getString(R.string.saving_dialog_title), getString(R.string.please_wait_message));
    }

    public void showProgressDialog(String title, String message) {
        if (progressDialog != null) {
            if (StringUtils.isNotBlank(title)) {
                progressDialog.setTitle(title);
            }

            if (StringUtils.isNotBlank(message)) {
                progressDialog.setMessage(message);
            }

            if (!(this).isFinishing()) {

                progressDialog.show();
            }
        }
    }

    public void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private class BaseActivityToggle extends ActionBarDrawerToggle {

        private BaseActivityToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar,
                                   @StringRes int openDrawerContentDescRes, @StringRes int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            if (!SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
                updateLastSyncText();
            }
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
        }
    }

    private class Notification {
        public final String message;
        public final Drawable notificationIcon;
        public final String positiveButtonText;
        public final View.OnClickListener positiveButtonOnClick;
        public final String negativeButtonText;
        public final View.OnClickListener negativeButtonOnClick;
        public final Object tag;

        private Notification(String message, Drawable notificationIcon, String positiveButtonText,
                             View.OnClickListener positiveButtonOnClick, String negativeButtonText,
                             View.OnClickListener negativeButtonOnClick, Object tag) {
            this.message = message;
            this.notificationIcon = notificationIcon;
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonOnClick = positiveButtonOnClick;
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonOnClick = negativeButtonOnClick;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Notification) {
                Notification notification = (Notification) o;
                String message = this.message;
                if (message == null) message = "";
                String positiveButtonText = this.positiveButtonText;
                if (positiveButtonText == null) positiveButtonText = "";
                String negativeButtonText = this.negativeButtonText;
                if (negativeButtonText == null) negativeButtonText = "";

                return message.equals(notification.message) && positiveButtonText.equals(notification.positiveButtonText) &&
                        negativeButtonText.equals(notification.negativeButtonText);
            }
            return false;
        }
    }

}
