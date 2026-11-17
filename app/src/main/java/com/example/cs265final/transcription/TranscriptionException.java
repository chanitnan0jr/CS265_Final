package com.example.cs265final.transcription;

import androidx.annotation.NonNull;

/** Exception thrown when submitting or polling a background transcription job. */
public final class TranscriptionException extends Exception {
  private final boolean retryable;

  public TranscriptionException(@NonNull String message, boolean retryable) {
    super(message);
    this.retryable = retryable;
  }

  /** Returns false for bad input or server rejections where re-uploading cannot help. */
  public boolean getRetryable() {
    return retryable;
  }
}
