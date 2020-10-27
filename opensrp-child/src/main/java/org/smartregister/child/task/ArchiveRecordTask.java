package org.smartregister.child.task;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import org.smartregister.child.R;
import org.smartregister.child.util.Constants;
import org.smartregister.child.util.Utils;
import org.smartregister.clientandeventmodel.Event;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;

import timber.log.Timber;

public class ArchiveRecordTask extends AsyncTask<Void, Void, Void> {

    private WeakReference<Activity> activityWeakReference;
    private Map<String, String> details;
    private ProgressDialog progressDialog;

    public ArchiveRecordTask(@NonNull Activity activity, @NonNull Map<String, String> details) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.details = details;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(activityWeakReference.get());
        progressDialog.setCancelable(false);
        progressDialog.setMessage(activityWeakReference.get().getString(R.string.child_archive_record_task_dialog_message));
        progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            String baseEntityId = details.get(Constants.KEY.BASE_ENTITY_ID);
            Event archiveRecordEvent;
            if (baseEntityId != null) {
                archiveRecordEvent = Utils.createArchiveRecordEvent(baseEntityId);
                Utils.initiateEventProcessing(Collections.singletonList(archiveRecordEvent.getFormSubmissionId()));
            }

        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        progressDialog.dismiss();
        Activity activity = activityWeakReference.get();
        Intent intent = new Intent(activity, Utils.metadata().getChildRegisterActivity());// update with the register
        activity.startActivity(intent);
        activity.finish();
    }
}
