package org.smartregister.chw.hf.job;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.evernote.android.job.v14.JobProxy14;

/**
 * Wraps {@link JobProxy14} to avoid passing null PendingIntents to {@link AlarmManager#cancel}
 * and to attach immutable flags required on newer Android versions.
 */
public class HfJobProxy14 extends JobProxy14 {

    public HfJobProxy14(Context context) {
        super(context);
    }

    @Override
    public void cancel(int jobId) {
        AlarmManager alarmManager = getAlarmManager();
        if (alarmManager != null) {
            try {
                PendingIntent repeatingIntent = getPendingIntent(jobId, false, null, createPendingIntentFlags(true));
                if (repeatingIntent != null) {
                    alarmManager.cancel(repeatingIntent);
                }

                PendingIntent oneOffIntent = getPendingIntent(jobId, false, null, createPendingIntentFlags(false));
                if (oneOffIntent != null) {
                    alarmManager.cancel(oneOffIntent);
                }
            } catch (Exception e) {
                mCat.e(e);
            }
        }
    }

    @Override
    protected PendingIntent getPendingIntent(int jobId, boolean exact, @Nullable Bundle transientExtras, int flags) {
        int safeFlags = flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            safeFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return super.getPendingIntent(jobId, exact, transientExtras, safeFlags);
    }
}
