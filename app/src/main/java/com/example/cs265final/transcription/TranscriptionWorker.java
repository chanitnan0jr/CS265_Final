package com.example.cs265final.transcription;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.cs265final.BuildConfig;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioFiles;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.NoteStorage;
import com.example.cs265final.data.TranscriptionStatus;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;

/** Uploads an audio note and polls the transcription API; updates the stored note in place. */
public final class TranscriptionWorker extends Worker {
  public static final String KEY_JOB_ID = "job_id";
  private static final String TAG = "Voice2NoteTranscription";
  private static final int MAX_ATTEMPTS = 3;
  private static final long POLL_INTERVAL_MS = 2_000L;
  private static final long MAX_POLL_MS = 5 * 60_000L;

  public TranscriptionWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
    super(appContext, params);
  }

  @NonNull
  @Override
  public Result doWork() {
    String jobId = getInputData().getString(KEY_JOB_ID);
    if (jobId == null || jobId.trim().isEmpty()) {
      return Result.failure();
    }
    Context context = getApplicationContext();
    NoteStorage storage = new NoteStorage(context);
    Note note;
    try {
      note = storage.findNote(jobId);
    } catch (JSONException e) {
      note = null;
    }
    if (note == null || !note.getTranscription().isPending()) {
      return Result.success();
    }
    if (note.getAudioUri().trim().isEmpty()) {
      return fail(storage, jobId, context.getString(R.string.transcription_error_missing_audio));
    }

    String baseUrl = BuildConfig.TRANSCRIPTION_BASE_URL.trim();
    if (baseUrl.isEmpty()) {
      return fail(storage, jobId, context.getString(R.string.transcription_error_no_server));
    }

    boolean marked;
    try {
      marked =
          storage.updateTranscription(
              jobId,
              current ->
                  current.withTranscription(
                      current.getBody(), jobId, TranscriptionStatus.PROCESSING, ""));
    } catch (JSONException e) {
      marked = false;
    }
    if (!marked) {
      return Result.success();
    }

    try {
      TranscriptionResponse response = transcribe(context, baseUrl, note.getAudioUri());
      if (response.getText().trim().isEmpty()) {
        return fail(storage, jobId, context.getString(R.string.transcription_error_empty_result));
      }
      boolean saved;
      try {
        saved =
            storage.updateTranscription(
                jobId,
                current ->
                    current.withTranscription(
                        TranscriptionClient.mergeTranscript(
                            current.getBody(), response.getText()),
                        jobId,
                        TranscriptionStatus.COMPLETED,
                        ""));
      } catch (JSONException e) {
        saved = false;
      }
      if (saved) {
        return Result.success();
      }
      if (getRunAttemptCount() < MAX_ATTEMPTS) {
        return Result.retry();
      }
      return fail(storage, jobId, context.getString(R.string.transcription_error_save_failed));
    } catch (TranscriptionException e) {
      Log.w(TAG, "Transcription failed (retryable=" + e.getRetryable() + ")");
      if (e.getRetryable() && getRunAttemptCount() < MAX_ATTEMPTS) {
        return Result.retry();
      }
      String msg = e.getMessage();
      return fail(
          storage,
          jobId,
          (msg != null && !msg.trim().isEmpty())
              ? msg
              : context.getString(R.string.transcription_error_generic));
    } catch (SecurityException e) {
      Log.w(TAG, "Transcription cannot read the audio file");
      return fail(storage, jobId, context.getString(R.string.transcription_error_permission));
    } catch (IOException e) {
      Log.w(TAG, "Transcription network error");
      if (getRunAttemptCount() < MAX_ATTEMPTS) {
        return Result.retry();
      }
      return fail(storage, jobId, context.getString(R.string.transcription_error_network));
    } catch (Exception e) {
      Log.w(TAG, "Transcription error: " + e.getClass().getSimpleName());
      return fail(storage, jobId, context.getString(R.string.transcription_error_generic));
    }
  }

  private TranscriptionResponse transcribe(Context context, String baseUrl, String audioUri)
      throws IOException, TranscriptionException {
    Uri uri = Uri.parse(audioUri);
    ContentResolver resolver = context.getContentResolver();
    String displayName = AudioFiles.contentDisplayName(resolver, uri);
    String fileName = displayName != null ? displayName : "audio";
    String resolvedType = resolver.getType(uri);
    String mimeType =
        resolvedType != null ? resolvedType : TranscriptionClient.mimeTypeFor(fileName);

    String remoteJob;
    try (InputStream input = resolver.openInputStream(uri)) {
      if (input == null) {
        throw new TranscriptionException(
            context.getString(R.string.transcription_error_open_file), false);
      }
      remoteJob = TranscriptionClient.submit(baseUrl, fileName, mimeType, "th", input);
    } catch (FileNotFoundException e) {
      throw new TranscriptionException(
          context.getString(R.string.transcription_error_missing_audio), false);
    } catch (SecurityException e) {
      throw new TranscriptionException(
          context.getString(R.string.transcription_error_permission), false);
    }

    long waited = 0L;
    while (waited <= MAX_POLL_MS) {
      if (isStopped()) {
        throw new TranscriptionException(
            context.getString(R.string.transcription_error_stopped), true);
      }
      TranscriptionResponse response = TranscriptionClient.poll(baseUrl, remoteJob);
      String status = response.getStatus();
      if ("completed".equals(status)) {
        return response;
      }
      if ("failed".equals(status)) {
        String err = response.getError().trim();
        throw new TranscriptionException(
            err.isEmpty() ? context.getString(R.string.transcription_error_generic) : err,
            false);
      }
      if (!"queued".equals(status) && !"processing".equals(status)) {
        throw new TranscriptionException(
            context.getString(R.string.transcription_error_bad_status), false);
      }
      try {
        Thread.sleep(POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new TranscriptionException(
            context.getString(R.string.transcription_error_stopped), true);
      }
      waited += POLL_INTERVAL_MS;
    }
    throw new TranscriptionException(
        context.getString(R.string.transcription_error_timeout), false);
  }

  private Result fail(NoteStorage storage, String jobId, String message) {
    try {
      storage.updateTranscription(
          jobId,
          current ->
              current.withTranscription(
                  current.getBody(), jobId, TranscriptionStatus.FAILED, message));
    } catch (JSONException ignored) {
      // Best-effort status update.
    }
    return Result.failure();
  }
}
