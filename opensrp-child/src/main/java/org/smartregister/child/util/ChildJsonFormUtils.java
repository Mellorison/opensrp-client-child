package org.smartregister.child.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.R;
import org.smartregister.child.activity.BaseChildFormActivity;
import org.smartregister.child.contract.ChildRegisterContract;
import org.smartregister.child.domain.ChildEventClient;
import org.smartregister.child.domain.ChildMetadata;
import org.smartregister.child.domain.FormLocationTree;
import org.smartregister.child.domain.Identifiers;
import org.smartregister.child.enums.LocationHierarchy;
import org.smartregister.child.model.ChildMotherDetailModel;
import org.smartregister.child.task.SaveOutOfAreaServiceTask;
import org.smartregister.clientandeventmodel.Address;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.FormEntityConstants;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Photo;
import org.smartregister.domain.ProfileImage;
import org.smartregister.domain.Response;
import org.smartregister.domain.ResponseStatus;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.form.FormLocation;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.immunization.domain.jsonmapping.Vaccine;
import org.smartregister.immunization.domain.jsonmapping.VaccineGroup;
import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.ImageRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.sync.ClientProcessor;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.FormUtils;
import org.smartregister.util.ImageUtils;
import org.smartregister.util.JsonFormUtils;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 26/02/2019.
 */
public class ChildJsonFormUtils extends JsonFormUtils {
    public static final String METADATA = "metadata";
    public static final String ENCOUNTER_TYPE = "encounter_type";
    public static final int REQUEST_CODE_GET_JSON = 2244;
    public static final String CURRENT_OPENSRP_ID = "current_opensrp_id";
    public static final String READ_ONLY = "read_only";
    public static final String STEP2 = "step2";
    public static final String RELATIONAL_ID = "relational_id";
    public static final String CURRENT_ZEIR_ID = "current_zeir_id";
    public static final String ZEIR_ID = "ZEIR_ID";
    public static final String updateBirthRegistrationDetailsEncounter = "Update Birth Registration";
    public static final String BCG_SCAR_EVENT = "Bcg Scar";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(com.vijay.jsonwizard.utils.FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN, Locale.ENGLISH);
    public static final String GENDER = "gender";
    public static final String M_ZEIR_ID = "M_ZEIR_ID";
    public static final String F_ZEIR_ID = "F_ZEIR_ID";
    private static final String ENCOUNTER = "encounter";
    private static final String IDENTIFIERS = "identifiers";
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private static final String OPENSRP_ID = "opensrp_id";
    private static Map<String, Set<String>> eventTypeMap = new HashMap<String, Set<String>>() {
        {
            put(Constants.KEY.FATHER, ImmutableSet.of(Constants.EventType.FATHER_REGISTRATION, Constants.EventType.UPDATE_FATHER_DETAILS));
            put(Constants.KEY.MOTHER, ImmutableSet.of(Constants.EventType.NEW_WOMAN_REGISTRATION, Constants.EventType.UPDATE_MOTHER_DETAILS));
        }
    };

    /**
     * Populate metadata onto form
     *
     * @param form              JSONObject of form
     * @param formName          Name of form to be processed
     * @param id                Entity ID
     * @param currentLocationId Current location of
     * @param metadata          Map of metadata to be loaded to form
     * @return JSONObject of form populated with entityId, locationId and any metadata in map
     * @throws Exception
     */
    public static JSONObject getFormAsJson(JSONObject form, String formName, String id, String currentLocationId, Map<String, String> metadata) throws Exception {
        if (form == null) {
            return null;
        }

        String zeirId = id;
        form.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);

        if (Utils.metadata().childRegister.formName.equals(formName)) {
            if (StringUtils.isBlank(zeirId)) {
                zeirId = Utils.getNextOpenMrsId();
                if (StringUtils.isBlank(zeirId) || (ChildLibrary.getInstance().getUniqueIdRepository().countUnUsedIds() < 1L)) {
                    Timber.e("JsonFormUtils --> UniqueIds are empty or only one unused found");
                    return null;
                }
            }

            if (StringUtils.isNotBlank(zeirId)) {
                zeirId = zeirId.replace("-", "");
            }

            Map<String, String> locationMetadata = ChildJsonFormUtils.addRegistrationFormLocationHierarchyQuestions(form);
            metadata.putAll(locationMetadata);

            metadata.put(ChildJsonFormUtils.ZEIR_ID, zeirId); //inject zeir id into the form

            prePopulateJsonFormFields(form, metadata, new ArrayList<String>());
        } else if (formName.equals(Utils.metadata().childRegister.outOfCatchmentFormName)) {
            if (StringUtils.isNotBlank(zeirId)) {
                zeirId = zeirId.replace("-", "");
            } else {
                JSONArray fields = form.getJSONObject(ChildJsonFormUtils.STEP1).getJSONArray(ChildJsonFormUtils.FIELDS);
                for (int i = 0; i < fields.length(); i++) {
                    if (fields.getJSONObject(i).getString(ChildJsonFormUtils.KEY).equals(ChildJsonFormUtils.ZEIR_ID)) {
                        fields.getJSONObject(i).put(READ_ONLY, false);
                        break;
                    }
                }
            }

            JSONObject stepOne = form.getJSONObject(ChildJsonFormUtils.STEP1);
            JSONArray jsonArray = stepOne.getJSONArray(ChildJsonFormUtils.FIELDS);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(ChildJsonFormUtils.ZEIR_ID)) {
                    jsonObject.remove(ChildJsonFormUtils.VALUE);
                    jsonObject.put(ChildJsonFormUtils.VALUE, zeirId);
                }
            }

            ChildJsonFormUtils.addAvailableVaccines(ChildLibrary.getInstance().context().applicationContext(), form);
        } else {
            Timber.w("JsonFormUtils --> Unsupported form requested for launch %s", formName);
        }
        Timber.d("JsonFormUtils --> form is %s", form.toString());

        return form;
    }

    /**
     * Generate location tree for location type fields
     *
     * @param form JSON form object
     * @return Map of key-value pairs with location openmrs_entity_id as key and the location id as the value
     */
    public static Map<String, String> addRegistrationFormLocationHierarchyQuestions(JSONObject form) {
        try {
            JSONArray questions = com.vijay.jsonwizard.utils.FormUtils.getMultiStepFormFields(form);

            List<String> allLevels = getLocationLevels();
            List<String> healthFacilities = getHealthFacilityLevels();

            String defaultFacilityString = generateLocationString(healthFacilities);
            String defaultLocationString = generateLocationString(allLevels);

            return updateLocationTree(questions, defaultLocationString, defaultFacilityString, allLevels, healthFacilities);
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> addRegistrationFormLocationHierarchyQuestions");
            return null;
        }
    }

    /**
     * Generate a JSON array of user's assigned location names
     *
     * @param locationTags List of location tag names
     * @return JSON array of default location names
     */
    private static String generateLocationString(List<String> locationTags) {
        List<String> locationNames = LocationHelper.getInstance().generateDefaultLocationHierarchy(locationTags);
        return AssetHandler.javaToJsonString(locationNames, new TypeToken<List<String>>() {
        }.getType());
    }

    /**
     * Generate form location tree hierarchy
     *
     * @param locationTags  List of location levels to be displayed in the tree hierarchy
     * @param showOther     Flag on whether to display the "Other" option in the tree
     * @param selectableTag Tag name of level that should be selectable in the tree
     * @return FormLocationTree object
     */
    private static FormLocationTree generateFormLocationTree(List<String> locationTags, boolean showOther, String selectableTag) {
        ArrayList<String> allowedLevels = (ArrayList<String>) locationTags;

        if (!StringUtils.isBlank(selectableTag)) {
            int finalIndex = locationTags.indexOf(selectableTag) + 1;
            allowedLevels = finalIndex <= locationTags.size() - 1 ? new ArrayList<>(locationTags.subList(0, finalIndex)) : (ArrayList<String>) locationTags;
        }

        List<FormLocation> formLocationList = LocationHelper.getInstance().generateLocationHierarchyTree(showOther, allowedLevels);

        String locationsString = AssetHandler.javaToJsonString(formLocationList, new TypeToken<List<FormLocation>>() {
        }.getType());

        return new FormLocationTree(locationsString, formLocationList);
    }

    public static void addAvailableVaccines(Context context, JSONObject form) {
        List<VaccineGroup> supportedVaccines = VaccinatorUtils.getSupportedVaccines(context);
        if (supportedVaccines != null && !supportedVaccines.isEmpty() && form != null) {
            // For each of the vaccine groups, create a checkbox question
            try {
                JSONArray questionList = getQuestionList(context, form);

                HashMap<String, ArrayList<JSONObject>> vaccineTypeConstraints = generateVaccineTypeConstraints(supportedVaccines);

                for (VaccineGroup curVaccineGroup : supportedVaccines) {
                    JSONObject curQuestion = getCurQuestion(context, curVaccineGroup);

                    JSONArray options = new JSONArray();
                    for (Vaccine curVaccine : curVaccineGroup.vaccines) {
                        ArrayList<String> definedVaccineNames = new ArrayList<>();
                        if (curVaccine.vaccine_separator != null) {
                            String rawNames = curVaccine.name;
                            String separator = curVaccine.vaccine_separator;
                            String[] split = rawNames.split(separator);
                            definedVaccineNames.addAll(Arrays.asList(split));
                        } else {
                            definedVaccineNames.add(curVaccine.name);
                        }

                        for (String curVaccineName : definedVaccineNames) {
                            JSONObject curVaccines = new JSONObject();
                            curVaccines.put(JsonFormConstants.KEY, curVaccineName);
                            curVaccines.put("text", VaccinatorUtils.getTranslatedVaccineName(context, curVaccineName));
                            curVaccines.put("value", "false");
                            JSONArray constraints = new JSONArray();

                            // Add the constraints
                            if (vaccineTypeConstraints.containsKey(curVaccine.type)) {
                                for (JSONObject curConstraint : vaccineTypeConstraints.get(curVaccine.type)) {
                                    if (!curConstraint.getString("vaccine").equals(curVaccineName)) {
                                        JSONObject constraintClone = new JSONObject(curConstraint.toString());
                                        constraintClone.remove("vaccine");
                                        constraints.put(constraintClone);
                                    }
                                }
                            }

                            if (constraints.length() > 0) {
                                curVaccines.put("constraints", constraints);
                            }

                            options.put(curVaccines);
                        }
                    }

                    curQuestion.put("options", options);
                    questionList.put(curQuestion);
                }
            } catch (JSONException e) {
                Timber.e(e, "JsonFormUtils --> addAvailableVaccines");
            }
        }
    }

    @NotNull
    private static HashMap<String, ArrayList<JSONObject>> generateVaccineTypeConstraints(List<VaccineGroup> supportedVaccines) throws JSONException {
        HashMap<String, ArrayList<JSONObject>> vaccineTypeConstraints = new HashMap<>();
        for (VaccineGroup curVaccineGroup : supportedVaccines) {
            for (Vaccine curVaccine : curVaccineGroup.vaccines) {
                if (!vaccineTypeConstraints.containsKey(curVaccine.type)) {
                    vaccineTypeConstraints.put(curVaccine.type, new ArrayList<JSONObject>());
                }
                ArrayList<String> vaccineNamesDefined = new ArrayList<>();
                if (curVaccine.vaccine_separator != null) {
                    String unSplitNames = curVaccine.name;
                    String separator = curVaccine.vaccine_separator;
                    String[] splitValues = unSplitNames.split(separator);
                    vaccineNamesDefined.addAll(Arrays.asList(splitValues));
                } else {
                    vaccineNamesDefined.add(curVaccine.name);
                }

                for (String curVaccineName : vaccineNamesDefined) {
                    JSONObject curConstraint = getCurConstraint(curVaccineGroup, curVaccine, curVaccineName);
                    vaccineTypeConstraints.get(curVaccine.type).add(curConstraint);
                }
            }
        }
        return vaccineTypeConstraints;
    }

    @NotNull
    private static JSONArray getQuestionList(Context context, JSONObject form) throws JSONException {
        JSONArray questionList = form.getJSONObject("step1").getJSONArray("fields");
        JSONObject vaccinationLabel = new JSONObject();
        vaccinationLabel.put(JsonFormConstants.KEY, "Vaccines_Provided_Label");
        vaccinationLabel.put("type", "label");
        vaccinationLabel.put("text", context.getString(R.string.which_vaccinations_were_provided));
        vaccinationLabel.put("openmrs_entity_parent", "-");
        vaccinationLabel.put("openmrs_entity", "-");
        vaccinationLabel.put("openmrs_entity_id", "-");
        questionList.put(vaccinationLabel);
        return questionList;
    }

    @NotNull
    private static JSONObject getCurConstraint(VaccineGroup curVaccineGroup, Vaccine curVaccine, String curVaccineName) throws JSONException {
        JSONObject curConstraint = new JSONObject();
        curConstraint.put("vaccine", curVaccineName);
        curConstraint.put("type", "array");
        curConstraint.put("ex",
                "notEqualTo(step1:" + curVaccineGroup.id + ", \"[\"" + curVaccineName + "\"]\")");
        curConstraint.put("err", "Cannot be given with the other " + curVaccine.type + " dose");
        return curConstraint;
    }

    private static JSONObject getCurQuestion(Context context, VaccineGroup curVaccineGroup) throws JSONException {
        JSONObject curQuestion = new JSONObject();
        curQuestion.put(JsonFormConstants.KEY, curVaccineGroup.id);
        curQuestion.put("type", "check_box");
        curQuestion.put("is_vaccine_group", true);
        curQuestion.put("label", VaccinatorUtils.translate(context, curVaccineGroup.name));
        curQuestion.put("openmrs_entity_parent", "-");
        curQuestion.put("openmrs_entity", "-");
        curQuestion.put("openmrs_entity_id", "-");
        return curQuestion;
    }

    /**
     * Fetch configured locations levels
     *
     * @return List of location level names
     */
    @NotNull
    private static List<String> getLocationLevels() {
        return Utils.metadata().getLocationLevels();
    }

    /**
     * Fetch configured list of health facility levels
     *
     * @return List of health facility level names
     */
    @NotNull
    private static List<String> getHealthFacilityLevels() {
        return Utils.metadata().getHealthFacilityLevels();
    }

    /**
     * Fetch list of configured allowed levels
     *
     * @return List of allowed level names
     */
    @NotNull
    private static List<String> getAllowedLevels() {
        return LocationHelper.getInstance().getAllowedLevels();
    }

    private static Map<String, String> updateLocationTree(JSONArray questions, String defaultLocationString, String defaultFacilityString, List<String> allLevels, List<String> healthFacilities) throws JSONException {
        Map<String, String> locationMetadata = new HashMap<>();

        ChildMetadata childMetadata = Utils.metadata();
        LocationHierarchy locationHierarchy;//Default
        if (childMetadata.getFieldsWithLocationHierarchy() != null && !childMetadata.getFieldsWithLocationHierarchy().isEmpty()) {

            FormLocationTree upToFacilities = generateFormLocationTree(healthFacilities, false, null);
            FormLocationTree upToFacilitiesWithOther = generateFormLocationTree(healthFacilities, true, null);
            FormLocationTree entireTree = generateFormLocationTree(allLevels, true, null);

            List<FormLocation> formLocations = LocationHelper.getInstance().generateLocationHierarchyTree(false, getAllowedLevels());

            for (int i = 0; i < questions.length(); i++) {

                JSONObject widget = questions.getJSONObject(i);

                if (widget.has(JsonFormConstants.TYPE) && widget.getString(JsonFormConstants.TYPE).equals(JsonFormConstants.TREE)) {

                    String key = widget.optString(JsonFormConstants.KEY);
                    String hierarchyType = widget.optString(Constants.JSON_FORM_KEY.HIERARCHY);

                    locationHierarchy = !StringUtils.isBlank(hierarchyType) ? LocationHierarchy.valueOf(hierarchyType.toUpperCase(Locale.ENGLISH)) : LocationHierarchy.ENTIRE_TREE;

                    if (StringUtils.isNotBlank(key) && childMetadata.getFieldsWithLocationHierarchy().contains(widget.optString(JsonFormConstants.KEY))) {
                        switch (locationHierarchy) {
                            case FACILITY_ONLY:
                                if (StringUtils.isNotBlank(upToFacilities.getFormLocationString())) {
                                    addLocationTree(key, widget, upToFacilities.getFormLocationString(), JsonFormConstants.TREE);
                                }
                                if (StringUtils.isNotBlank(defaultFacilityString)) {
                                    addLocationTreeDefault(key, widget, defaultFacilityString);
                                }
                                break;
                            case FACILITY_WITH_OTHER:
                                if (StringUtils.isNotBlank(upToFacilitiesWithOther.getFormLocationString())) {
                                    addLocationTree(key, widget, upToFacilitiesWithOther.getFormLocationString(), JsonFormConstants.TREE);
                                }
                                if (StringUtils.isNotBlank(defaultFacilityString)) {
                                    addLocationTreeDefault(key, widget, defaultFacilityString);
                                }
                                break;
                            case ENTIRE_TREE:
                                String selectableTag = widget.optString(Constants.JSON_FORM_KEY.SELECTABLE);
                                if (StringUtils.isNotBlank(selectableTag)) {
                                    entireTree = generateFormLocationTree(allLevels, true, selectableTag);
                                }
                                if (StringUtils.isNotBlank(entireTree.getFormLocationString())) {
                                    addLocationTree(key, widget, entireTree.getFormLocationString(), JsonFormConstants.TREE);
                                }
                                if (StringUtils.isNotBlank(defaultLocationString)) {
                                    addLocationTreeDefault(key, widget, defaultLocationString);
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    generateLocationMetadata(locationMetadata, formLocations, widget);
                }
            }
        }

        return locationMetadata;
    }

    private static void generateLocationMetadata(Map<String, String> locationMetadata, List<FormLocation> formLocations, JSONObject widget) throws JSONException {
        String selectableTag = widget.optString(Constants.JSON_FORM_KEY.SELECTABLE);
        if (StringUtils.isNotBlank(selectableTag)) {
            String locationKey = getSelectableKey(formLocations, selectableTag);

            String prefix = getJsonFieldEntityId(widget, Constants.ENTITY.MOTHER);
            String addressKey = widget.optString(JsonFormConstants.OPENMRS_ENTITY_ID);
            locationMetadata.put(prefix + addressKey, LocationHelper.getInstance().getOpenMrsLocationId(locationKey));
        }
    }

    @NotNull
    private static String getJsonFieldEntityId(JSONObject jsonObject, String entity) throws JSONException {
        return jsonObject.has(ChildJsonFormUtils.ENTITY_ID) && jsonObject.getString(ChildJsonFormUtils.ENTITY_ID).equalsIgnoreCase(Constants.KEY.MOTHER) ? (entity + "_") : "";
    }

    /**
     * Returns the form location key from a location hierarchy given a specific location tag
     *
     * @param formLocations Location tree to be searched
     * @param selectableTag Location tag to filter the location node
     * @return Location key
     */
    private static String getSelectableKey(List<FormLocation> formLocations, String selectableTag) {
        if (formLocations != null && !formLocations.isEmpty()) {
            for (FormLocation location : formLocations) {
                if (location.level.equalsIgnoreCase(selectableTag)) {
                    return location.key;
                } else {
                    return getSelectableKey(location.nodes, selectableTag);
                }
            }
        }

        return null;
    }

    private static void addLocationTree(@NonNull String widgetKey, @NonNull JSONObject widget,
                                        @NonNull String updateString, @NonNull String treeType) {
        try {
            if (widgetKey.equals(widget.optString(JsonFormConstants.KEY))) {
                widget.put(treeType, new JSONArray(updateString));
            }
        } catch (JSONException e) {
            Timber.e(e, "JsonFormUtils --> addLocationTree");
        }
    }

    private static void addLocationTreeDefault(@NonNull String widgetKey, @NonNull JSONObject widget,
                                               @NonNull String updateString) {
        addLocationTree(widgetKey, widget, updateString, JsonFormConstants.DEFAULT);
    }

    public static void saveReportDeceased(Context context, String jsonString,
                                          String locationId, String entityId) {
        try {

            String providerId = ChildLibrary.getInstance().context().allSharedPreferences().fetchRegisteredANM();
            EventClientRepository db = ChildLibrary.getInstance().eventClientRepository();
            JSONObject jsonForm = new JSONObject(jsonString);

            JSONArray fields = fields(jsonForm);
            if (fields == null) {
                return;
            }

            String encounterDateField = getFieldValue(fields, "Date_of_Death");

            String encounterType = getString(jsonForm, ENCOUNTER_TYPE);
            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            Date encounterDate = new Date();
            String encounterDateTimeString = null;
            if (StringUtils.isNotBlank(encounterDateField)) {
                encounterDateTimeString = formatDate(encounterDateField);
                Date dateTime = formatDate(encounterDateField, false);
                if (dateTime != null) {
                    encounterDate = dateTime;
                }
            }

            Event event = getEvent(providerId, locationId, entityId, encounterType, encounterDate, Constants.KEY.CHILD);
            addSaveReportDeceasedObservations(fields, event);
            updateMetadata(metadata, event);


            if (event != null) {
                createDeathEventObject(context, providerId, locationId, entityId, db, encounterDate, encounterDateTimeString, event);


                ContentValues values = new ContentValues();
                values.put(Constants.KEY.DOD, encounterDateField);
                values.put(Constants.KEY.DATE_REMOVED, Utils.getTodaysDate());
                updateChildFTSTables(values, entityId);

                updateDateOfRemoval(entityId, encounterDateTimeString);//TO DO Refactor  with better

                //     Utils.postEvent(new ClientDirtyFlagEvent(entityId, encounterType));
            }

            processClients(Utils.getAllSharedPreferences(), ChildLibrary.getInstance().getEcSyncHelper());

        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> saveReportDeceased");
        }
    }

    private static void addSaveReportDeceasedObservations(JSONArray fields, Event event) {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject jsonObject = getJSONObject(fields, i);
            String value = getString(jsonObject, VALUE);
            if (StringUtils.isNotBlank(value)) {
                addObservation(event, jsonObject);
            }
        }
    }

    private static void updateMetadata(JSONObject metadata, Event event) {
        if (metadata != null) {
            Iterator<?> keys = metadata.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                JSONObject jsonObject = getJSONObject(metadata, key);
                String value = getString(jsonObject, VALUE);
                if (StringUtils.isNotBlank(value)) {
                    String entityVal = getString(jsonObject, OPENMRS_ENTITY);
                    if (entityVal != null) {
                        if (entityVal.equals(CONCEPT)) {
                            addToJSONObject(jsonObject, KEY, key);
                            addObservation(event, jsonObject);
                        } else if (entityVal.equals(ENCOUNTER)) {
                            String entityIdVal = getString(jsonObject, OPENMRS_ENTITY_ID);
                            if (entityIdVal.equals(FormEntityConstants.Encounter.encounter_date.name())) {
                                Date eDate = formatDate(value, false);
                                if (eDate != null) {
                                    event.setEventDate(eDate);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void processClients(AllSharedPreferences allSharedPreferences, ECSyncHelper ecSyncHelper) throws Exception {
        long lastSyncTimeStamp = allSharedPreferences.fetchLastUpdatedAtDate(0);
        Date lastSyncDate = new Date(lastSyncTimeStamp);

        List<EventClient> eventList = new ArrayList<>();
        eventList.addAll(ecSyncHelper.getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
        eventList.addAll(ecSyncHelper.getEvents(lastSyncDate, BaseRepository.TYPE_Unsynced));

        ChildLibrary.getInstance().getClientProcessorForJava().processClient(eventList);
        allSharedPreferences.saveLastUpdatedAtDate(lastSyncDate.getTime());
    }

    private static Event getEvent(String providerId, String locationId, String entityId,
                                  String encounterType, Date encounterDate, String childType) {

        Event event = (Event) new Event().withBaseEntityId(entityId) //should be different for main and subform
                .withEventDate(encounterDate).withEventType(encounterType).withLocationId(locationId)
                .withProviderId(providerId).withEntityType(childType)
                .withFormSubmissionId(generateRandomUUIDString()).withDateCreated(new Date());

        ChildJsonFormUtils.tagSyncMetadata(event);

        return event;
    }

    private static void createDeathEventObject(Context context, String providerId, String locationId,
                                               String entityId, EventClientRepository db, Date encounterDate,
                                               String encounterDateTimeString, Event event) throws JSONException {

        JSONObject eventJson = new JSONObject(ChildJsonFormUtils.gson.toJson(event));

        //After saving, Unsync(remove) this event's details
        //List<JSONObject> jsonEvents = new ArrayList<>();
        ///jsonEvents.add(eventJson);

        //Update client to deceased
        JSONObject client = db.getClientByBaseEntityId(eventJson.getString(ClientProcessor.baseEntityIdJSONKey));
        client.put(FormEntityConstants.Person.deathdate.name(), encounterDateTimeString);
        client.put(FormEntityConstants.Person.deathdate_estimated.name(), false);
        client.put(Constants.JSON_FORM_KEY.DEATH_DATE_APPROX, false);

        db.addorUpdateClient(entityId, client);

        //Add Death Event for child to flag for Server delete
        db.addEvent(event.getBaseEntityId(), eventJson);

        //Update Child Entity to include death date
        Event updateChildDetailsEvent = getEvent(providerId, locationId, entityId, ChildJsonFormUtils.updateBirthRegistrationDetailsEncounter, encounterDate, Constants.CHILD_TYPE);

        addMetaData(context, updateChildDetailsEvent, new Date());

        JSONObject eventJsonUpdateChildEvent = new JSONObject(ChildJsonFormUtils.gson.toJson(updateChildDetailsEvent));

        db.addEvent(entityId, eventJsonUpdateChildEvent); //Add event to flag server update
    }

    public static void updateChildFTSTables(ContentValues values, String entityId) {
        //Update REGISTER and FTS Tables
        String tableName = Utils.metadata().getRegisterQueryProvider().getDemographicTable();
        AllCommonsRepository allCommonsRepository = ChildLibrary.getInstance().context().allCommonsRepositoryobjects(tableName);
        if (allCommonsRepository != null) {
            allCommonsRepository.update(tableName, values, entityId);
            updateChildFTSTablesSearchOnly(tableName, Arrays.asList(new String[]{entityId}));
        }
    }

    /**
     * Update All FTS for each client
     *
     * @param tableName
     * @param entityIds
     */
    public static void updateChildFTSTablesSearchOnly(String tableName, List<String> entityIds) {
        ChildLibrary.getInstance().context().allCommonsRepositoryobjects(tableName).updateSearch(entityIds);
    }

    @SuppressLint("MissingPermission")
    public static Event addMetaData(Context context, Event event, Date start) {
        Map<String, String> metaFields = new HashMap<>();
        metaFields.put("deviceid", "163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        metaFields.put("end", "163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        metaFields.put("start", "163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        Calendar calendar = Calendar.getInstance();

        String end = DATE_TIME_FORMAT.format(calendar.getTime());

        Obs obs = new Obs();
        obs.setFieldCode("163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(DATE_TIME_FORMAT.format(start));
        obs.setFieldType("concept");
        obs.setFieldDataType("start");
        event.addObs(obs);

        obs = new Obs();
        obs.setFieldCode("163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(end);
        obs.setFieldDataType("end");
        event.addObs(obs);

        String deviceId = "";
        try {

            TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = mTelephonyManager.getSimSerialNumber(); //Already handled by native form

        } catch (SecurityException e) {
            Timber.e(e, "JsonFormUtils --> MissingPermission --> getSimSerialNumber");
        } catch (NullPointerException e) {
            Timber.e(e);
        }
        obs = new Obs();
        obs.setFieldCode("163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        obs.setValue(deviceId);
        obs.setFieldDataType("deviceid");
        event.addObs(obs);

        return event;
    }

    protected static Event tagSyncMetadata(@NonNull Event event) {
        AllSharedPreferences allSharedPreferences = Utils.getAllSharedPreferences();
        String providerId = allSharedPreferences.fetchRegisteredANM();
        event.setProviderId(providerId);
        event.setLocationId(locationId(allSharedPreferences));

        String childLocationId = getChildLocationId(event.getLocationId(), allSharedPreferences);
        event.setChildLocationId(childLocationId);

        if (StringUtils.isNotBlank(childLocationId) && childLocationId.startsWith(AllConstants.ADVANCED_DATA_CAPTURE_STRATEGY_PREFIX)) {
            event.addDetails(AllConstants.DATA_STRATEGY, childLocationId.substring(childLocationId.indexOf('_') + 1));
        }

        event.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        event.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));

        event.setClientDatabaseVersion(ChildLibrary.getInstance().getDatabaseVersion());
        event.setClientApplicationVersion(ChildLibrary.getInstance().getApplicationVersion());
        return event;
    }

    @Nullable
    public static String getChildLocationId(@NonNull String defaultLocationId, @NonNull AllSharedPreferences allSharedPreferences) {
        String currentLocality = allSharedPreferences.fetchCurrentLocality();

        try {
            if (StringUtils.isNotBlank(currentLocality)) {
                String currentLocalityId = LocationHelper.getInstance().getOpenMrsLocationId(currentLocality);
                if (StringUtils.isNotBlank(currentLocalityId) && !defaultLocationId.equals(currentLocalityId)) {
                    return currentLocalityId;
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return null;
    }

    public static void updateDateOfRemoval(String baseEntityId, String dateOfRemovalString) {
        ContentValues contentValues = new ContentValues();

        if (dateOfRemovalString != null) {
            contentValues.put(Constants.KEY.DATE_REMOVED, dateOfRemovalString);
        }

        ChildLibrary.getInstance().eventClientRepository().getWritableDatabase()
                .update(Utils.metadata().getRegisterQueryProvider().getDemographicTable(), contentValues, Constants.KEY.BASE_ENTITY_ID + " = ?",
                        new String[]{baseEntityId});
    }

    public static String locationId(AllSharedPreferences allSharedPreferences) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        String userLocationId = allSharedPreferences.fetchUserLocalityId(providerId);
        if (StringUtils.isBlank(userLocationId)) {
            userLocationId = allSharedPreferences.fetchDefaultLocalityId(providerId);
        }
        return userLocationId;
    }

    public static ChildEventClient processChildDetailsForm(String jsonString, FormTag formTag) {
        try {
            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);

            if (!registrationFormParams.getLeft()) {
                return null;
            }

            JSONObject jsonForm = registrationFormParams.getMiddle();
            JSONArray fields = registrationFormParams.getRight();

            String entityId = getString(jsonForm, ENTITY_ID);
            if (StringUtils.isBlank(entityId)) {
                entityId = generateRandomUUIDString();
            }

            processGender(fields);//multi language to re visit

            processLocationFields(fields);

            lastInteractedWith(fields);

            dobUnknownUpdateFromAge(fields, Constants.CHILD_TYPE);

            JSONObject dobUnknownObject = getFieldJSONObject(fields, Constants.JSON_FORM_KEY.DATE_BIRTH);

            String date = dobUnknownObject.getString(Constants.KEY.VALUE);
            dobUnknownObject.put(Constants.KEY.VALUE, Utils.reverseHyphenatedString(date) + " 12:00:00");

            Client baseClient = ChildJsonFormUtils.createBaseClient(fields, formTag, entityId);
            baseClient.setRelationalBaseEntityId(getString(jsonForm, Constants.KEY.RELATIONAL_ID));//mama

            Event baseEvent = ChildJsonFormUtils.createEvent(fields, getJSONObject(jsonForm, METADATA),
                    formTag, entityId, jsonForm.getString(ChildJsonFormUtils.ENCOUNTER_TYPE), Constants.CHILD_TYPE);

            for (int i = baseEvent.getObs().size() - 1; i > -1; i--) {
                Obs obs = baseEvent.getObs().get(i);

                if (obs != null && "mother_hiv_status".equals(obs.getFormSubmissionField())) {
                    List<Object> values = obs.getValues();

                    if (values != null && values.size() == 1 && values.get(0) == null) {
                        baseEvent.getObs().remove(obs);
                    }
                }
            }

            ChildJsonFormUtils.tagSyncMetadata(baseEvent);// tag docs

            //Add previous relational ids if they existed.
            addRelationships(baseClient, jsonString);

            return new ChildEventClient(baseClient, baseEvent);
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> processChildDetailsForm");
            return null;
        }
    }

    protected static Triple<Boolean, JSONObject, JSONArray> validateParameters(String jsonString) {
        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = fields(jsonForm);
        return Triple.of(jsonForm != null && fields != null, jsonForm, fields);
    }

    protected static void processGender(JSONArray fields) {
        try {
            //TO DO Will need re-architecting later to support more languages, perhaps update the selector widget

            JSONObject genderObject = getFieldJSONObject(fields, Constants.SEX);
            String genderValue = "";

            String rawGender = genderObject.getString(JsonFormConstants.VALUE);
            char rawGenderChar = !TextUtils.isEmpty(rawGender) ? rawGender.charAt(0) : ' ';
            switch (rawGenderChar) {
                case 'm':
                case 'M':
                    genderValue = "Male";
                    break;

                case 'f':
                case 'F':
                    genderValue = "Female";
                    break;

                default:
                    break;

            }

            genderObject.put(Constants.KEY.VALUE, genderValue);
        } catch (JSONException e) {
            Timber.e(e, "JsonFormUtils --> processGender");
        }
    }

    /**
     * Update value tag for fields of type tree with the locationId
     *
     * @param fields JSONArray of form fields
     * @throws JSONException
     */
    protected static void processLocationFields(JSONArray fields) throws JSONException {
        for (int i = 0; i < fields.length(); i++) {
            if (fields.getJSONObject(i).has(JsonFormConstants.TYPE) && fields.getJSONObject(i).getString(JsonFormConstants.TYPE).equals(JsonFormConstants.TREE)) {
                try {
                    String rawValue = fields.getJSONObject(i).getString(JsonFormConstants.VALUE);
                    JSONArray valueArray = new JSONArray(rawValue);
                    if (valueArray.length() > 0) {
                        String lastLocationName = valueArray.getString(valueArray.length() - 1);
                        String lastLocationId = LocationHelper.getInstance().getOpenMrsLocationId(lastLocationName);
                        fields.getJSONObject(i).put(JsonFormConstants.VALUE, lastLocationId);
                    }
                } catch (Exception e) {
                    Timber.e(e, "JsonFormUitls --> processLocationFields");
                }
            }
        }
    }

    protected static void lastInteractedWith(JSONArray fields) {
        try {
            JSONObject lastInteractedWith = new JSONObject();
            lastInteractedWith.put(Constants.KEY.KEY, Constants.JSON_FORM_KEY.LAST_INTERACTED_WITH);
            lastInteractedWith.put(Constants.KEY.VALUE, Calendar.getInstance().getTimeInMillis());
            fields.put(lastInteractedWith);
        } catch (JSONException e) {
            Timber.e(e, "JsonFormUtils --> lastInteractedWith");
        }
    }

    protected static void dobUnknownUpdateFromAge(JSONArray fields, String entity) {
        try {

            String dobUnknownField = entity.equalsIgnoreCase(Constants.KEY.MOTHER) ? Constants.JSON_FORM_KEY.MOTHER_GUARDIAN_DATE_BIRTH_UNKNOWN : Constants.JSON_FORM_KEY.DATE_BIRTH_UNKNOWN;
            String dobField = entity.equalsIgnoreCase(Constants.KEY.MOTHER) ? Constants.JSON_FORM_KEY.MOTHER_GUARDIAN_DATE_BIRTH : Constants.JSON_FORM_KEY.DATE_BIRTH;
            String dobAgeField = entity.equalsIgnoreCase(Constants.KEY.MOTHER) ? Constants.JSON_FORM_KEY.MOTHER_GUARDIAN_AGE : Constants.JSON_FORM_KEY.AGE;

            JSONObject dobUnknownObject = getFieldJSONObject(fields, dobUnknownField);
            if (dobUnknownObject != null) {

                JSONArray options = getJSONArray(dobUnknownObject, Constants.JSON_FORM_KEY.OPTIONS);
                boolean isDobUnknown = Boolean.valueOf(ChildJsonFormUtils.getFieldValue(options, dobUnknownField));

                if (isDobUnknown) {

                    String ageString = getFieldValue(fields, dobAgeField);
                    if (StringUtils.isNotBlank(ageString) && StringUtils.isNumeric(ageString)) {
                        int age = Integer.valueOf(ageString);
                        JSONObject dobJSONObject = getFieldJSONObject(fields, dobField);
                        dobJSONObject.put(VALUE, Utils.getDob(age));

                        //Mark the birth date as an approximation
                        JSONObject isBirthdateApproximate = new JSONObject();
                        isBirthdateApproximate.put(Constants.KEY.KEY, FormEntityConstants.Person.birthdate_estimated);
                        isBirthdateApproximate.put(Constants.KEY.VALUE, Constants.BOOLEAN_INT.TRUE);
                        isBirthdateApproximate.put(Constants.OPENMRS.ENTITY, Constants.OPENMRS_ENTITY.PERSON);//Required for value to be processed
                        isBirthdateApproximate.put(Constants.OPENMRS.ENTITY_ID, FormEntityConstants.Person.birthdate_estimated);
                        isBirthdateApproximate.put(ChildJsonFormUtils.ENTITY_ID, dobUnknownObject.getString(ChildJsonFormUtils.ENTITY_ID));
                        fields.put(isBirthdateApproximate);

                    }
                } else {
                    //Else to override dob unknown flag incase it was already previously saved on the db

                    JSONObject dobUnknownValue = new JSONObject();
                    dobUnknownValue.put(JsonFormConstants.KEY, dobUnknownField);
                    dobUnknownValue.put(JsonFormConstants.VALUE, "false");

                    JSONArray dobUnknownValueArray = new JSONArray();
                    dobUnknownValueArray.put(dobUnknownValue);

                    dobUnknownObject.put(JsonFormConstants.VALUE, dobUnknownValueArray);
                }
            }
        } catch (JSONException e) {
            Timber.e(e, "JsonFormUtils --> dobUnknownUpdateFromAge");
        }
    }

    public static void mergeAndSaveClient(Client baseClient) throws Exception {
        JSONObject updatedClientJson = new JSONObject(ChildJsonFormUtils.gson.toJson(baseClient));
        JSONObject originalClientJsonObject = ChildLibrary.getInstance().getEcSyncHelper().getClient(baseClient.getBaseEntityId());
        JSONObject mergedJson = ChildJsonFormUtils.merge(originalClientJsonObject, updatedClientJson);
        //TODO Save edit log ?
        ChildLibrary.getInstance().getEcSyncHelper().addClient(baseClient.getBaseEntityId(), mergedJson);
    }

    public static void saveImage(String providerId, String entityId, String imageLocation) {
        try {
            if (StringUtils.isBlank(imageLocation)) {
                return;
            }

            File file = new File(imageLocation);
            if (!file.exists()) {
                return;
            }

            Bitmap compressedImageFile = ChildLibrary.getInstance().getCompressor().compressToBitmap(file);
            saveStaticImageToDisk(compressedImageFile, providerId, entityId);
        } catch (IOException e) {
            Timber.e(e, JsonFormConstants.class.getCanonicalName());
        }
    }

    private static void saveStaticImageToDisk(Bitmap image, String providerId, String entityId) {
        if (image == null || StringUtils.isBlank(providerId) || StringUtils.isBlank(entityId)) {
            return;
        }
        OutputStream os = null;

        try {
            if (StringUtils.isNotBlank(entityId)) {
                final String absoluteFileName = DrishtiApplication.getAppDir() + File.separator + entityId + ".JPEG";

                File outputFile = new File(absoluteFileName);
                os = new FileOutputStream(outputFile);
                Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                image.compress(compressFormat, 100, os);
                // insert into the db
                ProfileImage profileImage = new ProfileImage();
                profileImage.setImageid(UUID.randomUUID().toString());
                profileImage.setAnmId(providerId);
                profileImage.setEntityID(entityId);
                profileImage.setFilepath(absoluteFileName);
                profileImage.setFilecategory("profilepic");
                profileImage.setSyncStatus(ImageRepository.TYPE_Unsynced);
                ImageRepository imageRepo = Utils.context().imageRepository();
                imageRepo.add(profileImage);
            }
        } catch (FileNotFoundException e) {
            Timber.e(e, "JsonFormUtils --> Failed to save static image to disk");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Timber.e(e, "JsonFormUtils --> Failed to close static images output stream after attempting to write image");
                }
            }
        }
    }

    public static String getMetadataForEditForm(Context context, Map<String, String> childDetails) {
        return getMetadataForEditForm(context, childDetails, new ArrayList<String>());
    }

    public static String getMetadataForEditForm(Context context, Map<String, String> childDetails, List<String> nonEditableFields) {
        try {
            JSONObject form = new FormUtils(context).getFormJson(Utils.metadata().childRegister.formName);

            if (form != null) {
                ChildJsonFormUtils.addRegistrationFormLocationHierarchyQuestions(form);
                Timber.d("Form is %s", form.toString());

                form.put(ChildJsonFormUtils.ENTITY_ID, childDetails.get(Constants.KEY.BASE_ENTITY_ID));
                form.put(ChildJsonFormUtils.ENCOUNTER_TYPE, Utils.metadata().childRegister.updateEventType);
                form.put(ChildJsonFormUtils.RELATIONAL_ID, childDetails.get(RELATIONAL_ID));
                form.put(ChildJsonFormUtils.CURRENT_ZEIR_ID,
                        Utils.getValue(childDetails, Constants.KEY.ZEIR_ID, true).replace("-", ""));
                form.put(ChildJsonFormUtils.CURRENT_OPENSRP_ID,
                        Utils.getValue(childDetails, Constants.JSON_FORM_KEY.UNIQUE_ID, false));

                JSONObject metadata = form.getJSONObject(ChildJsonFormUtils.METADATA);

                metadata.put(ChildJsonFormUtils.ENCOUNTER_LOCATION, ChildLibrary.getInstance().getLocationPickerView(context).getSelectedItem());

                prePopulateJsonFormFields(form, childDetails, nonEditableFields);

                return form.toString();
            }
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> getMetadataForEditForm");
        }

        return "";
    }

    /**
     * Populate JSON form object fields with values passed in the childDetails map
     *
     * @param form              JSON form
     * @param childDetails      Map of values to be pre-populated
     * @param nonEditableFields List of fields that should not be editable
     * @throws JSONException
     */
    private static void prePopulateJsonFormFields(JSONObject form, Map<String, String> childDetails, List<String> nonEditableFields) throws JSONException {
        JSONObject stepOne = form.getJSONObject(ChildJsonFormUtils.STEP1);
        JSONArray jsonArray = stepOne.getJSONArray(ChildJsonFormUtils.FIELDS);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            setFormFieldValues(childDetails, nonEditableFields, jsonObject);
        }
    }

    private static void setFormFieldValues(Map<String, String> childDetails, List<String> nonEditableFields, JSONObject jsonObject)
            throws JSONException {

        String prefix = "";

        if (jsonObject.has(JsonFormUtils.ENTITY_ID)) {
            String entityId = jsonObject.getString(JsonFormUtils.ENTITY_ID);
            if (entityId.equalsIgnoreCase(Constants.KEY.MOTHER)) {
                prefix = "mother_";
            } else if (entityId.equalsIgnoreCase(Constants.KEY.FATHER)) {
                prefix = "father_";
            }
        }

        String dobUnknownField = prefix.startsWith(Constants.KEY.MOTHER) ? Constants.JSON_FORM_KEY.MOTHER_GUARDIAN_DATE_BIRTH_UNKNOWN : Constants.JSON_FORM_KEY.DATE_BIRTH_UNKNOWN;
        String dobAgeField = prefix.startsWith(Constants.KEY.MOTHER) ? Constants.JSON_FORM_KEY.MOTHER_GUARDIAN_AGE : Constants.JSON_FORM_KEY.AGE;

        setFormFieldInitDataCleanUp(childDetails, prefix);

        if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(Constants.KEY.PHOTO)) {
            processPhoto(childDetails.get(Constants.KEY.BASE_ENTITY_ID), jsonObject);
        } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(dobUnknownField)) {
            JSONObject optionsObject = jsonObject.getJSONArray(Constants.JSON_FORM_KEY.OPTIONS).getJSONObject(0);
            optionsObject.put(ChildJsonFormUtils.VALUE, Utils.getValue(childDetails, prefix + Constants.KEY.DOB_UNKNOWN, false));
        } else if (jsonObject.getString(ChildJsonFormUtils.KEY).equalsIgnoreCase(dobAgeField)) {
            processAge(Utils.getValue(childDetails, prefix + Constants.KEY.DOB, false), jsonObject);
        } else if (jsonObject.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.DATE_PICKER)) {
            processDate(childDetails, prefix, jsonObject);
        } else if (jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY).equalsIgnoreCase(ChildJsonFormUtils.PERSON_INDENTIFIER)) {
            jsonObject.put(ChildJsonFormUtils.VALUE, getMappedValue(jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY_ID), childDetails).replace("-", ""));
        } else if (jsonObject.has(JsonFormConstants.TREE)) {
            processTree(jsonObject, Utils.getValue(childDetails, jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY).equalsIgnoreCase(ChildJsonFormUtils.PERSON_ADDRESS) ? prefix + jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY_ID) : jsonObject.getString(ChildJsonFormUtils.KEY), false));
        } else if (jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY).equalsIgnoreCase(ChildJsonFormUtils.CONCEPT)) {
            jsonObject.put(ChildJsonFormUtils.VALUE, getMappedValue(jsonObject.getString(ChildJsonFormUtils.KEY), childDetails));
        } else if (jsonObject.has(Constants.JSON_FORM_KEY.SUB_TYPE) && jsonObject.getString(Constants.JSON_FORM_KEY.SUB_TYPE).equalsIgnoreCase(Constants.JSON_FORM_KEY.LOCATION_SUB_TYPE)) {
            setSubTypeFieldValue(childDetails, jsonObject);
        } else if (jsonObject.has(JsonFormConstants.OPTIONS_FIELD_NAME)) {
            setOptionFieldValue(childDetails, jsonObject, prefix);
        } else {
            jsonObject.put(ChildJsonFormUtils.VALUE, getMappedValue(prefix + jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY_ID), childDetails));
        }

        jsonObject.put(ChildJsonFormUtils.READ_ONLY, nonEditableFields.contains(jsonObject.getString(ChildJsonFormUtils.KEY)));
    }

    private static void setSubTypeFieldValue(Map<String, String> childDetails, JSONObject jsonObject) throws JSONException {
        if (!jsonObject.has(Constants.JSON_FORM_KEY.VALUE_FIELD) || jsonObject.getString(Constants.JSON_FORM_KEY.VALUE_FIELD).equalsIgnoreCase(jsonObject.getString(ChildJsonFormUtils.KEY))) {
            jsonObject.put(JsonFormConstants.VALUE, getMappedValue(jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY_ID), childDetails));
        } else {
            jsonObject.put(JsonFormConstants.VALUE, getMappedValue(jsonObject.getString(Constants.JSON_FORM_KEY.VALUE_FIELD), childDetails));
        }
    }

    private static void setOptionFieldValue(Map<String, String> childDetails, JSONObject jsonObject, String prefix) throws JSONException {
        String val = getMappedValue(prefix + jsonObject.getString(ChildJsonFormUtils.KEY), childDetails);
        String key = prefix + jsonObject.getString(ChildJsonFormUtils.KEY);

        if (!TextUtils.isEmpty(val)) {
            JSONArray array = new JSONArray(val.charAt(0) == '[' ? val : "[" + key + "]");
            jsonObject.put(JsonFormConstants.VALUE, array);
        }
    }

    private static void setFormFieldInitDataCleanUp(Map<String, String> childDetails, String prefix) {
        //Inject if missing for entity age processing
        String dobUnknownField = prefix.startsWith(Constants.KEY.MOTHER) ? Constants.JSON_FORM_KEY.MOTHER_GUARDIAN_DATE_BIRTH_UNKNOWN : Constants.JSON_FORM_KEY.DATE_BIRTH_UNKNOWN;
        String dobUnknownKey = prefix + "dob_unknown";

        if (childDetails.containsKey(dobUnknownField) && !childDetails.containsKey(dobUnknownKey)) {
            childDetails.put(dobUnknownKey, childDetails.get(dobUnknownField));
        }
    }

    private static void processTree(JSONObject jsonObject, String entity) throws JSONException {
        List<String> entityHierarchy = null;

        if (entity != null) {
            if (entity.equalsIgnoreCase("other")) {
                entityHierarchy = new ArrayList<>();
                entityHierarchy.add(entity);
            } else {
                entityHierarchy = LocationHelper.getInstance().getOpenMrsLocationHierarchy(entity, false);
            }
        }

        String birthFacilityHierarchyString = AssetHandler.javaToJsonString(entityHierarchy, new TypeToken<List<String>>() {
        }.getType());
        jsonObject.put(ChildJsonFormUtils.VALUE, birthFacilityHierarchyString);
    }

    protected static void processPhoto(String baseEntityId, JSONObject jsonObject) throws JSONException {
        if (StringUtils.isNotBlank(baseEntityId)) {
            Photo photo = ImageUtils.profilePhotoByClientID(baseEntityId, Utils.getProfileImageResourceIDentifier());
            if (StringUtils.isNotBlank(photo.getFilePath())) {
                jsonObject.put(ChildJsonFormUtils.VALUE, photo.getFilePath());
            }
        }
    }

    protected static void processAge(String dobString, JSONObject jsonObject) throws JSONException {
        if (StringUtils.isNotBlank(dobString)) {
            jsonObject.put(ChildJsonFormUtils.VALUE, Utils.getAgeFromDate(dobString));
        }
    }

    protected static void processDate(Map<String, String> childDetails, String prefix, JSONObject jsonObject) throws JSONException {
        String key = jsonObject.getString(ChildJsonFormUtils.OPENMRS_ENTITY_ID).equalsIgnoreCase(FormEntityConstants.Person.birthdate.toString()) ? prefix + Constants.KEY.DOB : jsonObject.getString(ChildJsonFormUtils.KEY);
        String dateString = Utils.getValue(childDetails, key, false);
        dateString = StringUtils.isBlank(dateString) ? Utils.getValue(childDetails, key.toLowerCase(Locale.ENGLISH), false) : dateString;
        String isDOBUnknown = childDetails.get(prefix + Constants.KEY.DOB_UNKNOWN);
        if (isDOBUnknown == null || !Boolean.valueOf(isDOBUnknown)) {
            Date date = Utils.dobStringToDate(dateString);
            if (StringUtils.isNotBlank(dateString) && date != null) {
                jsonObject.put(ChildJsonFormUtils.VALUE, DATE_FORMAT.format(date));
            }
        }
    }

    protected static String getMappedValue(String key, Map<String, String> childDetails) {
        String value = Utils.getValue(childDetails, key.toUpperCase(Locale.ENGLISH), false);

        return !TextUtils.isEmpty(value) ? value : Utils.getValue(childDetails, key.toLowerCase(Locale.ENGLISH), false);
    }

    protected static Triple<Boolean, JSONObject, JSONArray> validateParameters(String jsonString, String step) {
        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = fields(jsonForm, step);

        return Triple.of(jsonForm != null && fields != null, jsonForm, fields);
    }

    public static JSONArray fields(JSONObject jsonForm, String step) {
        try {
            JSONObject step1 = jsonForm.has(step) ? jsonForm.getJSONObject(step) : null;

            if (step1 == null) {
                return null;
            }

            return step1.has(FIELDS) ? step1.getJSONArray(FIELDS) : null;
        } catch (JSONException e) {
            Timber.e(e, "JsonFormUtils --> fields");
        }
        return null;
    }

    public static FormTag formTag(AllSharedPreferences allSharedPreferences) {
        FormTag formTag = new FormTag();
        formTag.providerId = allSharedPreferences.fetchRegisteredANM();
        formTag.team = allSharedPreferences.fetchDefaultTeam(allSharedPreferences.fetchRegisteredANM());
        formTag.teamId = allSharedPreferences.fetchDefaultTeamId(allSharedPreferences.fetchRegisteredANM());
        formTag.appVersion = ChildLibrary.getInstance().getApplicationVersion();
        formTag.appVersionName = ChildLibrary.getInstance().getApplicationVersionName();
        formTag.databaseVersion = ChildLibrary.getInstance().getDatabaseVersion();
        return formTag;
    }

    public static String getFieldValue(String jsonString, String step, String key) {
        JSONObject jsonForm = toJSONObject(jsonString);
        if (jsonForm == null) {
            return null;
        }

        JSONArray fields = fields(jsonForm, step);
        if (fields == null) {
            return null;
        }

        return getFieldValue(fields, key);
    }

    public static ChildEventClient processMotherRegistrationForm(String jsonString, String relationalId, ChildEventClient base) {
        try {
            return processParentEventForm(jsonString, relationalId, base, Constants.KEY.MOTHER);
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> processMotherRegistrationForm");
            return null;
        }
    }

    public static ChildEventClient processFatherRegistrationForm(String jsonString, String relationalId, ChildEventClient base) {
        try {
            return processParentEventForm(jsonString, relationalId, base, Constants.KEY.FATHER);
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> processFatherRegistrationForm");
            return null;
        }
    }

    @Nullable
    private static ChildEventClient processParentEventForm(String jsonString, String relationalId, ChildEventClient childEventClient, String bindType)
            throws JSONException {

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);

        if (bindType.equals(Constants.KEY.FATHER)) {
            boolean isFatherDetailsValid = validateFatherDetails(jsonString);
            if (!isFatherDetailsValid) {
                return null;
            }
        }

        if (!registrationFormParams.getLeft()) {
            return null;
        } else {

            Client baseClient = childEventClient.getClient();
            Event baseEvent = childEventClient.getEvent();

            JSONObject jsonForm = registrationFormParams.getMiddle();
            JSONArray fields = registrationFormParams.getRight();

            JSONObject metadata = getJSONObject(jsonForm, METADATA);

            JSONObject lookUpJSONObject = getJSONObject(metadata, Constants.KEY.LOOK_UP);

            //Currently lookup works only for mothers - do not create new events for existing mothers.
            if (lookUpJSONObject != null && bindType.equalsIgnoreCase(Constants.KEY.MOTHER) &&
                    StringUtils.isNotBlank(getString(lookUpJSONObject, JsonFormConstants.VALUE))) {
                return null;
            } else {

                processLocationFields(fields);

                dobUnknownUpdateFromAge(fields, Constants.KEY.MOTHER);

                Event subFormEvent = null;

                Client subformClient = createSubFormClient(fields, baseClient, bindType, relationalId);

                //only set default gender if not explicitly set in the registration form
                if (StringUtils.isBlank(subformClient.getGender()) && bindType.equalsIgnoreCase(Constants.KEY.MOTHER)) {
                    subformClient.setGender(Constants.GENDER.FEMALE);
                } else if (StringUtils.isBlank(subformClient.getGender()) && bindType.equalsIgnoreCase(Constants.KEY.FATHER)) {
                    subformClient.setGender(Constants.GENDER.MALE);
                }

                if (baseEvent != null) {
                    JSONObject subBindTypeJson = getJSONObject(jsonForm, bindType);
                    if (subBindTypeJson != null) {
                        String subBindTypeEncounter = getString(subBindTypeJson, ENCOUNTER_TYPE);
                        if (StringUtils.isNotBlank(subBindTypeEncounter)) {
                            subFormEvent = ChildJsonFormUtils.createSubFormEvent(getEntityFields(fields, bindType), metadata, baseEvent, subformClient.getBaseEntityId(), subBindTypeEncounter, bindType);
                        }
                    }
                }

                lastInteractedWith(fields);

                return new ChildEventClient(subformClient, subFormEvent);
            }
        }
    }

    private static boolean validateFatherDetails(String jsonString) {
        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = fields(jsonForm);
        boolean isFormValid = false;

        // Further validate father details field since they are optional
        if (jsonForm.has(Constants.KEY.FATHER) && fields != null) {
            for (int fieldIndex = 0; fieldIndex < fields.length(); fieldIndex++) {
                try {
                    JSONObject field = fields.getJSONObject(fieldIndex);
                    if (field.has(ENTITY_ID) && field.getString(ENTITY_ID).equalsIgnoreCase(Constants.KEY.FATHER) &&
                            field.has(JsonFormConstants.VALUE)) {
                        String value = field.getString(JsonFormConstants.VALUE);
                        isFormValid = StringUtils.isNotBlank(value);
                        if (isFormValid) {
                            //TODO Fix bug in spinner setting value as the hint/label when nothing is selected - Native Form issue
                            if (field.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.SPINNER)
                                    && value.equalsIgnoreCase(field.getString(JsonFormConstants.HINT))) {
                                isFormValid = false;
                                continue;
                            }
                            break;
                        }
                    }
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }
        }
        return isFormValid;
    }

    /**
     * Adds relationship as defined in the  ec_client_relationship.json file.
     * <p>
     * create ec_client_relationship.json file in your assets directory that is a json array in the format
     * [
     * {
     * "client_relationship": "mother",
     * "field": "entity_id",
     * "comment": "Mother relational id"
     * },
     * {
     * "client_relationship": "father",
     * "field": "entity_id",
     * "comment": "Father relational id"
     * }
     * ]
     *
     * @param childClient childClient client object
     * @param jsonString  form json
     */
    private static void addRelationships(Client childClient, String jsonString) {
        try {
            JSONObject jsonForm = toJSONObject(jsonString);
            JSONObject metadata = getJSONObject(jsonForm, METADATA);
            JSONObject lookUpJSONObject = getJSONObject(metadata, Constants.KEY.LOOK_UP);

            String existingMotherRelationalId = null;
            if (lookUpJSONObject != null) {
                existingMotherRelationalId = getString(lookUpJSONObject, JsonFormConstants.VALUE);
            }

            Context context = ChildLibrary.getInstance().context().applicationContext();
            JSONArray relationships = new JSONArray(AssetHandler.readFileFromAssetsFolder(FormUtils.ecClientRelationships, context));
            JSONObject client = ChildLibrary.getInstance().eventClientRepository().getClientByBaseEntityId(childClient.getBaseEntityId());
            if (client != null && client.has(Constants.JSON_FORM_KEY.RELATIONSHIPS)) {
                JSONObject relationshipsJson = client.getJSONObject(Constants.JSON_FORM_KEY.RELATIONSHIPS);
                for (int i = 0; i < relationships.length(); i++) {
                    JSONObject relationship = relationships.getJSONObject(i);
                    if (relationship.has(Constants.CLIENT_RELATIONSHIP)) {
                        String relationshipType = relationship.getString(Constants.CLIENT_RELATIONSHIP);
                        if (relationshipsJson.has(relationshipType)) {
                            childClient.addRelationship(relationshipType, String.valueOf(relationshipsJson.getJSONArray(relationshipType).get(0)));
                        }
                    }
                }
                return;
            }

            //Special case - add relationship for existing mothers when creating this child as a sibling
            if (StringUtils.isNotBlank(existingMotherRelationalId)) {
                childClient.addRelationship(Constants.KEY.MOTHER, existingMotherRelationalId);
            }
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> addRelationship");
        }
    }

    /**
     * Get Relations for the child with the provided entity id
     *
     * @param baseEntityId child base entity id
     * @param bindType     type of relationship e.g father, mother
     * @return First relational id of the given relation type
     * @throws JSONException when it fails to retrieve the relationships
     */
    public static String getRelationalIdByType(String baseEntityId, String bindType) throws JSONException {
        JSONObject client = ChildLibrary.getInstance().eventClientRepository().getClientByBaseEntityId(baseEntityId);
        if (client != null && client.has(Constants.JSON_FORM_KEY.RELATIONSHIPS)) {
            JSONObject relationships = client.getJSONObject(Constants.JSON_FORM_KEY.RELATIONSHIPS);
            if (relationships.has(bindType)) {
                return String.valueOf(relationships.getJSONArray(bindType).get(0));
            }
        }
        return null;
    }

    private static Client createSubFormClient(JSONArray fields, Client parent, String bindType, String entityRelationId) {
        if (StringUtils.isBlank(bindType)) {
            return null;
        }
        String stringBirthDate = getSubFormFieldValue(fields, FormEntityConstants.Person.birthdate, bindType);
        Map<String, String> identifierMap = getSubFormIdentifierMap(bindType);
        Date birthDate = formatDate(stringBirthDate, true);
        birthDate = cleanBirthDateForSave(birthDate);//Fix weird bug day decrements on save
        String stringDeathDate = getSubFormFieldValue(fields, FormEntityConstants.Person.deathdate, bindType);
        Date deathDate = formatDate(stringDeathDate, true);
        deathDate = cleanBirthDateForSave(deathDate);
        String approxBirthDate = getSubFormFieldValue(fields, FormEntityConstants.Person.birthdate_estimated, bindType);
        boolean birthDateApprox = isDateApprox(approxBirthDate);
        String approxDeathDate = getSubFormFieldValue(fields, FormEntityConstants.Person.deathdate_estimated, bindType);
        boolean deathDateApprox = isDateApprox(approxDeathDate);

        List<Address> addresses = new ArrayList<>(extractAddresses(fields, bindType).values());

        Map<String, String> clientMap = getClientAttributes(fields, bindType, entityRelationId);

        Client client = getClient(clientMap, birthDate, deathDate, birthDateApprox, deathDateApprox);
        client.withAddresses(addresses).withAttributes(extractAttributes(fields, clientMap.get(Constants.BIND_TYPE))).withIdentifiers(identifierMap);

        if (addresses.isEmpty()) {
            client.withAddresses(parent.getAddresses());
        }

        return client;
    }

    /**
     * Fixes weird bug where day decrements on save
     *
     * @param birthDate birth date to process
     */
    @NotNull
    private static Date cleanBirthDateForSave(Date birthDate) {
        if (birthDate != null) {
            return new LocalDateTime(birthDate.getTime()).plusHours(12).toDate();
        } else {
            return null;
        }
    }

    @NotNull
    private static Client getClient(Map<String, String> clientMap, Date birthDate, Date deathDate,
                                    boolean birthDateApprox, boolean deathDateApprox) {

        return (Client) new Client(clientMap.get(Constants.ENTITY_ID))
                .withFirstName(clientMap.get(Constants.FIRST_NAME)).withMiddleName(clientMap.get(Constants.MIDDLE_NAME)).withLastName(clientMap.get(Constants.LAST_NAME))
                .withBirthdate(birthDate, birthDateApprox)
                .withDeathdate(deathDate, deathDateApprox)
                .withGender(clientMap.get(GENDER))
                .withDateCreated(new Date());
    }

    private static Map<String, String> getClientAttributes(JSONArray fields, String bindType, String relationalId) {

        String entityId = TextUtils.isEmpty(relationalId) ? generateRandomUUIDString() : relationalId;
        String firstName = getSubFormFieldValue(fields, FormEntityConstants.Person.first_name, bindType);
        String middleName = getSubFormFieldValue(fields, FormEntityConstants.Person.middle_name, bindType);
        String lastName = getSubFormFieldValue(fields, FormEntityConstants.Person.last_name, bindType);
        String gender = getSubFormFieldValue(fields, FormEntityConstants.Person.gender, bindType);

        Map<String, String> attributes = new HashMap<>();
        attributes.put(Constants.ENTITY_ID, entityId);
        attributes.put(Constants.FIRST_NAME, firstName);
        attributes.put(Constants.MIDDLE_NAME, middleName);
        attributes.put(Constants.LAST_NAME, lastName);
        attributes.put(GENDER, gender);
        attributes.put(Constants.BIND_TYPE, bindType);
        return attributes;
    }

    @NotNull
    private static Map<String, String> getSubFormIdentifierMap(String bindType) {
        Map<String, String> identifiers = new HashMap<>();
        String parentZEIRId = Utils.getNextOpenMrsId();
        if (StringUtils.isBlank(parentZEIRId)) {
            return identifiers;
        }
        if (bindType.equalsIgnoreCase(Constants.KEY.MOTHER)) {
            identifiers.put(M_ZEIR_ID, parentZEIRId);
        } else if (bindType.equalsIgnoreCase(Constants.KEY.FATHER)) {
            identifiers.put(F_ZEIR_ID, parentZEIRId);
        }
        return identifiers;
    }

    private static boolean isDateApprox(String approxDate) {
        boolean dateApprox = false;
        if (!StringUtils.isEmpty(approxDate) && NumberUtils.isNumber(approxDate)) {
            int date = 0;
            try {
                date = Integer.parseInt(approxDate);
            } catch (Exception e) {
                Timber.e(e);
            }
            dateApprox = date > 0;
        }
        return dateApprox;
    }

    private static Event createSubFormEvent(JSONArray fields, JSONObject metadata, Event parent,
                                            String entityId, String encounterType, String bindType) {

        List<EventClient> eventClients = ChildLibrary.getInstance().eventClientRepository()
                .getEventsByBaseEntityIdsAndSyncStatus(BaseRepository.TYPE_Unsynced, Collections.singletonList(entityId));

        Set<String> eligibleBindTypeEvents = eventTypeMap.get(bindType);
        EventClient existingEventClient = null;
        if (eligibleBindTypeEvents != null) {
            for (EventClient eventClient : eventClients) {
                if (eligibleBindTypeEvents.contains(eventClient.getEvent().getEventType())) {
                    existingEventClient = eventClient;
                    break;
                }
            }
        }
        boolean alreadyExists = eventClients.size() > 0;

        org.smartregister.domain.Event existingEvent = existingEventClient != null ? existingEventClient.getEvent() : null;

        Event event = getSubFormEvent(parent, entityId, encounterType, bindType, alreadyExists, existingEvent);
        addSubFormEventObservations(fields, event);
        updateMetadata(metadata, event);

        return event;
    }

    private static void addSubFormEventObservations(JSONArray fields, Event event) {
        if (fields != null && fields.length() != 0)
            addSaveReportDeceasedObservations(fields, event);
    }

    private static Event getSubFormEvent(Event parent, String entityId, String encounterType,
                                         String bindType, boolean alreadyExists, org.smartregister.domain.Event existingEvent) {

        Event event = (Event) new Event().withBaseEntityId(alreadyExists ? existingEvent.getBaseEntityId() : entityId)
                .withEventDate(parent.getEventDate())
                .withEventType(alreadyExists ? existingEvent.getEventType() : encounterType)
                .withEntityType(bindType)
                .withFormSubmissionId(alreadyExists ? existingEvent.getFormSubmissionId() : generateRandomUUIDString())
                .withDateCreated(new Date());

        tagSyncMetadata(event);

        return event;
    }

    private static JSONArray getEntityFields(JSONArray fields, String mother) throws JSONException {
        JSONArray array = new JSONArray();

        for (int i = 0; i < fields.length(); i++) {
            if (fields.getJSONObject(i).has(ENTITY_ID) && fields.getJSONObject(i).getString(ENTITY_ID).equals(mother)) {
                array.put(fields.getJSONObject(i));
            }
        }
        return array;
    }

    public static void processOutOfAreaService(String jsonString, ChildRegisterContract.ProgressDialogCallback progressDialogCallback) {
        SaveOutOfAreaServiceTask saveOutOfAreaServiceTask = new SaveOutOfAreaServiceTask(ChildLibrary.getInstance().context().applicationContext(), jsonString, progressDialogCallback);
        Utils.startAsyncTask(saveOutOfAreaServiceTask, null);
    }

    public static boolean processMoveToCatchment(Context context, AllSharedPreferences allSharedPreferences, JSONObject jsonObject) {
        try {
            int eventsCount = jsonObject.has(Constants.NO_OF_EVENTS) ? jsonObject.getInt(Constants.NO_OF_EVENTS) : 0;
            if (eventsCount == 0) {
                return false;
            }

            JSONArray events = getOutOFCatchmentJsonArray(jsonObject, Constants.EVENTS);
            JSONArray clients = getOutOFCatchmentJsonArray(jsonObject, Constants.CLIENTS);

            ChildLibrary.getInstance().getEcSyncHelper().batchSave(events, clients);
            addProcessMoveToCatchment(context, allSharedPreferences, createEventList(events));
            processClients(allSharedPreferences, ChildLibrary.getInstance().getEcSyncHelper());

            List<String> clientIds = getClientIdsFromClientsJsonArray(clients);
            Timber.i("Moved %s  client(s) to new catchment area.", clientIds.size());

            return true;
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> processMoveToCatchment");
        }

        return false;
    }

    private static List<String> getClientIdsFromClientsJsonArray(JSONArray clients) throws JSONException {
        List<String> clientBaseEntityIds = new ArrayList<>();

        for (int i = 0; i < clients.length(); i++) {
            if (!clients.getJSONObject(i).getJSONObject(IDENTIFIERS).has(M_ZEIR_ID)) {
                clientBaseEntityIds.add(clients.getJSONObject(i).getString("baseEntityId"));
                ContentValues v = new ContentValues();
                v.put(Constants.KEY.LAST_INTERACTED_WITH, Calendar.getInstance().getTimeInMillis());
                updateChildFTSTables(v, clients.getJSONObject(i).getString("baseEntityId"));
            }
        }

        return clientBaseEntityIds;
    }

    private static JSONArray getOutOFCatchmentJsonArray(JSONObject jsonObject, String clients) throws JSONException {
        return jsonObject.has(clients) ? jsonObject.getJSONArray(clients) : new JSONArray();
    }

    private static List<Pair<Event, JSONObject>> createEventList(JSONArray events) throws JSONException {
        List<Pair<Event, JSONObject>> eventList = new ArrayList<>();

        for (int i = 0; i < events.length(); i++) {
            JSONObject jsonEvent = events.getJSONObject(i);
            Event event = ChildLibrary.getInstance().getEcSyncHelper().convert(jsonEvent, Event.class);
            if (event == null) {
                continue;
            }

            // Skip previous move to catchment events
            if (MoveToMyCatchmentUtils.MOVE_TO_CATCHMENT_EVENT.equals(event.getEventType())) {
                continue;
            }

            if (Constants.EventType.BITRH_REGISTRATION.equals(event.getEventType())) {
                eventList.add(0, Pair.create(event, jsonEvent));
            } else if (!eventList.isEmpty() && Constants.EventType.NEW_WOMAN_REGISTRATION.equals(event.getEventType())) {
                eventList.add(1, Pair.create(event, jsonEvent));
            } else {
                eventList.add(Pair.create(event, jsonEvent));
            }
        }

        return eventList;
    }

    private static void addProcessMoveToCatchment(Context context, AllSharedPreferences allSharedPreferences, List<Pair<Event, JSONObject>> eventList) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        String locationId = allSharedPreferences.fetchDefaultLocalityId(providerId);

        //The identifiers for provider we are transferring TO
        Identifiers localProviderIdentifiers = new Identifiers();
        localProviderIdentifiers.setProviderId(allSharedPreferences.fetchRegisteredANM());
        localProviderIdentifiers.setLocationId(locationId);
        localProviderIdentifiers.setChildLocationId(ChildJsonFormUtils.getChildLocationId(locationId, allSharedPreferences));
        localProviderIdentifiers.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        localProviderIdentifiers.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));

        for (Pair<Event, JSONObject> pair : eventList) {
            Event event = pair.first;
            JSONObject jsonEvent = pair.second;

            if (Utils.metadata().childRegister.registerEventType.equals(event.getEventType())) {
                updateHomeFacility(locationId, event);
            }

            if (Constants.EventType.BITRH_REGISTRATION.equals(event.getEventType())
                    || Constants.EventType.NEW_WOMAN_REGISTRATION.equals(event.getEventType())
                    || Constants.EventType.FATHER_REGISTRATION.equals(event.getEventType())) {
                createMoveToCatchmentEvent(context, localProviderIdentifiers, event);
            }

            /*
            //To do uncomment to handle reports refresh

            if (Constants.EventType.VACCINATION.equals(event.getEventType())) {
                for (Obs obs : event.getObs()) {
                    if (obs.getFieldCode().equals(Constants.CONCEPT.VACCINE_DATE)) {

                        String vaccineName = obs.getFormSubmissionField();
                        setVaccineAsInvalid(event.getBaseEntityId(), vaccineName);
                    }
                }
            }
            */

            // Update tags and Save unsynced event
            ChildJsonFormUtils.tagSyncMetadata(event);
            event.setVersion(System.currentTimeMillis());
            JSONObject updatedJsonEvent = ChildLibrary.getInstance().getEcSyncHelper().convertToJson(event);
            jsonEvent = ChildJsonFormUtils.merge(jsonEvent, updatedJsonEvent);

            ChildLibrary.getInstance().getEcSyncHelper().addEvent(event.getBaseEntityId(), jsonEvent);
        }
    }

    private static void createMoveToCatchmentEvent(Context context, Identifiers transferToIdentifiers, Event event) {
        //Create move to catchment event;
        Event moveToCatchmentEvent = ChildJsonFormUtils.processChangeOfCatchmentObservations(context, transferToIdentifiers, event);
        if (moveToCatchmentEvent != null) {
            JSONObject moveToCatchmentJsonEvent = ChildLibrary.getInstance().getEcSyncHelper().convertToJson(moveToCatchmentEvent);
            if (moveToCatchmentJsonEvent != null) {
                ChildLibrary.getInstance().getEcSyncHelper().addEvent(moveToCatchmentEvent.getBaseEntityId(), moveToCatchmentJsonEvent);
            }
        }
    }

    private static void updateHomeFacility(String toLocationId, Event event) {
        // Update home facility
        for (Obs obs : event.getObs()) {
            if (obs.getFormSubmissionField().equals(Constants.HOME_FACILITY)) {
                List<Object> values = new ArrayList<>();
                values.add(toLocationId);
                obs.setValues(values);
                break;
            }
        }
    }

    public static Event processChangeOfCatchmentObservations(Context context, Identifiers toIdentifiers, Event referenceEvent) {
        try {
            //From Identifiers
            String fromLocationId = referenceEvent.getLocationId();

            //To identifiers
            String toProviderId = toIdentifiers.getProviderId();
            String toLocationId = toIdentifiers.getLocationId();
            String toChildLocationId = toIdentifiers.getChildLocationId();
            String toTeam = toIdentifiers.getTeam();
            String toTeamId = toIdentifiers.getTeamId();

            //Same location/provider, no need to move
            if (toLocationId.equals(fromLocationId) || referenceEvent.getProviderId().equals(toProviderId)) {
                return null;
            }

            final String FORM_SUBMISSION_FIELD = "formsubmissionField";
            final String DATA_TYPE = "text";

            Event event = getEvent(referenceEvent.getProviderId(), fromLocationId, referenceEvent.getBaseEntityId(), MoveToMyCatchmentUtils.MOVE_TO_CATCHMENT_EVENT, new Date(), Constants.CHILD_TYPE);

            String formSubmissionField = "From_ProviderId";
            List<Object> vall = new ArrayList<>();
            vall.add(referenceEvent.getProviderId());
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            formSubmissionField = "From_LocationId";
            vall = new ArrayList<>();
            vall.add(referenceEvent.getLocationId());
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));


            formSubmissionField = "From_Child_LocationId";
            vall = new ArrayList<>();
            vall.add(referenceEvent.getChildLocationId());
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            formSubmissionField = "From_Team";
            vall = new ArrayList<>();
            vall.add(referenceEvent.getTeam());
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            formSubmissionField = "From_TeamId";
            vall = new ArrayList<>();
            vall.add(referenceEvent.getTeamId());
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            formSubmissionField = "To_ProviderId";
            vall = new ArrayList<>();
            vall.add(toProviderId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            formSubmissionField = "To_LocationId";
            vall = new ArrayList<>();
            vall.add(toLocationId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));


            formSubmissionField = "To_Child_LocationId";
            vall = new ArrayList<>();
            vall.add(toChildLocationId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            formSubmissionField = "To_Team";
            vall = new ArrayList<>();
            vall.add(toTeam);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            formSubmissionField = "To_TeamId";
            vall = new ArrayList<>();
            vall.add(toTeamId);
            event.addObs(new Obs(FORM_SUBMISSION_FIELD, DATA_TYPE, formSubmissionField, "", vall, new ArrayList<>(), null,
                    formSubmissionField));

            addMetaData(context, event, new Date());

            return event;
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> createMoveToCatchmentEvent");
            return null;
        }
    }

    /**
     * Starts an instance of JsonFormActivity with the provided form details
     *
     * @param context                     The activity form is being launched from
     * @param jsonFormActivityRequestCode The request code to be used to launch {@link BaseChildFormActivity}
     * @param formName                    The name of the form to launch
     * @param uniqueId                    The unique entity id for the form (e.g child's ZEIR id)
     * @param currentLocationId           OpenMRS id for the current device's location
     * @throws Exception
     */
    public static void startForm(Activity context, int jsonFormActivityRequestCode, String formName,
                                 String uniqueId, String currentLocationId) throws Exception {

        String entityId = uniqueId;
        JSONObject form = new FormUtils(context).getFormJson(formName);
        if (form != null) {
            form.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);
            if (Utils.metadata().childRegister.formName.equals(formName)) {
                if (StringUtils.isBlank(entityId)) {
                    UniqueIdRepository uniqueIdRepo = CoreLibrary.getInstance().context().getUniqueIdRepository();
                    entityId = uniqueIdRepo.getNextUniqueId() != null ? uniqueIdRepo.getNextUniqueId().getOpenmrsId() : "";
                    if (entityId.isEmpty()) {
                        Utils.showShortToast(context, context.getString(R.string.no_openmrs_id));
                        return;
                    }
                }

                addRegistrationFormLocationHierarchyQuestions(form);
            } else if (Constants.JsonForm.OUT_OF_CATCHMENT_SERVICE.equals(formName)) {
                addAvailableVaccines(context, form);
            }

            // Inject opensrp id into the form
            injectOpenSrpId(entityId, form);

            Form formParam = new Form();
            formParam.setWizard(true);
            formParam.setHideSaveLabel(true);
            formParam.setNextLabel("");

            Intent intent = new Intent(context, Utils.metadata().childFormActivity);
            intent.putExtra("json", form.toString());
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, formParam);
            if (Boolean.parseBoolean(ChildLibrary.getInstance().getProperties()
                    .getProperty(ChildAppProperties.KEY.MULTI_LANGUAGE_SUPPORT, "false"))) {
                intent.putExtra(JsonFormConstants.PERFORM_FORM_TRANSLATION, true);
            }
            Timber.d("JsonFormUtils --> form is %s", form.toString());
            context.startActivityForResult(intent, jsonFormActivityRequestCode);
        }
    }

    private static void injectOpenSrpId(String entityId, JSONObject form) throws JSONException {
        if (StringUtils.isNoneBlank(entityId)) {
            JSONArray fields = form.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                if (field.getString(JsonFormConstants.KEY).equalsIgnoreCase(ZEIR_ID) ||
                        field.getString(JsonFormUtils.KEY).equalsIgnoreCase(OPENSRP_ID)) {
                    field.remove(JsonFormUtils.VALUE);
                    field.put(JsonFormUtils.VALUE, entityId.replace("-", ""));
                    field.put(READ_ONLY, true);
                    break;
                }
            }
        }
    }

    public static void createBCGScarEvent(Context context, String baseEntityId, String providerId, String locationId) {
        try {
            Event event = getEvent(providerId, locationId, baseEntityId, BCG_SCAR_EVENT, new Date(), Constants.CHILD_TYPE);

            final String BCG_SCAR_CONCEPT = "160265AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            final String YES_CONCEPT = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

            List<Object> values = new ArrayList<>();
            values.add(YES_CONCEPT);

            List<Object> humanReadableValues = new ArrayList<>();
            humanReadableValues.add("Yes");

            event.addObs(
                    new Obs(CONCEPT, "select one", BCG_SCAR_CONCEPT, "", values, humanReadableValues, null, "bcg_scar"));

            JSONObject eventJson = new JSONObject(gson.toJson(event));
            if (eventJson != null) {
                ECSyncHelper.getInstance(context).addEvent(baseEntityId, eventJson);
            }
        } catch (Exception e) {
            Timber.e(e, "JsonFormUtils --> createBCGScarEvent");
        }
    }

    public static Map<String, String> updateClientAttribute(org.smartregister.Context openSRPContext, CommonPersonObjectClient childDetails, LocationHelper locationHelper, String attributeName, Object attributeValue) throws Exception {
        Date date = new Date();
        EventClientRepository db = openSRPContext.getEventClientRepository();

        JSONObject client = db.getClientByBaseEntityId(childDetails.entityId());
        JSONObject attributes = client.getJSONObject(ChildJsonFormUtils.attributes);
        attributes.put(attributeName, attributeValue);
        client.remove(ChildJsonFormUtils.attributes);
        client.put(ChildJsonFormUtils.attributes, attributes);
        db.addorUpdateClient(childDetails.entityId(), client);

        ContentValues contentValues = new ContentValues();
        //Add the base_entity_id
        contentValues.put(attributeName.toLowerCase(), attributeValue.toString());

        ChildDbUtils.updateChildDetailsValue(attributeName.toLowerCase(), String.valueOf(attributeValue), childDetails.entityId());

        AllSharedPreferences allSharedPreferences = openSRPContext.allSharedPreferences();
        String locationName = allSharedPreferences.fetchCurrentLocality();
        if (StringUtils.isBlank(locationName)) {
            locationName = locationHelper.getDefaultLocation();
        }

        Event event = getEvent(allSharedPreferences.fetchRegisteredANM(), locationHelper.getOpenMrsLocationId(locationName), childDetails.entityId(), ChildJsonFormUtils.updateBirthRegistrationDetailsEncounter, new Date(), Constants.CHILD_TYPE);

        ChildJsonFormUtils.addMetaData(openSRPContext.applicationContext(), event, date);
        JSONObject eventJson = new JSONObject(ChildJsonFormUtils.gson.toJson(event));
        db.addEvent(childDetails.entityId(), eventJson);
        processClients(allSharedPreferences, ECSyncHelper.getInstance(openSRPContext.applicationContext()));

        //update details
        Map<String, String> detailsMap = ChildDbUtils.fetchChildDetails(childDetails.entityId());
        if (detailsMap != null) {
            if (childDetails.getColumnmaps().containsKey(attributeName)) {
                childDetails.getColumnmaps().put(attributeName, attributeValue.toString());
            }
            Utils.putAll(detailsMap, childDetails.getColumnmaps());
        }

        return detailsMap;
    }

    /**
     * This method is used to process the result returned by advance search using the new approach.
     * To provide more context. The new advance search method returns a list of clients including their relationships
     * in one result. This processing is done to map the client to their relationships for instance map a child to their mother and or their father
     *
     * @param clientSearchResponse JSON string retrieved from the server
     * @return a list of client detail models
     */
    public static List<ChildMotherDetailModel> processReturnedAdvanceSearchResults(Response<String> clientSearchResponse) {
        List<ChildMotherDetailModel> childMotherDetailModels = new ArrayList<>();
        Set<String> processedClients = new HashSet<>();
        try {
            if (clientSearchResponse.status().equals(ResponseStatus.success)) {
                JSONArray searchResults = new JSONArray(clientSearchResponse.payload());
                for (int index = 0; index < searchResults.length(); index++) {
                    JSONObject searchResult = searchResults.getJSONObject(index);
                    String baseEntityId = searchResult.getString(Constants.Client.BASE_ENTITY_ID);

                    if (!searchResult.has(Constants.Client.RELATIONSHIPS) || processedClients.contains(baseEntityId)) {
                        continue;
                    }

                    JSONObject relationships = searchResult.getJSONObject(Constants.Client.RELATIONSHIPS);
                    if (relationships != null && relationships.has(Constants.KEY.MOTHER)) {
                        JSONObject motherJson = getRelationshipJson(searchResults, relationships.getJSONArray(Constants.KEY.MOTHER).getString(0));
                        if (motherJson != null) {
                            childMotherDetailModels.add(new ChildMotherDetailModel(searchResult, motherJson));
                        }
                    }
                    processedClients.add(baseEntityId);
                }
                Collections.sort(childMotherDetailModels, Collections.reverseOrder());
            }

        } catch (JSONException e) {
            Timber.e(e);
        }
        return childMotherDetailModels;
    }

    /**
     * Return Json for provided relational id
     *
     * @param searchResults List of returned clients
     * @param relationalId  base entity id of the relation e.g mother base entity id
     * @return Json for the given relational id
     */
    public static JSONObject getRelationshipJson(JSONArray searchResults, String relationalId) throws JSONException {
        for (int index = 0; index < searchResults.length(); index++) {
            JSONObject searchResult = searchResults.getJSONObject(index);
            if (searchResult.has(Constants.Client.BASE_ENTITY_ID) &&
                    relationalId.equalsIgnoreCase(searchResult.getString(Constants.Client.BASE_ENTITY_ID))) {
                return searchResult;
            }
        }
        return null;
    }
}
