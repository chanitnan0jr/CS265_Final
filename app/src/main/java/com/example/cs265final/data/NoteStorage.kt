package com.example.cs265final.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Note(val title: String, val body: String, val mood: String, val folder: String, val time: Long)

class NoteStorage(context: Context) {
    private val prefs = context.getSharedPreferences("notes", Context.MODE_PRIVATE)

    // A failed read must never be treated as an empty library and then overwritten.
    @Throws(JSONException::class)
    fun load(): List<Note> {
        val items = JSONArray(prefs.getString("items", "[]"))
        return (0 until items.length()).map { i ->
            items.getJSONObject(i).let {
                Note(it.getString("title"), it.getString("body"), it.getString("mood"), it.getString("folder"), it.getLong("time"))
            }
        }
    }

    fun save(notes: List<Note>): Boolean {
        val items = JSONArray()
        notes.forEach { items.put(JSONObject().put("title", it.title).put("body", it.body).put("mood", it.mood).put("folder", it.folder).put("time", it.time)) }
        // ponytail: synchronous small-library write; use a database and background writes as the library grows.
        return prefs.edit().putString("items", items.toString()).commit()
    }
}
