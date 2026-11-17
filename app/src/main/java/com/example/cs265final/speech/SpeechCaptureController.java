package com.example.cs265final.speech;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Coordinates Android SpeechRecognizer sessions, optional AudioRecord PCM capture on API 33+,
 * and legacy speech activity fallback.
 */
public final class SpeechCaptureController {
  private static final String TAG = "Voice2NoteSpeech";
  private static final long SPEECH_TIMEOUT_MS = 15_000L;

  /** UI callbacks emitted on the main thread. */
  public interface Listener {
    void onTranscriptChanged(@NonNull String transcript);

    void onListeningChanged(boolean listening);

    void onMessage(@NonNull String message);
  }

  private final Context appContext;
  private final RecordingDraftViewModel draftViewModel;
  private final Listener listener;
  private final SpeechDraft speechDraft = new SpeechDraft();
  private final Handler handler = new Handler(Looper.getMainLooper());

  @Nullable private SpeechRecognizer recognizer;
  @Nullable private Runnable speechTimeout;
  @Nullable private Integer currentSpeechSession;
  private boolean speechFinishing;
  private boolean listening;
  private boolean gotSpeechInSession;

  @Nullable private AtomicBoolean audioRecordActive;
  @Nullable private AudioRecord audioRecord;
  @Nullable private Thread audioCaptureThread;
  @Nullable private ParcelFileDescriptor audioPipeRead;
  @Nullable private ParcelFileDescriptor audioPipeWrite;
  @Nullable private ByteArrayOutputStream sessionAudioBuffer;
  @Nullable private ByteArrayOutputStream feedbackAudioBuffer;

  public SpeechCaptureController(
      @NonNull Context context,
      @NonNull RecordingDraftViewModel draftViewModel,
      @NonNull Listener listener) {
    this.appContext = context.getApplicationContext();
    this.draftViewModel = draftViewModel;
    this.listener = listener;
  }

  public boolean isListening() {
    return listening;
  }

  /**
   * Starts speech capture. On API &lt; 33, launches the system speech activity via
   * {@code legacyLauncher} when available.
   */
  @SuppressLint("NewApi")
  public void startSpeech(
      @NonNull String currentTranscript, @NonNull Consumer<Intent> legacyLauncher) {
    boolean usePipe = Build.VERSION.SDK_INT >= 33;
    if (!usePipe) {
      Intent legacyIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      legacyIntent.putExtra(
          RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      legacyIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
      legacyIntent.putExtra(
          RecognizerIntent.EXTRA_PROMPT, appContext.getString(R.string.speech_legacy_prompt));
      legacyIntent.putExtra("android.speech.extra.GET_AUDIO", true);
      legacyIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
      if (legacyIntent.resolveActivity(appContext.getPackageManager()) != null) {
        if (draftViewModel.getAudioBuffer() == null) {
          draftViewModel.setAudioBuffer(new ByteArrayOutputStream());
        }
        listener.onMessage("");
        setListening(true);
        legacyLauncher.accept(legacyIntent);
        return;
      }
    }

    if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
      listener.onMessage(appContext.getString(R.string.speech_unavailable));
      return;
    }

    int session = speechDraft.start(currentTranscript);
    currentSpeechSession = session;
    speechFinishing = false;
    gotSpeechInSession = false;
    if (draftViewModel.getAudioBuffer() == null) {
      draftViewModel.setAudioBuffer(new ByteArrayOutputStream());
    }
    ByteArrayOutputStream sessionBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream feedbackBuf = new ByteArrayOutputStream();
    sessionAudioBuffer = sessionBuf;
    feedbackAudioBuffer = feedbackBuf;

    ParcelFileDescriptor readPipe = null;
    if (usePipe) {
      try {
        readPipe = startAudioCapture(sessionBuf);
      } catch (RuntimeException e) {
        stopAudioCapture();
        readPipe = null;
      }
    }
    final ParcelFileDescriptor activePipe = readPipe;
    armSpeechTimeout(session);
    setListening(true);
    listener.onMessage("");

    try {
      if (recognizer == null) {
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
      }
      recognizer.setRecognitionListener(
          new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
              if (speechDraft.isCurrent(session)) {
                setListening(true);
                if (!speechFinishing) {
                  clearSpeechTimeout();
                }
              }
            }

            @Override
            public void onBeginningOfSpeech() {
              if (speechDraft.isCurrent(session)) {
                gotSpeechInSession = true;
                if (!speechFinishing) {
                  clearSpeechTimeout();
                }
              }
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {
              if (speechDraft.isCurrent(session)
                  && activePipe == null
                  && buffer != null
                  && buffer.length > 0) {
                synchronized (feedbackBuf) {
                  feedbackBuf.write(buffer, 0, buffer.length);
                }
              }
            }

            @Override
            public void onEndOfSpeech() {
              if (speechDraft.isCurrent(session)) {
                speechFinishing = true;
                stopAudioCapture();
                flushSessionAudio();
                armSpeechTimeout(session);
              }
            }

            @Override
            public void onError(int error) {
              if (speechDraft.isCurrent(session)) {
                stopAudioCapture();
                Log.w(TAG, "Recognition error code: " + error + " gotSpeech=" + gotSpeechInSession);
                if (gotSpeechInSession) {
                  flushSessionAudio();
                } else {
                  sessionAudioBuffer = null;
                  feedbackAudioBuffer = null;
                }
                setListening(false);
                clearSpeechTimeout();
                currentSpeechSession = null;
                speechDraft.cancel();
                listener.onMessage(appContext.getString(R.string.speech_error_retry));
              }
            }

            @Override
            public void onResults(Bundle results) {
              if (speechDraft.isCurrent(session)) {
                stopAudioCapture();
                flushSessionAudio();
                setListening(false);
                clearSpeechTimeout();
                currentSpeechSession = null;
                updateText(results);
                speechDraft.cancel();
              }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
              updateText(partialResults);
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}

            private void updateText(@Nullable Bundle bundle) {
              if (!speechDraft.isCurrent(session) || bundle == null) {
                return;
              }
              List<String> items =
                  bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
              if (items != null && !items.isEmpty() && items.get(0) != null) {
                String updated = speechDraft.result(session, items.get(0));
                if (updated != null) {
                  gotSpeechInSession = true;
                  listener.onTranscriptChanged(updated);
                }
              }
            }
          });

      Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      intent.putExtra(
          RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH");
      intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
      if (readPipe != null) {
        intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readPipe);
        intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1);
        intent.putExtra(
            RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
        intent.putExtra(
            RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, AudioUtils.DEFAULT_SAMPLE_RATE);
      }
      recognizer.startListening(intent);
    } catch (RuntimeException e) {
      cancelSpeech();
      listener.onMessage(appContext.getString(R.string.speech_start_failed));
    }
  }

  public void handleLegacyResult(
      int resultCode, @Nullable Intent data, @NonNull String currentTranscript) {
    setListening(false);
    if (resultCode != Activity.RESULT_OK) {
      if (resultCode != Activity.RESULT_CANCELED) {
        listener.onMessage(appContext.getString(R.string.speech_error_retry));
      }
      return;
    }
    if (data == null) {
      return;
    }
    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    String text =
        (results != null && !results.isEmpty() && results.get(0) != null)
            ? results.get(0).trim()
            : "";
    if (!text.isEmpty()) {
      int session = speechDraft.start(currentTranscript);
      String merged = speechDraft.result(session, text);
      if (merged != null) {
        listener.onTranscriptChanged(merged);
      }
      speechDraft.cancel();
    }
    Uri uri = data.getData();
    Log.i(TAG, "Legacy activity result textLen=" + text.length() + " audioUri=" + uri);
    if (uri != null) {
      byte[] pcm = AudioUtils.decodeAudioUriToPcm16k(appContext, uri);
      if (pcm != null && AudioUtils.hasNonZeroPcm(pcm)) {
        ByteArrayOutputStream dst = draftViewModel.getAudioBuffer();
        if (dst == null) {
          dst = new ByteArrayOutputStream();
          draftViewModel.setAudioBuffer(dst);
        }
        synchronized (dst) {
          dst.write(pcm, 0, pcm.length);
        }
        Log.i(TAG, "Decoded legacy audio URI to " + pcm.length + " PCM bytes");
      }
    }
  }

  public void stopSpeech() {
    if (speechFinishing) {
      return;
    }
    speechFinishing = true;
    stopAudioCapture();
    flushSessionAudio();
    try {
      if (recognizer != null) {
        recognizer.stopListening();
      }
      if (currentSpeechSession != null) {
        armSpeechTimeout(currentSpeechSession);
      }
    } catch (RuntimeException e) {
      cancelSpeech();
      listener.onMessage(appContext.getString(R.string.speech_stop_failed));
    }
  }

  public void cancelSpeech() {
    stopAudioCapture();
    sessionAudioBuffer = null;
    feedbackAudioBuffer = null;
    clearSpeechTimeout();
    currentSpeechSession = null;
    speechFinishing = false;
    speechDraft.cancel();
    if (recognizer != null) {
      try {
        recognizer.cancel();
      } catch (RuntimeException ignored) {
        // Best-effort cleanup.
      }
      try {
        recognizer.destroy();
      } catch (RuntimeException ignored) {
        // Best-effort cleanup.
      }
      recognizer = null;
    }
    setListening(false);
  }

  public void flushSessionAudio() {
    byte[] recorded = extractNonZeroBytes(sessionAudioBuffer);
    byte[] feedback = extractNonZeroBytes(feedbackAudioBuffer);
    byte[] chosen = recorded != null ? recorded : feedback;
    if (chosen != null) {
      ByteArrayOutputStream dst = draftViewModel.getAudioBuffer();
      if (dst == null) {
        dst = new ByteArrayOutputStream();
        draftViewModel.setAudioBuffer(dst);
      }
      synchronized (dst) {
        dst.write(chosen, 0, chosen.length);
      }
      Log.i(
          TAG,
          "Captured "
              + chosen.length
              + " PCM bytes (source="
              + (recorded != null ? "AudioRecord" : "onBufferReceived")
              + ")");
    }
    sessionAudioBuffer = null;
    feedbackAudioBuffer = null;
  }

  public void stopAudioCapture() {
    if (audioRecordActive != null) {
      audioRecordActive.set(false);
      audioRecordActive = null;
    }
    if (audioRecord != null) {
      try {
        audioRecord.stop();
      } catch (RuntimeException ignored) {
        // Best-effort stop.
      }
      audioRecord = null;
    }
    if (audioCaptureThread != null && audioCaptureThread != Thread.currentThread()) {
      try {
        audioCaptureThread.join(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    audioCaptureThread = null;
    closeQuietly(audioPipeWrite);
    audioPipeWrite = null;
    closeQuietly(audioPipeRead);
    audioPipeRead = null;
  }

  public void release() {
    cancelSpeech();
  }

  @SuppressLint("MissingPermission")
  @Nullable
  private ParcelFileDescriptor startAudioCapture(@Nullable ByteArrayOutputStream targetBuffer) {
    stopAudioCapture();
    int minBuf =
        AudioRecord.getMinBufferSize(
            AudioUtils.DEFAULT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    if (minBuf <= 0) {
      return null;
    }
    AudioRecord rec =
        new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            AudioUtils.DEFAULT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            Math.max(minBuf * 2, 6_400));
    if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
      rec.release();
      return null;
    }
    ParcelFileDescriptor[] pipe;
    try {
      pipe = ParcelFileDescriptor.createPipe();
    } catch (IOException e) {
      rec.release();
      return null;
    }
    ParcelFileDescriptor readFd = pipe[0];
    ParcelFileDescriptor writeFd = pipe[1];
    audioPipeRead = readFd;
    audioPipeWrite = writeFd;
    audioRecord = rec;
    AtomicBoolean active = new AtomicBoolean(true);
    audioRecordActive = active;
    try {
      rec.startRecording();
    } catch (RuntimeException e) {
      stopAudioCapture();
      rec.release();
      return null;
    }

    Thread thread =
        new Thread(
            () -> {
              byte[] chunk = new byte[3_200];
              ParcelFileDescriptor.AutoCloseOutputStream out =
                  new ParcelFileDescriptor.AutoCloseOutputStream(writeFd);
              try {
                while (active.get()) {
                  int n = rec.read(chunk, 0, chunk.length);
                  if (n < 0) {
                    break;
                  }
                  if (n > 0) {
                    if (targetBuffer != null) {
                      synchronized (targetBuffer) {
                        targetBuffer.write(chunk, 0, n);
                      }
                    }
                    if (out != null) {
                      try {
                        out.write(chunk, 0, n);
                        out.flush();
                      } catch (IOException e) {
                        closeQuietly(out);
                        out = null;
                      }
                    }
                  }
                }
              } catch (Exception ignored) {
                // Stopping a capture can close the pipe while the reader is active.
              } finally {
                closeQuietly(out);
                try {
                  rec.stop();
                } catch (RuntimeException ignored) {
                  // Already stopped.
                }
                rec.release();
              }
            },
            "Voice2NoteMicTee");
    thread.setDaemon(true);
    thread.start();
    audioCaptureThread = thread;
    return readFd;
  }

  private void clearSpeechTimeout() {
    if (speechTimeout != null) {
      handler.removeCallbacks(speechTimeout);
      speechTimeout = null;
    }
  }

  private void armSpeechTimeout(int session) {
    clearSpeechTimeout();
    speechTimeout =
        () -> {
          if (speechDraft.isCurrent(session)) {
            cancelSpeech();
            listener.onMessage(appContext.getString(R.string.speech_timeout));
          }
        };
    handler.postDelayed(speechTimeout, SPEECH_TIMEOUT_MS);
  }

  private void setListening(boolean value) {
    listening = value;
    listener.onListeningChanged(value);
  }

  @Nullable
  private static byte[] extractNonZeroBytes(@Nullable ByteArrayOutputStream buffer) {
    if (buffer == null) {
      return null;
    }
    byte[] bytes;
    synchronized (buffer) {
      bytes = buffer.toByteArray();
    }
    return AudioUtils.hasNonZeroPcm(bytes) ? bytes : null;
  }

  private static void closeQuietly(@Nullable java.io.Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) {
        // Best-effort close.
      }
    }
  }
}
