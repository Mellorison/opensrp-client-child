package org.smartregister.child.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.contract.IMotherLookup;
import org.smartregister.child.fragment.ChildFormFragment;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.MotherLookUpUtils;
import org.smartregister.child.util.Utils;
import org.smartregister.clientandeventmodel.DateUtil;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.util.LangUtils;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 01/03/2019.
 */
public class BaseChildFormActivity extends JsonFormActivity implements IMotherLookup {
    private ChildFormFragment childFormFragment;
    private boolean enableOnCloseDialog = true;
    private JSONObject form;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        String language = LangUtils.getLanguage(base);
        super.attachBaseContext(LangUtils.setAppLocale(base, language));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            form = new JSONObject(currentJsonState());
        } catch (JSONException e) {
            Timber.e(e.toString());
        }

        enableOnCloseDialog = getIntent().getBooleanExtra(Constants.FormActivity.EnableOnCloseDialog, true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {

            String et = form.getString(ChildJsonFormUtils.ENCOUNTER_TYPE);

            confirmCloseTitle = getString(R.string.confirm_form_close);
            confirmCloseMessage = et.trim().toLowerCase().contains("update") ? this.getString(R.string.any_changes_you_make) : this.getString(R.string.confirm_form_close_explanation);

            setConfirmCloseTitle(confirmCloseTitle);
            setConfirmCloseMessage(confirmCloseMessage);


        } catch (JSONException e) {
            Timber.e(e.toString());
        }
    }

    @Override
    public void initializeFormFragment() {
        initializeFormFragmentCore();
    }

    protected void initializeFormFragmentCore() {
        childFormFragment = ChildFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction().add(com.vijay.jsonwizard.R.id.container, childFormFragment).commit();
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        if (toolbar != null) {
            toolbar.setContentInsetStartWithNavigation(0);
        }
        super.setSupportActionBar(toolbar);
    }

    @Override
    public void writeValue(String stepName, String key, String value, String openMrsEntityParent, String openMrsEntity,
                           String openMrsEntityId, boolean popup) throws JSONException {
        super.writeValue(stepName, key, value, openMrsEntityParent, openMrsEntity, openMrsEntityId, popup);
        if (ChildLibrary.getInstance().metadata().formWizardValidateRequiredFieldsBefore) {
            validateActivateNext();
        }
    }

    @Override
    public void writeValue(String stepName, String parentKey, String childObjectKey, String childKey, String value,
                           String openMrsEntityParent, String openMrsEntity, String openMrsEntityId, boolean popup)
            throws JSONException {
        super.writeValue(stepName, parentKey, childObjectKey, childKey, value, openMrsEntityParent, openMrsEntity,
                openMrsEntityId, popup);
        if (ChildLibrary.getInstance().metadata().formWizardValidateRequiredFieldsBefore) {
            validateActivateNext();
        }
    }

    @Override
    public void writeValue(String stepName, String key, String value, String openMrsEntityParent, String openMrsEntity,
                           String openMrsEntityId) throws JSONException {
        super.writeValue(stepName, key, value, openMrsEntityParent, openMrsEntity, openMrsEntityId);
        if (ChildLibrary.getInstance().metadata().formWizardValidateRequiredFieldsBefore) {
            validateActivateNext();
        }
    }

    @Override
    public void writeValue(String stepName, String parentKey, String childObjectKey, String childKey, String value,
                           String openMrsEntityParent, String openMrsEntity, String openMrsEntityId) throws JSONException {
        super.writeValue(stepName, parentKey, childObjectKey, childKey, value, openMrsEntityParent, openMrsEntity,
                openMrsEntityId);
        if (ChildLibrary.getInstance().metadata().formWizardValidateRequiredFieldsBefore) {
            validateActivateNext();
        }
    }

    /**
     * Conditionaly display the confirmation dialog
     */
    @Override
    public void onBackPressed() {
        if (enableOnCloseDialog) {
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppThemeAlertDialog).setTitle(confirmCloseTitle)
                    .setMessage(confirmCloseMessage).setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BaseChildFormActivity.this.finish();
                        }
                    }).setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Timber.d("No button on dialog in %s", JsonFormActivity.class.getCanonicalName());
                        }
                    }).create();

            dialog.show();

        } else {
            BaseChildFormActivity.this.finish();
        }
    }

    public void validateActivateNext() {
        Fragment fragment = getVisibleFragment();
        if (fragment instanceof ChildFormFragment) {
            ((ChildFormFragment) fragment).validateActivateNext();
        }
    }

    public Fragment getVisibleFragment() {
        List<Fragment> fragments = this.getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible()) return fragment;
        }
        return null;
    }

    public boolean checkIfBalanceNegative() {
        boolean balancecheck = true;
        String balanceString = childFormFragment.getRelevantTextViewString(Constants.BALANCE);

        if (balanceString.contains(Constants.NEW_BALANCE) && StringUtils.isNumeric(balanceString)) {
            int balance = Integer.parseInt(balanceString.replace(Constants.NEW_BALANCE_, "").trim());
            if (balance < 0) {
                balancecheck = false;
            }
        }

        return balancecheck;
    }

    public boolean checkIfAtLeastOneServiceGiven() {
        JSONObject object = getStep(Constants.STEP_1);
        try {
            if (object.getString(Constants.TITLE).contains("Record out of catchment area service")) {
                JSONArray fields = object.getJSONArray(Constants.FIELDS);
                for (int i = 0; i < fields.length(); i++) {
                    JSONObject vaccineGroup = fields.getJSONObject(i);
                    if (vaccineGroup.has(Constants.KEY.KEY) && vaccineGroup.has(Constants.IS_VACCINE_GROUP)) {
                        if (vaccineGroup.getBoolean(Constants.IS_VACCINE_GROUP) && vaccineGroup.has(Constants.OPTIONS)) {
                            JSONArray vaccineOptions = vaccineGroup.getJSONArray(Constants.OPTIONS);
                            for (int j = 0; j < vaccineOptions.length(); j++) {
                                JSONObject vaccineOption = vaccineOptions.getJSONObject(j);
                                if (vaccineOption.has(Constants.VALUE) && vaccineOption.getBoolean(Constants.VALUE)) {
                                    return true;
                                }
                            }
                        }
                    } else if (vaccineGroup.has(Constants.KEY.KEY) &&
                            vaccineGroup.getString(Constants.KEY.KEY).equals(Constants.WEIGHT_KG) &&
                            vaccineGroup.has(Constants.VALUE) && vaccineGroup.getString(Constants.VALUE).length() > 0) {
                        return true;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String lookUpQuery(Map<String, String> entityMap, String tableName) {

        String[] lookupColumns = new String[]{Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + MotherLookUpUtils.RELATIONALID, Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + MotherLookUpUtils.DETAILS, Constants.KEY.ZEIR_ID, Constants.KEY.FIRST_NAME, Constants.KEY.LAST_NAME,
                Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + AllConstants.ChildRegistrationFields.GENDER,
                Constants.KEY.DOB,
                MotherLookUpUtils.NRC_NUMBER,
                MotherLookUpUtils.MOTHER_GUARDIAN_PHONE_NUMBER.toLowerCase(Locale.ENGLISH),
                Utils.metadata().getRegisterQueryProvider().getMotherDetailsTable() + "." + "is_consented",
                Utils.metadata().getRegisterQueryProvider().getMotherDetailsTable() + "." + "preferred_language",
                Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + "residential_area",
                Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + "residential_area_other",
                Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + "residential_address",
                Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + Constants.KEY.BASE_ENTITY_ID};

        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, lookupColumns);
        queryBuilder.customJoin(" join " + Utils.metadata().getRegisterQueryProvider().getChildDetailsTable() + " on " + Utils.metadata().getRegisterQueryProvider().getChildDetailsTable() + "." + Constants.KEY.RELATIONAL_ID + "=" + Utils.metadata().getRegisterQueryProvider().getMotherDetailsTable() + "." + Constants.KEY.BASE_ENTITY_ID +
                " join " + Utils.metadata().getRegisterQueryProvider().getMotherDetailsTable() + " on " + Utils.metadata().getRegisterQueryProvider().getMotherDetailsTable() + "." + Constants.KEY.BASE_ENTITY_ID + " = " + Utils.metadata().getRegisterQueryProvider().getDemographicTable() + "." + Constants.KEY.BASE_ENTITY_ID);
        String query = queryBuilder.mainCondition(getMainConditionString(entityMap));

        // Make the id distinct
        query = query.replace("ec_client.id as _id", "distinct(ec_client.id) as _id");

        return queryBuilder.Endquery(query);
    }

    protected static String getMainConditionString(Map<String, String> entityMap) {

        String mainConditionString = "";
        for (Map.Entry<String, String> entry : entityMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (StringUtils.containsIgnoreCase(key, MotherLookUpUtils.firstName)) {
                key = MotherLookUpUtils.firstName;
            }

            if (StringUtils.containsIgnoreCase(key, MotherLookUpUtils.lastName)) {
                key = MotherLookUpUtils.lastName;
            }

            if (StringUtils.equalsIgnoreCase(key, MotherLookUpUtils.MOTHER_GUARDIAN_PHONE_NUMBER)) {
                key = MotherLookUpUtils.MOTHER_GUARDIAN_PHONE_NUMBER.toLowerCase(Locale.ENGLISH);
            }

            if (StringUtils.equalsIgnoreCase(key, MotherLookUpUtils.MOTHER_GUARDIAN_NRC)) {
                key = MotherLookUpUtils.NRC_NUMBER;
            }


            if (StringUtils.containsIgnoreCase(key, MotherLookUpUtils.birthDate)) {
                if (!isDate(value)) {
                    continue;
                }
                key = MotherLookUpUtils.dob;
            }

            if (!key.equals(MotherLookUpUtils.dob)) {
                if (StringUtils.isBlank(mainConditionString)) {
                    mainConditionString += " " + key + " Like '%" + value + "%'";
                } else {
                    mainConditionString += " AND " + key + " Like '%" + value + "%'";

                }
            } else {
                if (StringUtils.isBlank(mainConditionString)) {
                    mainConditionString += " cast(" + key + " as date) " + " =  cast('" + value + "'as date) ";
                } else {
                    mainConditionString += " AND cast(" + key + " as date) " + " =  cast('" + value + "'as date) ";

                }
            }
        }

        return mainConditionString;

    }

    public static boolean isDate(String dobString) {
        try {
            DateUtil.yyyyMMdd.parse(dobString);
            return true;
        } catch (ParseException e) {
            return false;
        }

    }
}

