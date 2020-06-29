package org.smartregister.child.interactor;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.child.ChildLibrary;
import org.smartregister.child.contract.ChildRegisterContract;
import org.smartregister.child.domain.ChildEventClient;
import org.smartregister.child.domain.UpdateRegisterParams;
import org.smartregister.child.event.ClientDirtyFlagEvent;
import org.smartregister.child.util.AppExecutors;
import org.smartregister.child.util.ChildJsonFormUtils;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.FormEntityConstants;
import org.smartregister.domain.UniqueId;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.growthmonitoring.domain.HeightWrapper;
import org.smartregister.growthmonitoring.domain.WeightWrapper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.sync.ClientProcessor;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.AppProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by ndegwamartin on 25/02/2019.
 */
public class ChildRegisterInteractor implements ChildRegisterContract.Interactor {

    public static final String TAG = ChildRegisterInteractor.class.getName();
    private AppExecutors appExecutors;


    public ChildRegisterInteractor() {
        this(new AppExecutors());
    }

    @VisibleForTesting
    ChildRegisterInteractor(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    @Override
    public void onDestroy(boolean isChangingConfiguration) {
        //TODO set presenter or model to null
    }

    @Override
    public void getNextUniqueId(final Triple<String, Map<String, String>, String> triple,
                                final ChildRegisterContract.InteractorCallBack callBack) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                UniqueId uniqueId = getUniqueIdRepository().getNextUniqueId();
                final String entityId = uniqueId != null ? uniqueId.getOpenmrsId() : "";
                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (StringUtils.isBlank(entityId)) {
                            callBack.onNoUniqueId();
                        } else {
                            callBack.onUniqueIdFetched(triple, entityId);
                        }
                    }
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    @Override
    public void saveRegistration(final List<ChildEventClient> childEventClientList, final String jsonString,
                                 final UpdateRegisterParams updateRegisterParams,
                                 final ChildRegisterContract.InteractorCallBack callBack) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                saveRegistration(childEventClientList, jsonString, updateRegisterParams);
                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onRegistrationSaved(updateRegisterParams.isEditMode());
                    }
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    @Override
    public void removeChildFromRegister(final String closeFormJsonString, final String providerId) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //TODO add functionality to remove child from register
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    public void saveRegistration(List<ChildEventClient> childEventClientList, String jsonString, UpdateRegisterParams params) {

        try {
            List<String> currentFormSubmissionIds = new ArrayList<>();

            for (int i = 0; i < childEventClientList.size(); i++) {
                try {

                    ChildEventClient childEventClient = childEventClientList.get(i);
                    Client baseClient = childEventClient.getClient();
                    Event baseEvent = childEventClient.getEvent();

                    if (baseClient != null) {
                        JSONObject clientJson = new JSONObject(ChildJsonFormUtils.gson.toJson(baseClient));
                        if (params.isEditMode()) {
                            //Create new Father registration event in the case where the father details are provided while updating child/mother details.
                            if (Constants.EventType.FATHER_REGISTRATION.equalsIgnoreCase(baseEvent.getEventType())) {
                                addClient(jsonString, params, baseClient, clientJson);
                            } else {
                                try {
                                    ChildJsonFormUtils.mergeAndSaveClient(baseClient);
                                } catch (Exception e) {
                                    Timber.e(e, "ChildRegisterInteractor --> mergeAndSaveClient");
                                }
                            }
                        } else {
                            addClient(jsonString, params, baseClient, clientJson);
                        }
                    }

                    addEvent(params, currentFormSubmissionIds, baseEvent);
                    updateOpenSRPId(jsonString, params, baseClient);
                    addImageLocation(jsonString, i, baseClient, baseEvent);

                    //Broadcast after all processing is done
                    if (Constants.CHILD_TYPE.equals(baseEvent.getEntityType())) {
                        Utils.postEvent(new ClientDirtyFlagEvent(baseClient.getBaseEntityId(), baseEvent.getEventType()));
                    }

                } catch (Exception e) {

                    Timber.e(e, "ChildRegisterInteractor --> saveRegistration loop");
                }
            }

            long lastSyncTimeStamp = getAllSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            ChildLibrary.getInstance().getClientProcessorForJava().processClient(getSyncHelper().getEvents(currentFormSubmissionIds));

            getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        } catch (Exception e) {
            Timber.e(e, "ChildRegisterInteractor --> saveRegistration");
        }
    }

    private void addClient(String jsonString, UpdateRegisterParams params, Client baseClient, JSONObject clientJson) throws JSONException {
        getSyncHelper().addClient(baseClient.getBaseEntityId(), clientJson);

        // This prevents a crash when the birthdate of a mother is not available in the clientJson
        // We also don't need to process the mother's weight & height
        processWeight(baseClient.getIdentifiers(), jsonString, params, clientJson);
        processHeight(baseClient.getIdentifiers(), jsonString, params, clientJson);
    }

    private void addImageLocation(String jsonString, int i, Client baseClient, Event baseEvent) {
        if (baseClient != null || baseEvent != null) {
            String imageLocation = null;
            if (i == 0) {
                imageLocation = ChildJsonFormUtils.getFieldValue(jsonString, Constants.KEY.PHOTO);
            } else if (i == 1) {
                imageLocation =
                        ChildJsonFormUtils.getFieldValue(jsonString, ChildJsonFormUtils.STEP2, Constants.KEY.PHOTO);
            }

            if (StringUtils.isNotBlank(imageLocation)) {
                ChildJsonFormUtils.saveImage(baseEvent.getProviderId(), baseClient.getBaseEntityId(), imageLocation);
            }
        }
    }

    private void updateOpenSRPId(String jsonString, UpdateRegisterParams params, Client baseClient) {
        if (params.isEditMode()) {
            // Unassign current OPENSRP ID
            if (baseClient != null) {
                try {
                    String newOpenSRPId = baseClient.getIdentifier(ChildJsonFormUtils.ZEIR_ID).replace("-", "");
                    String currentOpenSRPId = ChildJsonFormUtils.getString(jsonString, ChildJsonFormUtils.CURRENT_ZEIR_ID).replace("-", "");
                    if (!newOpenSRPId.equals(currentOpenSRPId)) {
                        //OPENSRP ID was changed
                        getUniqueIdRepository().open(currentOpenSRPId);
                    }
                } catch (Exception e) {//might crash if M_ZEIR
                    Timber.d(e, "ChildRegisterInteractor --> unassign opensrp id");
                }
            }

        } else {
            if (baseClient != null) {
                //mark OPENSRP ID as used
                markUniqueIdAsUsed(baseClient.getIdentifier(ChildJsonFormUtils.ZEIR_ID));
                markUniqueIdAsUsed(baseClient.getIdentifier(ChildJsonFormUtils.M_ZEIR_ID));
                markUniqueIdAsUsed(baseClient.getIdentifier(ChildJsonFormUtils.F_ZEIR_ID));
            }
        }
    }

    private void markUniqueIdAsUsed(String openSrpId) {
        if (StringUtils.isNotBlank(openSrpId))
            getUniqueIdRepository().close(openSrpId);
    }

    private void addEvent(UpdateRegisterParams params, List<String> currentFormSubmissionIds, Event baseEvent) throws JSONException {
        if (baseEvent != null) {
            JSONObject eventJson = new JSONObject(ChildJsonFormUtils.gson.toJson(baseEvent));
            getSyncHelper().addEvent(baseEvent.getBaseEntityId(), eventJson, params.getStatus());
            currentFormSubmissionIds.add(eventJson.getString(EventClientRepository.event_column.formSubmissionId.toString()));
        }
    }

    public ECSyncHelper getSyncHelper() {
        return ChildLibrary.getInstance().getEcSyncHelper();
    }

    @Override
    public void processWeight(@NonNull Map<String, String> identifiers, @NonNull String jsonEnrollmentFormString, @NonNull UpdateRegisterParams params, @NonNull JSONObject clientJson) throws JSONException {
        String weight = ChildJsonFormUtils.getFieldValue(jsonEnrollmentFormString, ChildJsonFormUtils.STEP1, Constants.KEY.BIRTH_WEIGHT);

        // This prevents a crash when the birthdate of a mother is not available in the clientJson
        // We also don't need to process the mother's weight & height
        if (StringUtils.isNotBlank(weight) && !isClientMother(identifiers)) {
            WeightWrapper weightWrapper = new WeightWrapper();
            weightWrapper.setGender(clientJson.getString(FormEntityConstants.Person.gender.name()));
            weightWrapper.setWeight(!TextUtils.isEmpty(weight) ? Float.valueOf(weight) : null);
            LocalDate localDate = new LocalDate(Utils.getChildBirthDate(clientJson));
            weightWrapper.setUpdatedWeightDate(localDate.toDateTime(LocalTime.MIDNIGHT), (new LocalDate()).isEqual(localDate));//This is the weight of birth so reference date should be the DOB
            weightWrapper.setId(clientJson.getString(ClientProcessor.baseEntityIdJSONKey));
            weightWrapper.setDob(Utils.getChildBirthDate(clientJson));

            Utils.recordWeight(GrowthMonitoringLibrary.getInstance().weightRepository(), weightWrapper, params.getStatus());
        }
    }

    @Override
    public void processHeight(@NonNull Map<String, String> identifiers, @NonNull String jsonEnrollmentFormString, @NonNull UpdateRegisterParams params, @NonNull JSONObject clientJson) throws JSONException {
        String height = ChildJsonFormUtils.getFieldValue(jsonEnrollmentFormString, ChildJsonFormUtils.STEP1, Constants.KEY.BIRTH_HEIGHT);

        // This prevents a crash when the birthdate of a mother is not available in the clientJson
        // We also don't need to process the mother's weight & height
        if (StringUtils.isNotBlank(height) && !isClientMother(identifiers)) {
            HeightWrapper heightWrapper = new HeightWrapper();
            heightWrapper.setGender(clientJson.getString(FormEntityConstants.Person.gender.name()));
            heightWrapper.setHeight(!TextUtils.isEmpty(height) ? Float.parseFloat(height) : 0);
            LocalDate localDate = new LocalDate(Utils.getChildBirthDate(clientJson));
            heightWrapper.setUpdatedHeightDate(localDate.toDateTime(LocalTime.MIDNIGHT), (new LocalDate()).isEqual(localDate));
            heightWrapper.setId(clientJson.getString(ClientProcessor.baseEntityIdJSONKey));
            heightWrapper.setDob(Utils.getChildBirthDate(clientJson));

            Utils.recordHeight(GrowthMonitoringLibrary.getInstance().heightRepository(), heightWrapper, params.getStatus());
        }
    }

    @Override
    public boolean isClientMother(@NonNull Map<String, String> identifiers) {
        return identifiers.containsKey(ChildJsonFormUtils.M_ZEIR_ID);
    }

    public AllSharedPreferences getAllSharedPreferences() {
        return Utils.context().allSharedPreferences();
    }

    public ClientProcessorForJava getClientProcessorForJava() {
        return ChildLibrary.getInstance().getClientProcessorForJava();
    }

    public UniqueIdRepository getUniqueIdRepository() {
        return ChildLibrary.getInstance().getUniqueIdRepository();
    }

    protected AppProperties getAppProperties() {
        return CoreLibrary.getInstance().context().getAppProperties();
    }

    public enum type {SAVED, UPDATED}

}
