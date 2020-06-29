package org.smartregister.child.contract;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.child.domain.ChildEventClient;
import org.smartregister.child.domain.UpdateRegisterParams;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.view.contract.BaseRegisterContract;

import java.util.List;
import java.util.Map;

/**
 * Created by ndegwamartin on 25/02/2019.
 */
public interface ChildRegisterContract {

    interface View extends BaseRegisterContract.View {
        ChildRegisterContract.Presenter presenter();
    }

    interface Presenter extends BaseRegisterContract.Presenter {

        void saveLanguage(String language);

        void startForm(String formName, String entityId, String metadata, String currentLocationId) throws Exception;

        void startForm(String formName, String entityId, Map<String, String> metadata, String currentLocationId) throws Exception;

        void saveForm(String jsonString, UpdateRegisterParams updateRegisterParam);

        void saveOutOfCatchmentService(String jsonString, ChildRegisterContract.ProgressDialogCallback progressDialogCallback);

        void closeChildRecord(String jsonString);

    }

    interface Model {

        void registerViewConfigurations(List<String> viewIdentifiers);

        void unregisterViewConfiguration(List<String> viewIdentifiers);

        void saveLanguage(String language);

        String getLocationId(String locationName);

        List<ChildEventClient> processRegistration(String jsonString, FormTag formTag);

        JSONObject getFormAsJson(String formName, String entityId, String currentLocationId, Map<String, String> metadata) throws Exception;

        String getInitials();

    }

    interface Interactor {

        void onDestroy(boolean isChangingConfiguration);

        void getNextUniqueId(Triple<String, Map<String, String>, String> triple, ChildRegisterContract.InteractorCallBack callBack);

        void saveRegistration(final List<ChildEventClient> childEventClientList, final String jsonString,
                              final UpdateRegisterParams updateRegisterParams,
                              final ChildRegisterContract.InteractorCallBack callBack);

        void removeChildFromRegister(String closeFormJsonString, String providerId);

        void processWeight(@NonNull Map<String, String> identifiers, @NonNull String jsonEnrollmentFormString, @NonNull UpdateRegisterParams params, @NonNull JSONObject clientJson) throws JSONException;

        void processHeight(@NonNull Map<String, String> identifiers, @NonNull String jsonEnrollmentFormString, @NonNull UpdateRegisterParams params, @NonNull JSONObject clientJson) throws JSONException;

        boolean isClientMother(@NonNull Map<String, String> identifiers);

    }

    interface InteractorCallBack {

        void onUniqueIdFetched(Triple<String, Map<String, String>, String> triple, String entityId);

        void onNoUniqueId();

        void onRegistrationSaved(boolean isEdit);

    }

    interface ProgressDialogCallback {
        void dissmissProgressDialog();
    }
}
