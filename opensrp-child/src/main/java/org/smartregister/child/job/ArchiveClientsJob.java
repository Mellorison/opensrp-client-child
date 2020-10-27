package org.smartregister.child.job;

import android.content.Intent;
import androidx.annotation.NonNull;

import com.evernote.android.job.DailyJob;
import com.evernote.android.job.JobRequest;

import org.smartregister.child.service.intent.ArchiveClientRecordIntentService;

import java.util.concurrent.TimeUnit;

public class ArchiveClientsJob extends DailyJob {

    public static final String TAG = "ArchiveClientsJob";

    private Class<? extends ArchiveClientRecordIntentService> intentServiceClass;
    private static JobRequest.Builder jobBuilder = new JobRequest.Builder(TAG).setUpdateCurrent(true);

    public ArchiveClientsJob(Class<? extends ArchiveClientRecordIntentService> intentServiceClass) {
        this.intentServiceClass = intentServiceClass;
    }

    public static void scheduleDaily() {
        DailyJob.schedule(jobBuilder, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(6));
    }

    public static void runAtOnce() {
        startNowOnce(jobBuilder);
    }

    @NonNull
    @Override
    protected DailyJobResult onRunDailyJob(@NonNull Params params) {
        Intent intent = new Intent(getContext(), intentServiceClass);
        getContext().startService(intent);
        return DailyJobResult.SUCCESS;
    }
}
