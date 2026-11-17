package com.example.cs265final.feature.playback;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import com.example.cs265final.R;

/** Audio playback screen with seek bar, time indicator, and ON_STOP pause handling. */
public final class PlaybackView extends LinearLayout {
  private static final long POLL_MS = 200L;

  private final TextView statusView;
  private final SeekBar seekBar;
  private final TextView timeView;
  private final Button toggleButton;
  private final Handler handler = new Handler(Looper.getMainLooper());

  @Nullable private MediaPlayer player;
  @Nullable private Uri boundUri;
  @Nullable private Lifecycle observedLifecycle;
  private boolean ready;
  private boolean playing;
  private int positionMs;
  private int durationMs;
  private Runnable onBack;

  private final LifecycleEventObserver lifecycleObserver =
      (LifecycleOwner source, Lifecycle.Event event) -> {
        if (event == Lifecycle.Event.ON_STOP) {
          pauseForBackground();
        }
      };

  private final Runnable progressTicker =
      new Runnable() {
        @Override
        public void run() {
          if (playing && player != null && ready) {
            try {
              positionMs = Math.max(0, player.getCurrentPosition());
            } catch (IllegalStateException ignored) {
              // Ignore transient state errors.
            }
            updateUi();
            handler.postDelayed(this, POLL_MS);
          }
        }
      };

  public PlaybackView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_playback, this, true);
    statusView = findViewById(R.id.text_playback_status);
    seekBar = findViewById(R.id.seekbar_playback);
    timeView = findViewById(R.id.text_playback_time);
    toggleButton = findViewById(R.id.button_playback_toggle);

    findViewById(R.id.button_playback_back)
        .setOnClickListener(
            view -> {
              if (onBack != null) {
                onBack.run();
              }
            });

    seekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (fromUser && player != null && ready) {
              positionMs = progress;
              player.seekTo(progress);
              updateUi();
            }
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });

    toggleButton.setOnClickListener(view -> togglePlayback());
  }

  public void setOnBack(Runnable action) {
    onBack = action;
  }

  public void bindLifecycle(@Nullable Lifecycle lifecycle) {
    if (observedLifecycle == lifecycle) {
      return;
    }
    if (observedLifecycle != null) {
      observedLifecycle.removeObserver(lifecycleObserver);
    }
    observedLifecycle = lifecycle;
    if (observedLifecycle != null) {
      observedLifecycle.addObserver(lifecycleObserver);
    }
  }

  public void bind(@NonNull Uri uri) {
    if (uri.equals(boundUri) && player != null) {
      return;
    }
    releasePlayer();
    boundUri = uri;
    ready = false;
    playing = false;
    positionMs = 0;
    durationMs = 0;
    statusView.setText(R.string.playback_loading);
    statusView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
    statusView.setVisibility(VISIBLE);
    updateUi();

    MediaPlayer mp = new MediaPlayer();
    player = mp;
    try {
      mp.setOnPreparedListener(
          prepared -> {
            durationMs = Math.max(0, prepared.getDuration());
            ready = true;
            statusView.setVisibility(GONE);
            updateUi();
          });
      mp.setOnCompletionListener(
          completed -> {
            playing = false;
            positionMs = durationMs;
            handler.removeCallbacks(progressTicker);
            updateUi();
          });
      mp.setOnErrorListener(
          (ignored, what, extra) -> {
            ready = false;
            playing = false;
            handler.removeCallbacks(progressTicker);
            statusView.setText(R.string.playback_error_play);
            statusView.setTextColor(ContextCompat.getColor(getContext(), R.color.status_error));
            statusView.setVisibility(VISIBLE);
            updateUi();
            return true;
          });
      PlaybackHelper.setAudioSource(mp, getContext(), uri, PlaybackHelper.DEFAULT_MEDIA_ATTRS);
      mp.prepareAsync();
    } catch (Exception e) {
      ready = false;
      playing = false;
      statusView.setText(R.string.playback_error_open);
      statusView.setTextColor(ContextCompat.getColor(getContext(), R.color.status_error));
      statusView.setVisibility(VISIBLE);
      updateUi();
    }
  }

  public void pauseForBackground() {
    if (player != null && ready && playing) {
      try {
        positionMs = Math.max(0, player.getCurrentPosition());
        player.pause();
      } catch (IllegalStateException ignored) {
        // Ignore if player already released.
      }
      playing = false;
      handler.removeCallbacks(progressTicker);
      updateUi();
    }
  }

  public void releasePlayer() {
    handler.removeCallbacks(progressTicker);
    playing = false;
    ready = false;
    boundUri = null;
    if (player != null) {
      try {
        player.release();
      } catch (RuntimeException ignored) {
        // Best-effort release.
      }
      player = null;
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    bindLifecycle(null);
    releasePlayer();
  }

  private void togglePlayback() {
    if (player == null || !ready) {
      return;
    }
    if (playing) {
      player.pause();
      playing = false;
      handler.removeCallbacks(progressTicker);
    } else {
      PlaybackHelper.ensureAudibleMediaOutput(getContext(), PlaybackHelper.DEFAULT_MEDIA_ATTRS);
      if (durationMs > 0 && positionMs >= durationMs) {
        player.seekTo(0);
        positionMs = 0;
      }
      player.start();
      playing = true;
      handler.removeCallbacks(progressTicker);
      handler.postDelayed(progressTicker, POLL_MS);
    }
    updateUi();
  }

  private void updateUi() {
    int max = Math.max(1, durationMs);
    seekBar.setMax(max);
    seekBar.setProgress(Math.max(0, Math.min(max, positionMs)));
    seekBar.setEnabled(ready && durationMs > 0);
    toggleButton.setEnabled(ready);
    toggleButton.setText(playing ? R.string.playback_pause : R.string.playback_play);
    timeView.setText(
        PlaybackHelper.formatTime(positionMs) + " / " + PlaybackHelper.formatTime(durationMs));
  }
}
