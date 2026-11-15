package com.example.cs265final.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteStorageTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = appContext.getSharedPreferences("note_storage_test", Context.MODE_PRIVATE)
    private val context = object : ContextWrapper(appContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = prefs
    }

    @After fun cleanup() { prefs.edit().clear().commit() }

    @Test fun savedNotesSurviveNewStorageInstance() {
        val notes = listOf(Note("Title", "Thai text", "calm", "Work", 123L))
        assertTrue(NoteStorage(context).save(notes))
        assertEquals(notes, NoteStorage(context).load())
    }

    @Test fun storedThaiNotesCanBeFilteredAfterReload() {
        val notes = listOf(
            Note("บันทึกวันนี้", "รู้สึกดี\nและสงบ", "สงบ", "ไดอารี่", 100L),
            Note("Meeting", "Project notes", "เฉยๆ", "งาน", 200L),
        )
        assertTrue(NoteStorage(context).save(notes))
        val restored = NoteStorage(context).load()
        assertEquals(notes, restored)
        assertEquals(listOf(notes[0]), restored.filterNotes(query = "รู้สึก", folder = "ไดอารี่", from = 100L, until = 200L))
        assertEquals(emptyList<Note>(), restored.filterNotes(folder = "งาน", until = 200L))
    }

    @Test fun renamedNotePersistsAndCanBeFoundByItsNewTitle() {
        val original = Note("Old title", "Original text", "calm", "Work", 100L)
        val other = Note("Other", "Other text", "happy", "Home", 200L)
        assertTrue(NoteStorage(context).save(listOf(original, other)))
        val loaded = NoteStorage(context).load()
        val renamed = loaded[0].copy(title = "New title")
        assertTrue(NoteStorage(context).save(listOf(renamed, loaded[1])))
        val restored = NoteStorage(context).load()
        assertEquals(listOf(renamed), restored.filterNotes(query = "New title"))
        assertEquals(other, restored[1])
        assertEquals("Original text", restored[0].body)
    }

    @Test fun sourceRoundTripsAndLegacyAudioUrisMigrate() {
        val legacy = """[{"title":"Recording","body":"text","mood":"calm","folder":"Work","time":1,"audioUri":"file:///data/voice.wav"},{"title":"Upload","body":"text","mood":"calm","folder":"Home","time":2,"audioUri":"content://audio/1"},{"title":"Old","body":"text","mood":"calm","folder":"Home","time":3}]"""
        assertTrue(prefs.edit().putString("items", legacy).commit())
        val migrated = NoteStorage(context).load()
        assertEquals(listOf(AudioSource.RECORDED, AudioSource.UPLOADED, AudioSource.UNKNOWN), migrated.map { it.audioSource })
        assertTrue(NoteStorage(context).save(migrated))
        assertEquals(migrated, NoteStorage(context).load())
    }

    @Test fun malformedDataIsNotTreatedAsAnEmptyLibrary() {
        prefs.edit().putString("items", "invalid").commit()
        try {
            NoteStorage(context).load()
            throw AssertionError("Expected invalid JSON to fail")
        } catch (_: JSONException) {
            assertEquals("invalid", prefs.getString("items", null))
        }
    }
}
