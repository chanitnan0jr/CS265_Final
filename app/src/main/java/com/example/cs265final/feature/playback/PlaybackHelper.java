package com.example.cs265final.feature.playback;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.Locale;

/** Shared audio output, focus, source binding, and time formatting helpers for playback views. */
public final class PlaybackHelper {
  public static final AudioAttributes DEFAULT_MEDIA_ATTRS =
      new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build();

  private PlaybackHelper() {}

  @NonNull
  public static String formatTime(int milliseconds) {
    int seconds = Math.max(0, milliseconds / 1000);
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
  }

  public static void ensureAudibleMediaOutput(
      @NonNull Context context, @NonNull AudioAttributes attrs) {
    AudioManager am = context.getSystemService(AudioManager.class);
    if (am == null) {
      return;
    }
    if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
      int target = Math.max(1, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2);
      try {
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
      } catch (RuntimeException ignored) {
        // Best-effort volume adjustment.
      }
    }
    if (Build.VERSION.SDK_INT >= 26) {
      am.requestAudioFocus(
          new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
              .setAudioAttributes(attrs)
              .build());
    } else {
      @SuppressWarnings("deprecation")
      int ignored =
          am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }
  }

  public static void setAudioSource(
      @NonNull MediaPlayer player,
      @NonNull Context context,
      @NonNull Uri uri,
      @NonNull AudioAttributes attrs)
      throws IOException {
    player.setAudioAttributes(attrs);
    player.setVolume(1f, 1f);
    boolean loaded = false;
    try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
      if (pfd != null) {
        player.setDataSource(pfd.getFileDescriptor());
        loaded = true;
      }
    } catch (Exception ignored) {
      // Fall back to Context+Uri resolution below.
    }
    if (!loaded) {
      player.setDataSource(context, uri);
    }
  }
}
