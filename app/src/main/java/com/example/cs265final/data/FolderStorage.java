package com.example.cs265final.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Persists user-visible note folders separately from notes so empty folders can be created,
 * renamed, and deleted cleanly.
 *
 * <p>Folders categorize notes for organization and search scoping; they are distinct from ordered
 * audio playback playlists.
 */
public final class FolderStorage {
  public static final String KEY_FOLDERS = "folders";
  public static final String DEFAULT_GENERAL_FOLDER = "ทั่วไป";
  public static final String DEFAULT_UPLOAD_FOLDER = "ไฟล์ที่อัปโหลด";
  public static final List<String> DEFAULT_FOLDERS =
      Collections.unmodifiableList(
          Arrays.asList("การเรียน", "ระบายความรู้สึก", "งาน / โปรเจกต์", "ไอเดีย"));

  private final SharedPreferences prefs;

  public FolderStorage(@NonNull Context context) {
    this.prefs = context.getSharedPreferences(NoteStorage.PREFS_NAME, Context.MODE_PRIVATE);
  }

  /**
   * Loads persisted folders. On first run, seeds {@link #DEFAULT_FOLDERS} plus any distinct folder
   * labels already present on existing notes.
   */
  @NonNull
  public List<String> load(@NonNull List<Note> existingNotes) throws JSONException {
    if (!prefs.contains(KEY_FOLDERS)) {
      Set<String> seeded = new LinkedHashSet<>(DEFAULT_FOLDERS);
      for (Note note : existingNotes) {
        String folder = note.getFolder().trim();
        if (!folder.isEmpty()) {
          seeded.add(folder);
        }
      }
      List<String> list = new ArrayList<>(seeded);
      save(list);
      return list;
    }
    String raw = prefs.getString(KEY_FOLDERS, "[]");
    JSONArray array = new JSONArray(raw == null ? "[]" : raw);
    Set<String> folders = new LinkedHashSet<>();
    for (int i = 0; i < array.length(); i++) {
      String name = array.optString(i, "").trim();
      if (!name.isEmpty()) {
        folders.add(name);
      }
    }
    return new ArrayList<>(folders);
  }

  public boolean save(@NonNull List<String> folders) {
    JSONArray array = new JSONArray();
    for (String folder : folders) {
      String trimmed = folder == null ? "" : folder.trim();
      if (!trimmed.isEmpty()) {
        array.put(trimmed);
      }
    }
    return prefs.edit().putString(KEY_FOLDERS, array.toString()).commit();
  }
}
