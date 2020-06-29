package org.smartregister.child.model;

import android.content.ContentValues;
import android.util.Log;

import androidx.annotation.NonNull;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.contract.ChildRegisterContract;
import org.smartregister.child.domain.ChildEventClient;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.configurableviews.ConfigurableViewsLibrary;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.util.FormUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 25/02/2019.
 */
public class BaseChildRegisterModel implements ChildRegisterContract.Model {
    private FormUtils formUtils;

    @Override
    public void registerViewConfigurations(List<String> viewIdentifiers) {
        ConfigurableViewsLibrary.getInstance().getConfigurableViewsHelper().registerViewConfigurations(viewIdentifiers);
    }

    @Override
    public void unregisterViewConfiguration(List<String> viewIdentifiers) {
        ConfigurableViewsLibrary.getInstance().getConfigurableViewsHelper().unregisterViewConfiguration(viewIdentifiers);
    }

    @Override
    public void saveLanguage(String language) {
        // TODO Save Language
        //Map<String, String> langs = getAvailableLanguagesMap();
        //Utils.saveLanguage(Utils.getKeyByValue(langs, language));
    }

    /*private Map<String, String> getAvailableLanguagesMap() {
        return null;
        //return AncApplication.getJsonSpecHelper().getAvailableLanguagesMap();
    }*/

    @Override
    public String getLocationId(String locationName) {
        return LocationHelper.getInstance().getOpenMrsLocationId(locationName);
    }

    /**
     * Prepare JSON for for editing/creating new registration. For update/editing of existing client you must
     * pass the entity_id of that particular client to the JSON form when injecting values read from the local
     * database; just before opening the form for editing.
     * <p>
     * Also remember to dynamically update the encounter_type of form when editing to match the following format depending on the entities you have:
     * <p>
     * "encounter_type": "Update Birth Registration",
     * "mother": {
     * "encounter_type": "Update Mother Details"
     * },
     * "father": {
     * "encounter_type": "Update Father Details"
     * }
     *
     * @param jsonString json form as string
     * @param formTag    form tags
     * @return a list of ChildEvents
     */
    @Override
    public List<ChildEventClient> processRegistration(@NonNull String jsonString, FormTag formTag) {
        JSONObject form;
        List<ChildEventClient> childEventClientList = new ArrayList<>();
        try {
            form = new JSONObject(jsonString);
            updateEncounterTypes(form);

            ChildEventClient childEventClient = ChildJsonFormUtils.processChildDetailsForm(jsonString, formTag);
            if (childEventClient == null) {
                return null;
            }


            childEventClientList.add(childEventClient);
            Client childClient = childEventClient.getClient();

            String motherRelationalId = ChildJsonFormUtils.getRelationalIdByType(childClient.getBaseEntityId(), Constants.KEY.MOTHER);
            ChildEventClient childMotherEventClient = ChildJsonFormUtils.processMotherRegistrationForm(
                    jsonString, motherRelationalId, childEventClient);

            if (childMotherEventClient != null) {
                if (motherRelationalId == null) {
                    childClient.addRelationship(Constants.KEY.MOTHER, childMotherEventClient.getClient().getBaseEntityId());
                }
                childEventClientList.add(childMotherEventClient);
                // Add search by mother
                ContentValues values = new ContentValues();
                values.put(Constants.KEY.LAST_INTERACTED_WITH, Calendar.getInstance().getTimeInMillis());
                String tableName = Utils.metadata().getRegisterQueryProvider().getDemographicTable();
                Utils.updateLastInteractionWith(childClient.getBaseEntityId(), tableName, values);
                updateMotherDetails(childMotherEventClient, childClient);
            }

            // Add father relationship if defined in metadata
            if (Utils.metadata().childRegister.getFatherRelationKey() != null) {
                String fatherRelationalId = ChildJsonFormUtils.getRelationalIdByType(childClient.getBaseEntityId(), Constants.KEY.FATHER);
                ChildEventClient fatherRegistrationEvent = ChildJsonFormUtils.processFatherRegistrationForm(
                        jsonString, fatherRelationalId, childEventClient);

                if (fatherRegistrationEvent != null) {
                    if (fatherRelationalId == null) {
                        childClient.addRelationship(Constants.KEY.FATHER, fatherRegistrationEvent.getClient().getBaseEntityId());
                    }
                    childEventClientList.add(fatherRegistrationEvent);
                }
            }

        } catch (JSONException e) {
            Timber.e(e, "Error processing registration form");
        }
        return childEventClientList;
    }


    private void updateEncounterTypes(JSONObject form) throws JSONException {
        //Update encounter types/event types when editing form
        if (form.has(ChildJsonFormUtils.ENTITY_ID) && StringUtils.isNotBlank(form.getString(ChildJsonFormUtils.ENTITY_ID))) {
            if (form.has(Constants.KEY.MOTHER)) {
                form.getJSONObject(Constants.KEY.MOTHER).put(JsonFormConstants.ENCOUNTER_TYPE, Constants.EventType.UPDATE_MOTHER_DETAILS);
            }
            if (form.has(Constants.KEY.FATHER)) {
                form.getJSONObject(Constants.KEY.FATHER).put(JsonFormConstants.ENCOUNTER_TYPE, Constants.EventType.UPDATE_FATHER_DETAILS);
            }
        }
    }

    /**
     * Temp fix for data refresh
     * To Do
     */
    private void updateMotherDetails(ChildEventClient childHeadEventClient, Client childClient) {
        //Update details
        //To Do temp find out why some details not updating normally

        String dob = null;
        try {
            dob = Utils.reverseHyphenatedString(Utils.convertDateFormat(childHeadEventClient.getClient().getBirthdate(), new SimpleDateFormat("dd-MM-yyyy")));

        } catch (Exception e) {
            Timber.e(e);
        }
        ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "mother_first_name", childHeadEventClient.getClient().getFirstName(), Calendar.getInstance().getTimeInMillis());
        ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "mother_last_name", childHeadEventClient.getClient().getLastName(), Calendar.getInstance().getTimeInMillis());
        if (dob != null)
            ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "mother_dob", dob, Calendar.getInstance().getTimeInMillis());
        ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "mother_dob_unknown", childHeadEventClient.getClient().getBirthdateApprox() ? "true" : "false", Calendar.getInstance().getTimeInMillis());

        ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "Mother_Guardian_First_Name", childHeadEventClient.getClient().getFirstName(), Calendar.getInstance().getTimeInMillis());
        ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "Mother_Guardian_Last_Name", childHeadEventClient.getClient().getLastName(), Calendar.getInstance().getTimeInMillis());
        if (dob != null)
            ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "Mother_Guardian_Date_Birth", dob, Calendar.getInstance().getTimeInMillis());
        ChildLibrary.getInstance().context().detailsRepository().add(childClient.getBaseEntityId(), "Mother_Guardian_Date_Birth_Unknown", childHeadEventClient.getClient().getBirthdateApprox() ? "true" : "false", Calendar.getInstance().getTimeInMillis());
    }

    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId, Map<String, String> metadata) throws Exception {
        JSONObject form = getFormUtils().getFormJson(formName);
        if (form == null) {
            return null;
        }
        return ChildJsonFormUtils.getFormAsJson(form, formName, entityId, currentLocationId, metadata);
    }

    private FormUtils getFormUtils() {

        try {
            formUtils = new FormUtils(Utils.context().applicationContext());
        } catch (Exception e) {
            Log.e(BaseChildRegisterModel.class.getCanonicalName(), e.getMessage(), e);
        }

        return formUtils;
    }

    public void setFormUtils(FormUtils formUtils) {
        this.formUtils = formUtils;
    }


    @Override
    public String getInitials() {
        return Utils.getUserInitials();
    }
}
