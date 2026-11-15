package com.example.cs265final.data

/** Filters notes by text, folder and a half-open timestamp range. Null filters include all notes. */
fun List<Note>.filterNotes(query: String = "", folder: String? = null, from: Long? = null, until: Long? = null): List<Note> {
    val text = query.trim()
    return filter { note ->
        (text.isEmpty() || note.title.contains(text, ignoreCase = true) || note.body.contains(text, ignoreCase = true)) &&
            (folder == null || note.folder == folder) &&
            (from == null || note.time >= from) && (until == null || note.time < until)
    }
}
