package com.example.cs265final.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;

/**
 * Coordinates note and folder persistence for the UI layer.
 *
 * <p>Uses {@link NoteStorage} and {@link FolderStorage} so UI screens stay independent of JSON
 * serialization and SharedPreferences details.
 */
public final class NoteRepository {
  private final NoteStorage noteStorage;
  private final FolderStorage folderStorage;

  /** Snapshot returned after loading notes and folders together. */
  public static final class LibrarySnapshot {
    public final List<Note> notes;
    public final List<String> folders;

    public LibrarySnapshot(@NonNull List<Note> notes, @NonNull List<String> folders) {
      this.notes = notes;
      this.folders = folders;
    }
  }

  public NoteRepository(@NonNull Context context) {
    this.noteStorage = new NoteStorage(context);
    this.folderStorage = new FolderStorage(context);
  }

  @NonNull
  public LibrarySnapshot loadLibrary() throws JSONException {
    List<Note> notes = noteStorage.load();
    List<String> folders = folderStorage.load(notes);
    return new LibrarySnapshot(notes, folders);
  }

  @NonNull
  public List<Note> loadNotes() throws JSONException {
    return noteStorage.load();
  }

  @Nullable
  public List<Note> saveNotesPreservingTranscriptions(@NonNull List<Note> notes) {
    return noteStorage.savePreservingTranscriptions(notes);
  }

  /** Ensures a folder name is registered in persistent folder storage when non-blank. */
  public boolean ensureFolderExists(@NonNull String folderName, @NonNull List<String> currentFolders) {
    String trimmed = folderName.trim();
    if (trimmed.isEmpty()) {
      return true;
    }
    for (String existing : currentFolders) {
      if (existing.equals(trimmed)) {
        return true;
      }
    }
    List<String> updated = new ArrayList<>(currentFolders);
    updated.add(trimmed);
    return folderStorage.save(updated);
  }

  /** Creates a new folder if the trimmed name is non-blank and does not already exist. */
  @Nullable
  public List<String> createFolder(@NonNull String name, @NonNull List<String> currentFolders) {
    String trimmed = name.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    for (String existing : currentFolders) {
      if (existing.equalsIgnoreCase(trimmed)) {
        return null;
      }
    }
    List<String> updated = new ArrayList<>(currentFolders);
    updated.add(trimmed);
    return folderStorage.save(updated) ? updated : null;
  }

  /** Renames a folder and updates every note assigned to that folder. */
  @Nullable
  public LibrarySnapshot renameFolder(
      @NonNull String oldName,
      @NonNull String newName,
      @NonNull List<Note> currentNotes,
      @NonNull List<String> currentFolders) {
    String trimmed = newName.trim();
    if (trimmed.isEmpty() || oldName.equals(trimmed)) {
      return null;
    }
    for (String existing : currentFolders) {
      if (existing.equalsIgnoreCase(trimmed)) {
        return null;
      }
    }
    List<Note> updatedNotes = new ArrayList<>(currentNotes.size());
    for (Note note : currentNotes) {
      if (oldName.equals(note.getFolder())) {
        updatedNotes.add(note.withFolder(trimmed));
      } else {
        updatedNotes.add(note);
      }
    }
    List<Note> savedNotes = noteStorage.savePreservingTranscriptions(updatedNotes);
    if (savedNotes == null) {
      return null;
    }
    List<String> updatedFolders = new ArrayList<>(currentFolders.size());
    boolean replaced = false;
    for (String folder : currentFolders) {
      if (oldName.equals(folder)) {
        updatedFolders.add(trimmed);
        replaced = true;
      } else {
        updatedFolders.add(folder);
      }
    }
    if (!replaced) {
      updatedFolders.add(trimmed);
    }
    if (!folderStorage.save(updatedFolders)) {
      return null;
    }
    return new LibrarySnapshot(savedNotes, updatedFolders);
  }

  /** Deletes a folder after reassigning its notes to {@code fallbackFolder}. */
  @Nullable
  public LibrarySnapshot deleteFolder(
      @NonNull String targetFolder,
      @NonNull String fallbackFolder,
      @NonNull List<Note> currentNotes,
      @NonNull List<String> currentFolders) {
    if (targetFolder.equals(fallbackFolder)) {
      return null;
    }
    List<Note> updatedNotes = new ArrayList<>(currentNotes.size());
    boolean movedAny = false;
    for (Note note : currentNotes) {
      if (targetFolder.equals(note.getFolder())) {
        updatedNotes.add(note.withFolder(fallbackFolder));
        movedAny = true;
      } else {
        updatedNotes.add(note);
      }
    }
    List<Note> savedNotes = noteStorage.savePreservingTranscriptions(updatedNotes);
    if (savedNotes == null) {
      return null;
    }
    List<String> updatedFolders = new ArrayList<>(currentFolders.size());
    for (String folder : currentFolders) {
      if (!targetFolder.equals(folder)) {
        updatedFolders.add(folder);
      }
    }
    if (movedAny && !updatedFolders.contains(fallbackFolder)) {
      updatedFolders.add(fallbackFolder);
    }
    if (!folderStorage.save(updatedFolders)) {
      return null;
    }
    return new LibrarySnapshot(savedNotes, updatedFolders);
  }

  /** Finds a note index by stable ID first, falling back to object reference. */
  public static int indexOfNote(@NonNull List<Note> notes, @NonNull Note target) {
    for (int i = 0; i < notes.size(); i++) {
      if (target.getId().equals(notes.get(i).getId())) {
        return i;
      }
    }
    return NoteFilters.indexOfNoteInstance(notes, target);
  }
}
