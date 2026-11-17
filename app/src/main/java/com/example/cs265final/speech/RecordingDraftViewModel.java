package com.example.cs265final.speech;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import java.io.ByteArrayOutputStream;

/** Retains the in-memory PCM audio buffer for an unsaved voice note across rotation. */
public final class RecordingDraftViewModel extends ViewModel {
  @Nullable private ByteArrayOutputStream audioBuffer;

  @Nullable
  public ByteArrayOutputStream getAudioBuffer() {
    return audioBuffer;
  }

  public void setAudioBuffer(@Nullable ByteArrayOutputStream audioBuffer) {
    this.audioBuffer = audioBuffer;
  }
}
