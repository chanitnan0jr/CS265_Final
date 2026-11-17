package com.example.cs265final.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NoteStorageTest {
  private final Context appContext = ApplicationProvider.getApplicationContext();
  private final SharedPreferences prefs =
      appContext.getSharedPreferences("note_storage_test", Context.MODE_PRIVATE);
  private final Context context =
      new ContextWrapper(appContext) {
        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
          return prefs;
        }
      };

  @After
  public void cleanup() {
    prefs.edit().clear().commit();
  }

  @Test
  public void savedNotesSurviveNewStorageInstance() throws Exception {
    List<Note> notes =
        Collections.singletonList(new Note("Title", "Thai text", "calm", "Work", 123L));
    assertTrue(new NoteStorage(context).save(notes));
    assertEquals(notes, new NoteStorage(context).load());
  }

  @Test
  public void storedThaiNotesCanBeFilteredAfterReload() throws Exception {
    List<Note> notes =
        Arrays.asList(
            new Note("บันทึกวันนี้", "รู้สึกดี\nและสงบ", "สงบ", "ไดอารี่", 100L),
            new Note("Meeting", "Project notes", "เฉยๆ", "งาน", 200L));
    assertTrue(new NoteStorage(context).save(notes));
    List<Note> restored = new NoteStorage(context).load();
    assertEquals(notes, restored);
    assertEquals(
        Collections.singletonList(notes.get(0)),
        NoteFilters.filterNotes(restored, "รู้สึก", "ไดอารี่", 100L, 200L, null));
    assertEquals(
        Collections.emptyList(),
        NoteFilters.filterNotes(restored, "", "งาน", null, 200L, null));
  }

  @Test
  public void renamedNotePersistsAndCanBeFoundByItsNewTitle() throws Exception {
    Note original = new Note("Old title", "Original text", "calm", "Work", 100L);
    Note other = new Note("Other", "Other text", "happy", "Home", 200L);
    assertTrue(new NoteStorage(context).save(Arrays.asList(original, other)));
    List<Note> loaded = new NoteStorage(context).load();
    Note renamed = loaded.get(0).withTitle("New title");
    assertTrue(new NoteStorage(context).save(Arrays.asList(renamed, loaded.get(1))));
    List<Note> restored = new NoteStorage(context).load();
    assertEquals(
        Collections.singletonList(renamed),
        NoteFilters.filterNotes(restored, "New title", null, null, null, null));
    assertEquals(other, restored.get(1));
    assertEquals("Original text", restored.get(0).getBody());
  }

  @Test
  public void sourceRoundTripsAndLegacyAudioUrisMigrate() throws Exception {
    String legacy =
        "[{\"title\":\"Recording\",\"body\":\"text\",\"mood\":\"calm\",\"folder\":\"Work\",\"time\":1,\"audioUri\":\"file:///data/voice.wav\"},"
            + "{\"title\":\"Upload\",\"body\":\"text\",\"mood\":\"calm\",\"folder\":\"Home\",\"time\":2,\"audioUri\":\"content://audio/1\"},"
            + "{\"title\":\"Old\",\"body\":\"text\",\"mood\":\"calm\",\"folder\":\"Home\",\"time\":3}]";
    assertTrue(prefs.edit().putString("items", legacy).commit());
    List<Note> migrated = new NoteStorage(context).load();
    assertEquals(
        Arrays.asList(AudioSource.RECORDED, AudioSource.UPLOADED, AudioSource.UNKNOWN),
        Arrays.asList(
            migrated.get(0).getAudioSource(),
            migrated.get(1).getAudioSource(),
            migrated.get(2).getAudioSource()));
    assertFalse(migrated.get(0).getId().isEmpty());
    assertTrue(new NoteStorage(context).save(migrated));
    assertEquals(migrated, new NoteStorage(context).load());
  }

  @Test
  public void malformedDataIsNotTreatedAsAnEmptyLibrary() {
    prefs.edit().putString("items", "invalid").commit();
    try {
      new NoteStorage(context).load();
      throw new AssertionError("Expected invalid JSON to fail");
    } catch (JSONException expected) {
      assertEquals("invalid", prefs.getString("items", null));
    }
  }

  @Test
  public void uiSaveKeepsAWorkerTranscriptionResult() throws Exception {
    Note queued =
        new Note(
            "id_1",
            "Voice",
            "",
            "ปกติ",
            "ไฟล์ที่อัปโหลด",
            1L,
            "content://audio/1",
            AudioSource.UPLOADED,
            "job_1",
            TranscriptionStatus.QUEUED,
            "");
    NoteStorage storage = new NoteStorage(context);
    assertTrue(storage.save(Collections.singletonList(queued)));
    assertTrue(
        storage.updateTranscription(
            "job_1",
            n -> n.withTranscription("ข้อความ", "job_1", TranscriptionStatus.COMPLETED, "")));

    List<Note> merged =
        storage.savePreservingTranscriptions(
            Collections.singletonList(queued.withTitle("Renamed")));

    assertNotNull(merged);
    assertEquals("Renamed", merged.get(0).getTitle());
    assertEquals("ข้อความ", merged.get(0).getBody());
    assertEquals(TranscriptionStatus.COMPLETED, merged.get(0).getTranscription());
    assertEquals(merged, storage.load());
  }

  @Test
  public void aStaleWorkerCannotUpdateAReplacementJob() throws Exception {
    Note queued =
        new Note(
            "id_1",
            "Voice",
            "",
            "ปกติ",
            "ไฟล์ที่อัปโหลด",
            1L,
            "content://audio/1",
            AudioSource.UPLOADED,
            "job_1",
            TranscriptionStatus.QUEUED,
            "");
    NoteStorage storage = new NoteStorage(context);
    assertTrue(storage.save(Collections.singletonList(queued)));
    assertTrue(
        storage.updateTranscription(
            "job_1", n -> n.withTranscription(n.getBody(), "job_2", n.getTranscription(), "")));

    assertFalse(
        storage.updateTranscription(
            "job_1",
            n -> n.withTranscription("old audio", "job_1", TranscriptionStatus.COMPLETED, "")));
    assertEquals("job_2", storage.load().get(0).getJobId());
    assertEquals(TranscriptionStatus.QUEUED, storage.load().get(0).getTranscription());
  }

  @Test
  public void deletingOneNoteKeepsAnotherNotesNewWorkerResult() throws Exception {
    Note target = new Note("Delete", "text", "calm", "Work", 1L);
    Note queued =
        new Note(
            "id_keep",
            "Keep",
            "",
            "calm",
            "Work",
            2L,
            "",
            AudioSource.UNKNOWN,
            "job_1",
            TranscriptionStatus.QUEUED,
            "");
    NoteStorage storage = new NoteStorage(context);
    assertTrue(storage.save(Arrays.asList(target, queued)));
    assertTrue(
        storage.updateTranscription(
            "job_1",
            n -> n.withTranscription("finished", "job_1", TranscriptionStatus.COMPLETED, "")));

    List<Note> saved = storage.savePreservingTranscriptions(Collections.singletonList(queued));

    assertEquals(
        Collections.singletonList(
            queued.withTranscription("finished", "job_1", TranscriptionStatus.COMPLETED, "")),
        saved);
    assertEquals(saved, storage.load());
  }

  @Test
  public void folderCrudValidatesAndUpdatesAssignedNotes() throws Exception {
    NoteRepository repo = new NoteRepository(context);
    Note note = new Note("Lecture", "notes", "ปกติ", "การเรียน", 10L);
    assertNotNull(repo.saveNotesPreservingTranscriptions(Collections.singletonList(note)));

    NoteRepository.LibrarySnapshot initial = repo.loadLibrary();
    assertTrue(initial.folders.contains("การเรียน"));

    assertNull(repo.createFolder("   ", initial.folders));
    assertNull(repo.createFolder("การเรียน", initial.folders));
    List<String> created = repo.createFolder("Podcast", initial.folders);
    assertNotNull(created);
    assertTrue(created.contains("Podcast"));

    NoteRepository.LibrarySnapshot renamed =
        repo.renameFolder("การเรียน", "CS265", initial.notes, created);
    assertNotNull(renamed);
    assertTrue(renamed.folders.contains("CS265"));
    assertFalse(renamed.folders.contains("การเรียน"));
    assertEquals("CS265", renamed.notes.get(0).getFolder());

    NoteRepository.LibrarySnapshot deleted =
        repo.deleteFolder(
            "CS265", FolderStorage.DEFAULT_GENERAL_FOLDER, renamed.notes, renamed.folders);
    assertNotNull(deleted);
    assertFalse(deleted.folders.contains("CS265"));
    assertEquals(FolderStorage.DEFAULT_GENERAL_FOLDER, deleted.notes.get(0).getFolder());
  }
}
