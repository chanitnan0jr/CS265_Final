package com.example.cs265final.data;

import androidx.annotation.NonNull;
import java.util.Objects;
import java.util.UUID;

/** Immutable voice-note record stored locally in JSON. */
public final class Note {
  private final String id;
  private final String title;
  private final String body;
  private final String mood;
  private final String folder;
  private final long time;
  private final String audioUri;
  private final AudioSource audioSource;
  private final String jobId;
  private final TranscriptionStatus transcription;
  private final String transcriptionError;

  public Note(
      @NonNull String title,
      @NonNull String body,
      @NonNull String mood,
      @NonNull String folder,
      long time) {
    this(
        UUID.randomUUID().toString(),
        title,
        body,
        mood,
        folder,
        time,
        "",
        AudioSource.UNKNOWN,
        "",
        TranscriptionStatus.NONE,
        "");
  }

  public Note(
      @NonNull String title,
      @NonNull String body,
      @NonNull String mood,
      @NonNull String folder,
      long time,
      @NonNull String audioUri,
      @NonNull AudioSource audioSource) {
    this(
        UUID.randomUUID().toString(),
        title,
        body,
        mood,
        folder,
        time,
        audioUri,
        audioSource,
        "",
        TranscriptionStatus.NONE,
        "");
  }

  public Note(
      @NonNull String id,
      @NonNull String title,
      @NonNull String body,
      @NonNull String mood,
      @NonNull String folder,
      long time,
      @NonNull String audioUri,
      @NonNull AudioSource audioSource,
      @NonNull String jobId,
      @NonNull TranscriptionStatus transcription,
      @NonNull String transcriptionError) {
    this.id = id.isEmpty() ? UUID.randomUUID().toString() : id;
    this.title = Objects.requireNonNull(title, "title");
    this.body = Objects.requireNonNull(body, "body");
    this.mood = Objects.requireNonNull(mood, "mood");
    this.folder = Objects.requireNonNull(folder, "folder");
    this.time = time;
    this.audioUri = Objects.requireNonNull(audioUri, "audioUri");
    this.audioSource = Objects.requireNonNull(audioSource, "audioSource");
    this.jobId = Objects.requireNonNull(jobId, "jobId");
    this.transcription = Objects.requireNonNull(transcription, "transcription");
    this.transcriptionError = Objects.requireNonNull(transcriptionError, "transcriptionError");
  }

  @NonNull
  public String getId() {
    return id;
  }

  @NonNull
  public String getTitle() {
    return title;
  }

  @NonNull
  public String getBody() {
    return body;
  }

  @NonNull
  public String getMood() {
    return mood;
  }

  @NonNull
  public String getFolder() {
    return folder;
  }

  public long getTime() {
    return time;
  }

  @NonNull
  public String getAudioUri() {
    return audioUri;
  }

  @NonNull
  public AudioSource getAudioSource() {
    return audioSource;
  }

  @NonNull
  public String getJobId() {
    return jobId;
  }

  @NonNull
  public TranscriptionStatus getTranscription() {
    return transcription;
  }

  @NonNull
  public String getTranscriptionError() {
    return transcriptionError;
  }

  @NonNull
  public Note withTitle(@NonNull String newTitle) {
    return new Note(
        id,
        newTitle,
        body,
        mood,
        folder,
        time,
        audioUri,
        audioSource,
        jobId,
        transcription,
        transcriptionError);
  }

  @NonNull
  public Note withFolder(@NonNull String newFolder) {
    return new Note(
        id,
        title,
        body,
        mood,
        newFolder,
        time,
        audioUri,
        audioSource,
        jobId,
        transcription,
        transcriptionError);
  }

  @NonNull
  public Note withAudio(@NonNull String newAudioUri, @NonNull AudioSource newAudioSource) {
    return new Note(
        id,
        title,
        body,
        mood,
        folder,
        time,
        newAudioUri,
        newAudioSource,
        jobId,
        transcription,
        transcriptionError);
  }

  @NonNull
  public Note withTranscription(
      @NonNull String newBody,
      @NonNull String newJobId,
      @NonNull TranscriptionStatus newStatus,
      @NonNull String newError) {
    return new Note(
        id,
        title,
        newBody,
        mood,
        folder,
        time,
        audioUri,
        audioSource,
        newJobId,
        newStatus,
        newError);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Note)) {
      return false;
    }
    Note note = (Note) other;
    return time == note.time
        && id.equals(note.id)
        && title.equals(note.title)
        && body.equals(note.body)
        && mood.equals(note.mood)
        && folder.equals(note.folder)
        && audioUri.equals(note.audioUri)
        && audioSource == note.audioSource
        && jobId.equals(note.jobId)
        && transcription == note.transcription
        && transcriptionError.equals(note.transcriptionError);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        title,
        body,
        mood,
        folder,
        time,
        audioUri,
        audioSource,
        jobId,
        transcription,
        transcriptionError);
  }
}
