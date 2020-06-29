package org.smartregister.child.sample.job;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

import org.smartregister.growthmonitoring.job.HeightIntentServiceJob;
import org.smartregister.growthmonitoring.job.WeightIntentServiceJob;
import org.smartregister.growthmonitoring.job.ZScoreRefreshIntentServiceJob;
import org.smartregister.job.ExtendedSyncServiceJob;
import org.smartregister.job.PullUniqueIdsServiceJob;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.job.ValidateSyncDataServiceJob;
import org.smartregister.sync.intent.SyncIntentService;

/**
 * Created by ndegwamartin on 05/09/2018.
 */
public class SampleJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case SyncServiceJob.TAG:
                return new SyncServiceJob(SyncIntentService.class);
            case ExtendedSyncServiceJob.TAG:
                return new ExtendedSyncServiceJob();
            case PullUniqueIdsServiceJob.TAG:
                return new PullUniqueIdsServiceJob();
            case ValidateSyncDataServiceJob.TAG:
                return new ValidateSyncDataServiceJob();
            case HeightIntentServiceJob.TAG:
                return new HeightIntentServiceJob();
            case WeightIntentServiceJob.TAG:
                return new WeightIntentServiceJob();
            case ZScoreRefreshIntentServiceJob.TAG:
                return new ZScoreRefreshIntentServiceJob();
            default:
                Log.d(SampleJobCreator.class.getCanonicalName(),
                        "Looks like you tried to create a job " + tag + " that is not declared in the Anc Job Creator");
                return null;
        }
    }
}
