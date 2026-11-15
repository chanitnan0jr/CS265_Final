package com.example.cs265final.data

/** Finds the selected object, even when multiple notes contain identical values. */
internal fun List<Note>.indexOfNoteInstance(target: Note): Int = indexOfFirst { it === target }

/** Filters notes by text, folder and a half-open timestamp range. Null filters include all notes. */
fun List<Note>.filterNotes(query: String = "", folder: String? = null, from: Long? = null, until: Long? = null, audioSource: AudioSource? = null): List<Note> {
    val text = query.trim()
    return filter { note ->
        (text.isEmpty() || note.title.contains(text, ignoreCase = true) || note.body.contains(text, ignoreCase = true)) &&
            (folder == null || note.folder == folder) &&
            (audioSource == null || note.audioSource == audioSource) &&
            (from == null || note.time >= from) && (until == null || note.time < until)
    }
}
