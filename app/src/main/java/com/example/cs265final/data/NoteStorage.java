package com.example.cs265final.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Persists notes as JSON inside app-private SharedPreferences.
 *
 * <p>All read-modify-write cycles are serialized under {@link #LOCK} because WorkManager jobs run
 * in the same process.
 */
public final class NoteStorage {
  public static final String PREFS_NAME = "notes";
  public static final String KEY_ITEMS = "items";
  private static final Object LOCK = new Object();

  private final SharedPreferences prefs;

  /** Transform applied while holding the storage lock. */
  public interface NoteTransform {
    @NonNull
    Note apply(@NonNull Note note);
  }

  public NoteStorage(@NonNull Context context) {
    this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  /**
   * Loads all persisted notes. Throws {@link JSONException} on corrupt data so callers never treat
   * a read failure as an empty library and overwrite user notes.
   */
  @NonNull
  public List<Note> load() throws JSONException {
    String raw = prefs.getString(KEY_ITEMS, "[]");
    JSONArray items = new JSONArray(raw == null ? "[]" : raw);
    List<Note> result = new ArrayList<>(items.length());
    for (int i = 0; i < items.length(); i++) {
      JSONObject obj = items.getJSONObject(i);
      String title = obj.getString("title");
      String body = obj.getString("body");
      String mood = obj.getString("mood");
      String folder = obj.getString("folder");
      long time = obj.getLong("time");
      String id = obj.optString("id", "");
      if (id.trim().isEmpty()) {
        // Derive a deterministic UUID for legacy records lacking an explicit id.
        String seed = i + ":" + title + ":" + time;
        id = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
      }
      String audioUri = obj.optString("audioUri", "");
      AudioSource source = parseAudioSource(obj, audioUri);
      TranscriptionStatus status = parseTranscriptionStatus(obj.optString("transcription", ""));
      result.add(
          new Note(
              id,
              title,
              body,
              mood,
              folder,
              time,
              audioUri,
              source,
              obj.optString("jobId", ""),
              status,
              obj.optString("transcriptionError", "")));
    }
    return result;
  }

  public boolean save(@NonNull List<Note> notes) {
    synchronized (LOCK) {
      return write(notes);
    }
  }

  /**
   * Saves UI edits without reverting a transcription result written by a background worker after
   * the UI loaded its list. Returns the merged list, or null when a read or write failed.
   */
  @Nullable
  public List<Note> savePreservingTranscriptions(@NonNull List<Note> notes) {
    synchronized (LOCK) {
      List<Note> stored;
      try {
        stored = load();
      } catch (JSONException e) {
        return null;
      }
      Map<String, Note> storedByJob = new HashMap<>();
      for (Note item : stored) {
        if (!item.getJobId().isEmpty()) {
          storedByJob.put(item.getJobId(), item);
        }
      }
      List<Note> merged = new ArrayList<>(notes.size());
      for (Note note : notes) {
        if (!note.getTranscription().isPending()) {
          merged.add(note);
          continue;
        }
        Note latest = storedByJob.get(note.getJobId());
        if (latest != null
            && (latest.getTranscription() != note.getTranscription()
                || !latest.getBody().equals(note.getBody())
                || !latest.getTranscriptionError().equals(note.getTranscriptionError()))) {
          merged.add(
              note.withTranscription(
                  latest.getBody(),
                  note.getJobId(),
                  latest.getTranscription(),
                  latest.getTranscriptionError()));
        } else {
          merged.add(note);
        }
      }
      return write(merged) ? merged : null;
    }
  }

  @Nullable
  public Note findNote(@NonNull String jobId) throws JSONException {
    synchronized (LOCK) {
      for (Note note : load()) {
        if (jobId.equals(note.getJobId())) {
          return note;
        }
      }
      return null;
    }
  }

  /** Applies a worker update only while the stored note still carries the matching job ID. */
  public boolean updateTranscription(@NonNull String jobId, @NonNull NoteTransform transform)
      throws JSONException {
    synchronized (LOCK) {
      List<Note> notes = new ArrayList<>(load());
      for (int i = 0; i < notes.size(); i++) {
        if (jobId.equals(notes.get(i).getJobId())) {
          notes.set(i, transform.apply(notes.get(i)));
          return write(notes);
        }
      }
      return false;
    }
  }

  private boolean write(@NonNull List<Note> notes) {
    JSONArray items = new JSONArray();
    try {
      for (Note note : notes) {
        items.put(
            new JSONObject()
                .put("id", note.getId())
                .put("title", note.getTitle())
                .put("body", note.getBody())
                .put("mood", note.getMood())
                .put("folder", note.getFolder())
                .put("time", note.getTime())
                .put("audioUri", note.getAudioUri())
                .put("audioSource", note.getAudioSource().name())
                .put("jobId", note.getJobId())
                .put("transcription", note.getTranscription().name())
                .put("transcriptionError", note.getTranscriptionError()));
      }
    } catch (JSONException e) {
      return false;
    }
    // commit() is used intentionally so callers know synchronously whether persistence succeeded.
    return prefs.edit().putString(KEY_ITEMS, items.toString()).commit();
  }

  private static AudioSource parseAudioSource(JSONObject obj, String audioUri) {
    if (obj.has("audioSource")) {
      String raw = obj.optString("audioSource", "");
      for (AudioSource candidate : AudioSource.values()) {
        if (candidate.name().equals(raw)) {
          return candidate;
        }
      }
      return AudioSource.UNKNOWN;
    }
    String lower = audioUri.toLowerCase(Locale.ROOT);
    if (lower.startsWith("file:")) {
      return AudioSource.RECORDED;
    }
    if (lower.startsWith("content:")) {
      return AudioSource.UPLOADED;
    }
    return AudioSource.UNKNOWN;
  }

  private static TranscriptionStatus parseTranscriptionStatus(String raw) {
    for (TranscriptionStatus status : TranscriptionStatus.values()) {
      if (status.name().equals(raw)) {
        return status;
      }
    }
    return TranscriptionStatus.NONE;
  }
}
