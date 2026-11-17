package com.example.cs265final.data;

/** Lifecycle state of a background audio transcription job for a note. */
public enum TranscriptionStatus {
  NONE,
  QUEUED,
  PROCESSING,
  COMPLETED,
  FAILED;

  /** Returns true while a background worker is still expected to update this note. */
  public boolean isPending() {
    return this == QUEUED || this == PROCESSING;
  }
}
