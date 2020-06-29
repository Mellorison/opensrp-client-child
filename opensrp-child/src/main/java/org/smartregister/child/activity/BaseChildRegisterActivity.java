package org.smartregister.child.activity;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.View;

import com.google.android.gms.vision.barcode.Barcode;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.contract.ChildRegisterContract;
import org.smartregister.child.domain.UpdateRegisterParams;
import org.smartregister.child.fragment.BaseAdvancedSearchFragment;
import org.smartregister.child.fragment.BaseChildRegisterFragment;
import org.smartregister.child.listener.ChildBottomNavigationListener;
import org.smartregister.child.util.ChildAppProperties;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.activity.BaseRegisterActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 25/02/2019.
 */
public abstract class BaseChildRegisterActivity extends BaseRegisterActivity implements ChildRegisterContract.View, ChildRegisterContract.ProgressDialogCallback {
    public static final String TAG = BaseChildRegisterActivity.class.getCanonicalName();


    protected boolean isAdvancedSearch = false;
    protected HashMap<String, String> advancedSearchFormData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public Context getOpenSRPContext() {
        return CoreLibrary.getInstance().context();
    }

    @Override
    protected void registerBottomNavigation() {

        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(ChildLibrary.getInstance().getProperties()
                .getPropertyBoolean(ChildAppProperties.KEY.FEATURE_BOTTOM_NAVIGATION_ENABLED) ? View.VISIBLE : View.GONE);

        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);

            //Bottom nav supports 5 items max, so we remove em first , then inflate
            bottomNavigationView.getMenu().removeItem(R.id.action_clients);
            bottomNavigationView.getMenu().removeItem(R.id.action_register);
            bottomNavigationView.getMenu().removeItem(R.id.action_search);
            bottomNavigationView.getMenu().removeItem(R.id.action_library);

            bottomNavigationView.inflateMenu(R.menu.bottom_nav_child_menu);
            bottomNavigationHelper.disableShiftMode(bottomNavigationView);

            if (!ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_SCAN_QR_ENABLED)) {
                bottomNavigationView.getMenu().removeItem(R.id.action_scan_qr);
            }

            if (!ChildLibrary.getInstance().getProperties().getPropertyBoolean(ChildAppProperties.KEY.FEATURE_NFC_CARD_ENABLED)) {
                bottomNavigationView.getMenu().removeItem(R.id.action_scan_card);
            }

            ChildBottomNavigationListener childBottomNavigationListener = new ChildBottomNavigationListener(this);
            bottomNavigationView.setOnNavigationItemSelectedListener(childBottomNavigationListener);

        }
    }

    @Override
    public void onBackPressed() {
        if (currentPage == 0) {
            super.onBackPressed();
        } else {
            switchToBaseFragment();
            setSelectedBottomBarMenuItem(R.id.action_home);
        }
    }

    @Override
    protected Fragment[] getOtherFragments() {

        return null;
    }

    @Override
    public void startFormActivity(String formName, String entityId, String metaData) {
        try {
            if (mBaseFragment instanceof BaseChildRegisterFragment) {
                String locationId = Utils.context().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
                presenter().startForm(formName, entityId, metaData, locationId);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            displayToast(getString(R.string.error_unable_to_start_form));
        }
    }

    @Override
    public void startFormActivity(String formName, String entityId, Map<String, String> metaData) {
        try {
            if (mBaseFragment instanceof BaseChildRegisterFragment) {
                String locationId = Utils.context().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
                presenter().startForm(formName, entityId, metaData, locationId);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            displayToast(getString(R.string.error_unable_to_start_form));
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, Utils.metadata().childFormActivity);
        intent.putExtra(Constants.INTENT_KEY.JSON, jsonForm.toString());

        Form form = new Form();
        form.setWizard(true);
        form.setHideSaveLabel(false);
        form.setNextLabel("");

        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, ChildJsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AllConstants.BARCODE.BARCODE_REQUEST_CODE && resultCode == RESULT_OK && isAdvancedSearch) {
            Barcode barcode = data.getParcelableExtra(AllConstants.BARCODE.BARCODE_KEY);
            String barcodeSearchTerm = barcode.displayValue;
            barcodeSearchTerm =
                    barcodeSearchTerm.contains("/") ? barcodeSearchTerm.substring(barcodeSearchTerm.lastIndexOf('/') + 1) :
                            barcodeSearchTerm;

            //Set value and refresh form values!
            advancedSearchFormData.put(Constants.KEY.ZEIR_ID, barcodeSearchTerm);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    protected void onActivityResultExtended(int requestCode, int resultCode, Intent data) {
        if (requestCode == ChildJsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            try {
                String jsonString = data.getStringExtra(Constants.INTENT_KEY.JSON);
                Timber.d(jsonString);

                JSONObject form = new JSONObject(jsonString);
                if (form.getString(ChildJsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().childRegister.registerEventType)) {
                    UpdateRegisterParams updateRegisterParam = new UpdateRegisterParams();
                    updateRegisterParam.setEditMode(false);
                    updateRegisterParam.setFormTag(ChildJsonFormUtils.formTag(Utils.context().allSharedPreferences()));

                    showProgressDialog(R.string.saving_dialog_title);
                    presenter().saveForm(jsonString, updateRegisterParam);
                } else if (form.getString(ChildJsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().childRegister.outOfCatchmentServiceEventType)) {

                    showProgressDialog(R.string.saving_dialog_title);
                    presenter().saveOutOfCatchmentService(jsonString, this);

                }
            } catch (Exception e) {
                Timber.e(Log.getStackTraceString(e));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        onChildRegisterResumption();
    }

    protected void onChildRegisterResumption() {
        if (isAdvancedSearch) {
            refreshAdvancedSearchFormValues();
            switchToAdvancedSearchFromRegister();

        } else {
            setSelectedBottomBarMenuItem(R.id.action_home);
        }
    }

    protected void refreshAdvancedSearchFormValues() {
        setFormData(this.advancedSearchFormData);
        ((BaseAdvancedSearchFragment) findFragmentByPosition(ADVANCED_SEARCH_POSITION)).assignedValuesBeforeBarcode();
    }

    /**
     * Forces the Home register activity to open the the Advanced search fragment after the barcode or other activity is
     * closed (as long as it was opened from the advanced search page)
     */
    protected void switchToAdvancedSearchFromRegister() {
        if (ChildLibrary.getInstance().getProperties()
                .getPropertyBoolean(ChildAppProperties.KEY.FEATURE_BOTTOM_NAVIGATION_ENABLED)) {
            setSelectedBottomBarMenuItem(org.smartregister.child.R.id.action_search);
        } else {

            switchToFragment(ADVANCED_SEARCH_POSITION);
        }

        setFormData(advancedSearchFormData);
        isAdvancedSearch = false;
        advancedSearchFormData = new HashMap<>();

    }

    private void setFormData(HashMap<String, String> formData) {
        ((BaseAdvancedSearchFragment) findFragmentByPosition(ADVANCED_SEARCH_POSITION)).setAdvancedSearchFormData(formData);
    }

    @Override
    public List<String> getViewIdentifiers() {
        return Arrays.asList(Utils.metadata().childRegister.config);
    }

    @Override
    public ChildRegisterContract.Presenter presenter() {
        return (ChildRegisterContract.Presenter) presenter;
    }

    @Override
    public void startRegistration() {
        //setSelectedBottomBarMenuItem(R.id.action_register);
        startFormActivity(getRegistrationForm(), null, "");
    }

    public abstract String getRegistrationForm();

    public void setAdvancedSearch(boolean advancedSearch) {
        isAdvancedSearch = advancedSearch;
    }

    public void setAdvancedSearchFormData(HashMap<String, String> advancedSearchFormData) {
        this.advancedSearchFormData = advancedSearchFormData;
    }

    public void startAdvancedSearch() {
        try {
            // mPager.setCurrentItem(ADVANCED_SEARCH_POSITION, false);
            setSelectedBottomBarMenuItem(org.smartregister.child.R.id.action_search);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    public abstract void startNFCCardScanner();

    //To be overriden
    public void saveForm(String jsonString, UpdateRegisterParams updateRegisterParam) {
        presenter().saveForm(jsonString, updateRegisterParam);
    }

    @Override
    public void dissmissProgressDialog() {
        hideProgressDialog();

    }

}