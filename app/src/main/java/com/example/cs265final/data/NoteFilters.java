package com.example.cs265final.data;

import java.util.ArrayList;
import java.util.List;

/** Filtering and identity lookup shared by the note screens. */
public final class NoteFilters {
  private NoteFilters() {}

  /** Finds the selected object even if several notes have identical values. */
  public static int indexOfNoteInstance(List<Note> notes, Note target) {
    for (int i = 0; i < notes.size(); i++) {
      if (notes.get(i) == target) {
        return i;
      }
    }
    return -1;
  }

  /** Filters by text, folder, audio source and a half-open timestamp range. */
  public static List<Note> filterNotes(
      List<Note> notes, String query, String folder, Long from, Long until,
      AudioSource audioSource) {
    String text = trimWhitespace(query);
    List<Note> matches = new ArrayList<>();
    for (Note note : notes) {
      if ((text.isEmpty() || containsIgnoreCase(note.getTitle(), text)
              || containsIgnoreCase(note.getBody(), text))
          && (folder == null || folder.equals(note.getFolder()))
          && (audioSource == null || audioSource == note.getAudioSource())
          && (from == null || note.getTime() >= from)
          && (until == null || note.getTime() < until)) {
        matches.add(note);
      }
    }
    return matches;
  }

  private static boolean containsIgnoreCase(String value, String text) {
    for (int i = 0; i <= value.length() - text.length(); i++) {
      if (value.regionMatches(true, i, text, 0, text.length())) {
        return true;
      }
    }
    return false;
  }

  private static String trimWhitespace(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isWhitespace(value.charAt(start))) {
      start++;
    }
    while (end > start && isWhitespace(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(start, end);
  }

  // Kotlin's trim also removes Unicode space characters, such as non-breaking space.
  private static boolean isWhitespace(char value) {
    return Character.isWhitespace(value) || Character.isSpaceChar(value);
  }
}
