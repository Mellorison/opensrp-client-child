package org.smartregister.child.service.intent;

import android.content.Intent;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.child.util.Utils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.sync.intent.BaseSyncIntentService;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public abstract class ArchiveClientRecordIntentService extends BaseSyncIntentService {

    private static final String TAG = "ArchiveClientRecordIntentService";

    public ArchiveClientRecordIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        archiveClients();
    }

    /**
     * This method will archive every client record with the IDs matching the ones provided
     */
    protected void archiveClients() {
        List<String> clientIdsToArchive = getClientIdsToArchive();
        try {
            List<String> formSubmissionIds = new ArrayList<>();
            List<Event> archiveRecordEvents = Utils.createArchiveRecordEvents(clientIdsToArchive);
            for (Event archiveRecordEvent : archiveRecordEvents) {
                if (StringUtils.isNotBlank(archiveRecordEvent.getBaseEntityId()))
                    formSubmissionIds.add(archiveRecordEvent.getFormSubmissionId());
            }
            if (!formSubmissionIds.isEmpty()) {
                Utils.initiateEventProcessing(formSubmissionIds);
                onArchiveDone();
            }

        } catch (Exception e) {
            Timber.e(e, "Error Archiving %d Records", clientIdsToArchive.size());
        }
    }

    /**
     * Provide your custom implementation of obtaining base entity ids of the clients that you want
     * to archive their record.
     *
     * @return a list of client base entity ids
     */
    protected abstract List<String> getClientIdsToArchive();

    /**
     * This method is called when archiving records is completed
     */
    protected abstract void onArchiveDone();
}
