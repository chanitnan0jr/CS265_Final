package com.example.cs265final.transcription;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.cs265final.data.Note;
import java.util.concurrent.TimeUnit;

/** Enqueues and cancels background WorkManager transcription jobs. */
public final class TranscriptionScheduler {
  private TranscriptionScheduler() {}

  public static void enqueue(@NonNull Context context, @NonNull Note note) {
    String jobId = note.getJobId();
    if (jobId.trim().isEmpty()) {
      return;
    }
    Constraints constraints =
        new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    Data input = new Data.Builder().putString(TranscriptionWorker.KEY_JOB_ID, jobId).build();
    OneTimeWorkRequest request =
        new OneTimeWorkRequest.Builder(TranscriptionWorker.class)
            .setConstraints(constraints)
            .setInputData(input)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build();
    WorkManager.getInstance(context)
        .enqueueUniqueWork(jobId, ExistingWorkPolicy.REPLACE, request);
  }

  public static void cancel(@NonNull Context context, @NonNull String jobId) {
    if (!jobId.trim().isEmpty()) {
      WorkManager.getInstance(context).cancelUniqueWork(jobId);
    }
  }
}
