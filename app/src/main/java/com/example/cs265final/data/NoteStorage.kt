package com.example.cs265final.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class AudioSource { UNKNOWN, RECORDED, UPLOADED }

data class Note(
    val title: String, val body: String, val mood: String, val folder: String,
    val time: Long, val audioUri: String = "", val audioSource: AudioSource = AudioSource.UNKNOWN,
)

class NoteStorage(context: Context) {
    private val prefs = context.getSharedPreferences("notes", Context.MODE_PRIVATE)

    // A failed read must never be treated as an empty library and then overwritten.
    @Throws(JSONException::class)
    fun load(): List<Note> = JSONArray(prefs.getString("items", "[]")).let { items ->
        (0 until items.length()).map { i ->
            items.getJSONObject(i).let {
                val audioUri = it.optString("audioUri", "")
                // Older notes have no source field. Current recorded files use file: and imports use content:.
                val legacySource = when {
                    audioUri.startsWith("file:", ignoreCase = true) -> AudioSource.RECORDED
                    audioUri.startsWith("content:", ignoreCase = true) -> AudioSource.UPLOADED
                    else -> AudioSource.UNKNOWN
                }
                val source = if (it.has("audioSource")) {
                    AudioSource.entries.firstOrNull { source -> source.name == it.optString("audioSource") } ?: AudioSource.UNKNOWN
                } else legacySource
                Note(it.getString("title"), it.getString("body"), it.getString("mood"), it.getString("folder"), it.getLong("time"), audioUri, source)
            }
        }
    }

    fun save(notes: List<Note>): Boolean {
        val items = JSONArray()
        notes.forEach {
            items.put(JSONObject().put("title", it.title).put("body", it.body).put("mood", it.mood).put("folder", it.folder).put("time", it.time).put("audioUri", it.audioUri).put("audioSource", it.audioSource.name))
        }
        // ponytail: synchronous small-library write; use a database and background writes as the library grows.
        return prefs.edit().putString("items", items.toString()).commit()
    }
}
