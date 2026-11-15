package com.example.cs265final.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteFiltersTest {
    @Test fun filtersByTextFolderAndDateTogether() {
        val notes = listOf(
            Note("Morning", "Hello Thai", "calm", "Work", 100),
            Note("Other", "HELLO", "calm", "Home", 200),
            Note("Late", "hello", "calm", "Work", 300),
        )
        assertEquals(listOf(notes[0]), notes.filterNotes(" hello ", "Work", 100, 300))
        assertEquals(notes, notes.filterNotes())
        assertEquals(emptyList<Note>(), notes.filterNotes("missing"))
    }
    @Test fun searchSupportsThaiAndDateBoundaries() {
        val before = Note("ก่อน", "first", "calm", "Work", 99)
        val start = Note("เรียนภาษาไทย", "บันทึกวันนี้", "calm", "Work", 100)
        val end = Note("หลัง", "บันทึกวันนี้", "calm", "Home", 200)
        val notes = listOf(before, start, end)
        assertEquals(listOf(start), notes.filterNotes(query = "ภาษาไทย"))
        assertEquals(listOf(start), notes.filterNotes(folder = "Work", from = 100, until = 200))
        assertEquals(listOf(end), notes.filterNotes(query = "บันทึก", from = 200))
    }
    @Test fun audioSourceIsIndependentOfFolderName() {
        val recorded = Note("voice", "text", "calm", "ไฟล์ที่อัปโหลด", 1, "file:///voice.wav", AudioSource.RECORDED)
        val uploaded = Note("upload", "text", "calm", "Work", 2, "content://audio/1", AudioSource.UPLOADED)
        val unknown = Note("text", "text", "calm", "Work", 3)
        val notes = listOf(recorded, uploaded, unknown)
        assertEquals(listOf(recorded), notes.filterNotes(audioSource = AudioSource.RECORDED))
        assertEquals(listOf(uploaded), notes.filterNotes(folder = "Work", audioSource = AudioSource.UPLOADED))
        assertEquals(listOf(unknown), notes.filterNotes(audioSource = AudioSource.UNKNOWN))
    }
    @Test fun selectsTheExactNoteWhenValuesAreIdentical() {
        val first = Note("Same", "Same", "calm", "Work", 100)
        val second = first.copy()
        val notes = listOf(first, second)
        assertEquals(0, notes.indexOfNoteInstance(first))
        assertEquals(1, notes.indexOfNoteInstance(second))
        assertEquals(-1, notes.indexOfNoteInstance(first.copy()))
    }
}
