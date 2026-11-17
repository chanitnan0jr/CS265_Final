package com.example.cs265final.transcription;

import androidx.annotation.NonNull;

/** Parsed status response from the transcription polling endpoint. */
public final class TranscriptionResponse {
  private final String status;
  private final String text;
  private final String error;

  public TranscriptionResponse(@NonNull String status, @NonNull String text, @NonNull String error) {
    this.status = status;
    this.text = text;
    this.error = error;
  }

  @NonNull
  public String getStatus() {
    return status;
  }

  @NonNull
  public String getText() {
    return text;
  }

  @NonNull
  public String getError() {
    return error;
  }
}
