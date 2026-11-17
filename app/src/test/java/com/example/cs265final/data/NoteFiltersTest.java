package com.example.cs265final.data;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class NoteFiltersTest {
  @Test
  public void filtersByTextFolderAndDateTogether() {
    List<Note> notes =
        Arrays.asList(
            new Note("Morning", "Hello Thai", "calm", "Work", 100),
            new Note("Other", "HELLO", "calm", "Home", 200),
            new Note("Late", "hello", "calm", "Work", 300));
    assertEquals(
        Collections.singletonList(notes.get(0)),
        NoteFilters.filterNotes(notes, " hello ", "Work", 100L, 300L, null));
    assertEquals(notes, NoteFilters.filterNotes(notes, "", null, null, null, null));
    assertEquals(
        Collections.emptyList(),
        NoteFilters.filterNotes(notes, "missing", null, null, null, null));
  }

  @Test
  public void searchSupportsThaiAndDateBoundaries() {
    Note before = new Note("ก่อน", "first", "calm", "Work", 99);
    Note start = new Note("เรียนภาษาไทย", "บันทึกวันนี้", "calm", "Work", 100);
    Note end = new Note("หลัง", "บันทึกวันนี้", "calm", "Home", 200);
    List<Note> notes = Arrays.asList(before, start, end);
    assertEquals(
        Collections.singletonList(start),
        NoteFilters.filterNotes(notes, "ภาษาไทย", null, null, null, null));
    assertEquals(
        Collections.singletonList(start),
        NoteFilters.filterNotes(notes, "", "Work", 100L, 200L, null));
    assertEquals(
        Collections.singletonList(end),
        NoteFilters.filterNotes(notes, "บันทึก", null, 200L, null, null));
  }

  @Test
  public void audioSourceIsIndependentOfFolderName() {
    Note recorded =
        new Note("voice", "text", "calm", "ไฟล์ที่อัปโหลด", 1, "file:///voice.wav", AudioSource.RECORDED);
    Note uploaded =
        new Note("upload", "text", "calm", "Work", 2, "content://audio/1", AudioSource.UPLOADED);
    Note unknown = new Note("text", "text", "calm", "Work", 3);
    List<Note> notes = Arrays.asList(recorded, uploaded, unknown);
    assertEquals(
        Collections.singletonList(recorded),
        NoteFilters.filterNotes(notes, "", null, null, null, AudioSource.RECORDED));
    assertEquals(
        Collections.singletonList(uploaded),
        NoteFilters.filterNotes(notes, "", "Work", null, null, AudioSource.UPLOADED));
    assertEquals(
        Collections.singletonList(unknown),
        NoteFilters.filterNotes(notes, "", null, null, null, AudioSource.UNKNOWN));
  }

  @Test
  public void selectsTheExactNoteWhenValuesAreIdentical() {
    Note first = new Note("Same", "Same", "calm", "Work", 100);
    Note second = first.withTitle("Same");
    List<Note> notes = Arrays.asList(first, second);
    assertEquals(0, NoteFilters.indexOfNoteInstance(notes, first));
    assertEquals(1, NoteFilters.indexOfNoteInstance(notes, second));
    assertEquals(-1, NoteFilters.indexOfNoteInstance(notes, first.withTitle("Same")));
  }

  @Test
  public void searchTrimsUnicodeSpacesAndHandlesEmptyRanges() {
    Note note = new Note("Mixed CASE", "บันทึก", "calm", "Work", 100);
    List<Note> notes = Collections.singletonList(note);
    assertEquals(
        notes, NoteFilters.filterNotes(notes, "\u00a0case\u00a0", null, null, null, null));
    assertEquals(notes, NoteFilters.filterNotes(notes, "\u00a0", null, null, null, null));
    assertEquals(
        Collections.emptyList(),
        NoteFilters.filterNotes(notes, "", null, 100L, 100L, null));
    assertEquals(
        Collections.emptyList(),
        NoteFilters.filterNotes(Collections.emptyList(), "", null, null, null, null));
  }

  @Test
  public void folderScopedSearchCombinesQueryAndFolder() {
    Note workMatch = new Note("Project plan", "budget", "calm", "งาน / โปรเจกต์", 100);
    Note studyMatch = new Note("Exam plan", "budget", "calm", "การเรียน", 200);
    List<Note> notes = Arrays.asList(workMatch, studyMatch);
    assertEquals(
        Collections.singletonList(studyMatch),
        NoteFilters.filterNotes(notes, "budget", "การเรียน", null, null, null));
    assertEquals(
        Collections.emptyList(),
        NoteFilters.filterNotes(notes, "Project", "การเรียน", null, null, null));
  }
}
