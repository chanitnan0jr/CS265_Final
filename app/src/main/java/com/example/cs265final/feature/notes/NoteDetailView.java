package com.example.cs265final.feature.notes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioUtils;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.TranscriptionStatus;
import com.example.cs265final.feature.playback.PlaybackHelper;
import com.example.cs265final.ui.UiLabels;
import com.example.cs265final.ui.WaveformView;
import java.io.File;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Date;
import java.util.function.Function;

/** Note detail screen with inline rename, folder assignment, audio playback, copy, and share. */
public final class NoteDetailView extends LinearLayout {
  private static final long TIMER_POLL_MS = 250L;
  private static final long COPIED_RESET_MS = 1_500L;

  private final View optionsButton;
  private final View renameContainer;
  private final EditText renameInput;
  private final TextView renameErrorView;
  private final TextView titleView;
  private final TextView dateView;
  private final TextView moodView;
  private final TextView folderView;
  private final View transcriptionCard;
  private final TextView transcriptionStatusView;
  private final View transcriptionProgress;
  private final TextView transcriptionErrorView;
  private final Button retryButton;
  private final Button smallPlayButton;
  private final WaveformView waveformView;
  private final TextView timerView;
  private final View bodyCard;
  private final TextView bodyView;
  private final Button listenButton;
  private final Button copyButton;

  private final Handler handler = new Handler(Looper.getMainLooper());

  @Nullable private Note boundNote;
  @Nullable private Uri playableUri;
  @Nullable private MediaPlayer mediaPlayer;
  @Nullable private Lifecycle observedLifecycle;
  private boolean observerAttached;
  private boolean mediaReady;
  private boolean mediaError;
  private boolean playing;
  private int elapsedSeconds;
  private int durationSeconds;
  private boolean renaming;

  private Function<String, Boolean> onRename;
  private Runnable onAttachAudio;
  private Runnable onMoveFolder;
  private Runnable onDelete;
  private Runnable onRetryTranscription;
  private Runnable onBack;

  private final LifecycleEventObserver lifecycleObserver =
      (LifecycleOwner source, Lifecycle.Event event) -> {
        if (event == Lifecycle.Event.ON_STOP) {
          pauseForBackground();
        }
      };

  private final Runnable timerTicker =
      new Runnable() {
        @Override
        public void run() {
          if (playing && mediaPlayer != null && mediaReady) {
            try {
              elapsedSeconds = Math.max(0, mediaPlayer.getCurrentPosition() / 1000);
            } catch (IllegalStateException ignored) {
              // Ignore transient player state.
            }
            updateAudioUi();
            handler.postDelayed(this, TIMER_POLL_MS);
          }
        }
      };

  private final Runnable resetCopiedLabel;

  public NoteDetailView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_note_detail, this, true);

    optionsButton = findViewById(R.id.button_detail_options);
    renameContainer = findViewById(R.id.detail_rename_container);
    renameInput = findViewById(R.id.input_detail_rename);
    renameErrorView = findViewById(R.id.text_detail_rename_error);
    titleView = findViewById(R.id.text_detail_title);
    dateView = findViewById(R.id.text_detail_date);
    moodView = findViewById(R.id.text_detail_mood);
    folderView = findViewById(R.id.text_detail_folder);
    transcriptionCard = findViewById(R.id.detail_transcription_card);
    transcriptionStatusView = findViewById(R.id.text_detail_transcription_status);
    transcriptionProgress = findViewById(R.id.progress_detail_transcription);
    transcriptionErrorView = findViewById(R.id.text_detail_transcription_error);
    retryButton = findViewById(R.id.button_detail_retry);
    smallPlayButton = findViewById(R.id.button_detail_play_small);
    waveformView = findViewById(R.id.waveform_detail);
    timerView = findViewById(R.id.text_detail_timer);
    bodyCard = findViewById(R.id.detail_body_card);
    bodyView = findViewById(R.id.text_detail_body);
    listenButton = findViewById(R.id.button_detail_listen);
    copyButton = findViewById(R.id.button_detail_copy);
    resetCopiedLabel = () -> copyButton.setText(R.string.detail_copy_text);

    findViewById(R.id.button_detail_back)
        .setOnClickListener(
            view -> {
              if (onBack != null) {
                onBack.run();
              }
            });

    optionsButton.setOnClickListener(view -> showOptionsMenu());
    findViewById(R.id.button_detail_edit).setOnClickListener(view -> startRename());
    findViewById(R.id.button_detail_rename_cancel).setOnClickListener(view -> dismissRename());
    findViewById(R.id.button_detail_rename_save).setOnClickListener(view -> submitRename());

    retryButton.setOnClickListener(
        view -> {
          if (onRetryTranscription != null) {
            onRetryTranscription.run();
          }
        });

    View.OnClickListener playClick = view -> togglePlayback();
    findViewById(R.id.detail_audio_bar).setOnClickListener(playClick);
    smallPlayButton.setOnClickListener(playClick);
    listenButton.setOnClickListener(playClick);

    copyButton.setOnClickListener(
        view -> {
          if (boundNote == null) {
            return;
          }
          ClipboardManager cm = getContext().getSystemService(ClipboardManager.class);
          if (cm != null) {
            cm.setPrimaryClip(
                ClipData.newPlainText(boundNote.getTitle(), boundNote.getBody()));
          }
          copyButton.setText(R.string.detail_copied);
          handler.removeCallbacks(resetCopiedLabel);
          handler.postDelayed(resetCopiedLabel, COPIED_RESET_MS);
        });

    findViewById(R.id.button_detail_share)
        .setOnClickListener(
            view -> {
              if (boundNote == null) {
                return;
              }
              Intent share = new Intent(Intent.ACTION_SEND);
              share.setType("text/plain");
              share.putExtra(Intent.EXTRA_SUBJECT, boundNote.getTitle());
              share.putExtra(
                  Intent.EXTRA_TEXT, boundNote.getTitle() + "\n\n" + boundNote.getBody());
              getContext().startActivity(Intent.createChooser(share, null));
            });

    folderView.setOnClickListener(
        view -> {
          if (onMoveFolder != null) {
            onMoveFolder.run();
          }
        });
  }

  public void setOnRename(Function<String, Boolean> action) {
    onRename = action;
  }

  public void setOnAttachAudio(Runnable action) {
    onAttachAudio = action;
  }

  public void setOnMoveFolder(Runnable action) {
    onMoveFolder = action;
  }

  public void setOnDelete(Runnable action) {
    onDelete = action;
  }

  public void setOnRetryTranscription(Runnable action) {
    onRetryTranscription = action;
  }

  public void setOnBack(Runnable action) {
    onBack = action;
  }

  public boolean isRenaming() {
    return renaming;
  }

  public void dismissRename() {
    renaming = false;
    renameErrorView.setVisibility(GONE);
    renameContainer.setVisibility(GONE);
    titleView.setVisibility(VISIBLE);
  }

  public void bindLifecycle(@Nullable Lifecycle lifecycle) {
    if (observedLifecycle == lifecycle) {
      if (observedLifecycle != null && !observerAttached && isAttachedToWindow()) {
        observedLifecycle.addObserver(lifecycleObserver);
        observerAttached = true;
      }
      return;
    }
    detachLifecycleObserver();
    observedLifecycle = lifecycle;
    if (observedLifecycle != null) {
      observedLifecycle.addObserver(lifecycleObserver);
      observerAttached = true;
    }
  }

  private void detachLifecycleObserver() {
    if (observedLifecycle != null && observerAttached) {
      observedLifecycle.removeObserver(lifecycleObserver);
      observerAttached = false;
    }
  }

  public void bind(@NonNull Note note, @NonNull DateFormat dateFormat) {
    boolean noteChanged =
        boundNote == null
            || !note.getId().equals(boundNote.getId())
            || !note.getAudioUri().equals(boundNote.getAudioUri());
    this.boundNote = note;
    if (noteChanged && renaming) {
      dismissRename();
    }

    titleView.setText(note.getTitle());
    dateView.setText(dateFormat.format(new Date(note.getTime())));
    moodView.setText(UiLabels.moodLabel(getContext(), note.getMood()));
    folderView.setText(UiLabels.folderLabel(getContext(), note.getFolder()));

    if (note.getBody().trim().isEmpty()) {
      bodyCard.setVisibility(GONE);
    } else {
      bodyView.setText(note.getBody());
      bodyCard.setVisibility(VISIBLE);
    }

    bindTranscriptionStatus(note);

    Uri resolvedUri = resolvePlayableUri(note.getAudioUri());
    if (resolvedUri == null && playableUri != null
        || (resolvedUri != null && !resolvedUri.equals(playableUri))) {
      setupMediaPlayer(resolvedUri);
    } else {
      updateAudioUi();
    }
  }

  public void pauseForBackground() {
    if (mediaPlayer != null && mediaReady && playing) {
      try {
        elapsedSeconds = Math.max(0, mediaPlayer.getCurrentPosition() / 1000);
        mediaPlayer.pause();
        playing = false;
      } catch (RuntimeException e) {
        mediaReady = false;
        mediaError = true;
        playing = false;
      }
      handler.removeCallbacks(timerTicker);
      updateAudioUi();
    }
  }

  public void releaseMedia() {
    handler.removeCallbacks(timerTicker);
    handler.removeCallbacks(resetCopiedLabel);
    playing = false;
    mediaReady = false;
    if (mediaPlayer != null) {
      try {
        mediaPlayer.release();
      } catch (RuntimeException ignored) {
        // Best-effort release.
      }
      mediaPlayer = null;
    }
    playableUri = null;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (observedLifecycle != null && !observerAttached) {
      observedLifecycle.addObserver(lifecycleObserver);
      observerAttached = true;
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    detachLifecycleObserver();
    releaseMedia();
  }

  private void startRename() {
    if (boundNote == null) {
      return;
    }
    renaming = true;
    renameInput.setText(boundNote.getTitle());
    renameInput.setSelection(renameInput.length());
    renameErrorView.setVisibility(GONE);
    renameContainer.setVisibility(VISIBLE);
    titleView.setVisibility(GONE);
  }

  private void submitRename() {
    String trimmed = renameInput.getText().toString().trim();
    if (trimmed.isEmpty()) {
      renameErrorView.setText(R.string.detail_rename_empty_error);
      renameErrorView.setVisibility(VISIBLE);
      return;
    }
    if (onRename != null && Boolean.TRUE.equals(onRename.apply(trimmed))) {
      dismissRename();
    } else {
      renameErrorView.setText(R.string.detail_rename_failed);
      renameErrorView.setVisibility(VISIBLE);
    }
  }

  private void showOptionsMenu() {
    PopupMenu menu = new PopupMenu(getContext(), optionsButton);
    menu.getMenu()
        .add(R.string.detail_rename_action)
        .setOnMenuItemClickListener(
            item -> {
              startRename();
              return true;
            });
    if (onMoveFolder != null) {
      menu.getMenu()
          .add(R.string.note_move_folder_action)
          .setOnMenuItemClickListener(
              item -> {
                onMoveFolder.run();
                return true;
              });
    }
    if (onDelete != null) {
      menu.getMenu()
          .add(R.string.note_delete_action)
          .setOnMenuItemClickListener(
              item -> {
                onDelete.run();
                return true;
              });
    }
    menu.show();
  }

  private void bindTranscriptionStatus(Note note) {
    TranscriptionStatus status = note.getTranscription();
    if (!status.isPending() && status != TranscriptionStatus.FAILED) {
      transcriptionCard.setVisibility(GONE);
      return;
    }
    transcriptionCard.setVisibility(VISIBLE);
    if (status == TranscriptionStatus.QUEUED) {
      transcriptionStatusView.setText(R.string.detail_status_queued);
      transcriptionStatusView.setTextColor(
          ContextCompat.getColor(getContext(), R.color.text_primary));
      transcriptionProgress.setVisibility(VISIBLE);
      transcriptionErrorView.setVisibility(GONE);
      retryButton.setVisibility(GONE);
    } else if (status == TranscriptionStatus.PROCESSING) {
      transcriptionStatusView.setText(R.string.detail_status_processing);
      transcriptionStatusView.setTextColor(
          ContextCompat.getColor(getContext(), R.color.text_primary));
      transcriptionProgress.setVisibility(VISIBLE);
      transcriptionErrorView.setVisibility(GONE);
      retryButton.setVisibility(GONE);
    } else {
      transcriptionStatusView.setText(R.string.detail_status_failed);
      transcriptionStatusView.setTextColor(
          ContextCompat.getColor(getContext(), R.color.status_error));
      transcriptionProgress.setVisibility(GONE);
      if (note.getTranscriptionError().trim().isEmpty()) {
        transcriptionErrorView.setVisibility(GONE);
      } else {
        transcriptionErrorView.setText(note.getTranscriptionError());
        transcriptionErrorView.setVisibility(VISIBLE);
      }
      retryButton.setVisibility(onRetryTranscription != null ? VISIBLE : GONE);
    }
  }

  private void setupMediaPlayer(@Nullable Uri uri) {
    releaseMedia();
    playableUri = uri;
    mediaReady = false;
    mediaError = false;
    playing = false;
    elapsedSeconds = 0;
    durationSeconds = 0;
    if (uri == null) {
      updateAudioUi();
      return;
    }

    MediaPlayer mp = new MediaPlayer();
    mediaPlayer = mp;
    try {
      mp.setOnPreparedListener(
          prepared -> {
            durationSeconds = Math.max(1, prepared.getDuration() / 1000);
            mediaReady = true;
            mediaError = false;
            updateAudioUi();
          });
      mp.setOnCompletionListener(
          completed -> {
            playing = false;
            elapsedSeconds = 0;
            handler.removeCallbacks(timerTicker);
            completed.seekTo(0);
            updateAudioUi();
          });
      mp.setOnErrorListener(
          (ignored, what, extra) -> {
            mediaReady = false;
            mediaError = true;
            playing = false;
            handler.removeCallbacks(timerTicker);
            updateAudioUi();
            return true;
          });
      PlaybackHelper.setAudioSource(mp, getContext(), uri, PlaybackHelper.DEFAULT_MEDIA_ATTRS);
      mp.prepareAsync();
    } catch (Exception e) {
      mediaReady = false;
      mediaError = true;
    }
    updateAudioUi();
  }

  private void togglePlayback() {
    if (mediaPlayer == null || playableUri == null || mediaError) {
      if (onAttachAudio != null) {
        onAttachAudio.run();
      }
      return;
    }
    if (!mediaReady) {
      return;
    }
    if (playing) {
      mediaPlayer.pause();
      playing = false;
      handler.removeCallbacks(timerTicker);
    } else {
      PlaybackHelper.ensureAudibleMediaOutput(getContext(), PlaybackHelper.DEFAULT_MEDIA_ATTRS);
      mediaPlayer.start();
      playing = true;
      handler.removeCallbacks(timerTicker);
      handler.postDelayed(timerTicker, TIMER_POLL_MS);
    }
    updateAudioUi();
  }

  private void updateAudioUi() {
    int shownSeconds = (playing || elapsedSeconds > 0) ? elapsedSeconds : durationSeconds;
    timerView.setText(PlaybackHelper.formatTime(shownSeconds * 1000));
    waveformView.setActive(playing || mediaReady);
    smallPlayButton.setText(playing ? "❚❚" : "▶");
    if (playableUri == null || mediaError) {
      listenButton.setText(R.string.detail_attach_audio);
    } else if (playing) {
      listenButton.setText(R.string.detail_stop_listening);
    } else {
      listenButton.setText(R.string.detail_listen_original);
    }
  }

  @Nullable
  private static Uri resolvePlayableUri(@NonNull String rawAudioUri) {
    if (rawAudioUri.trim().isEmpty()) {
      return null;
    }
    Uri parsed = Uri.parse(rawAudioUri);
    String path = parsed.getPath();
    if ("file".equalsIgnoreCase(parsed.getScheme())
        && path != null
        && path.toLowerCase().endsWith(".pcm")) {
      File pcmFile = new File(path);
      File wavFile = new File(path.substring(0, path.length() - 4) + ".wav");
      if (wavFile.exists() && wavFile.length() > 44L) {
        return Uri.fromFile(wavFile);
      }
      if (pcmFile.exists() && pcmFile.length() > 0L) {
        try {
          byte[] bytes = Files.readAllBytes(pcmFile.toPath());
          return Uri.fromFile(AudioUtils.writeWavFile(wavFile, bytes));
        } catch (Exception e) {
          return null;
        }
      }
      return null;
    }
    return parsed;
  }
}
