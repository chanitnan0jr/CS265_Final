package com.example.cs265final.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class AudioSource { UNKNOWN, RECORDED, UPLOADED }

enum class TranscriptionStatus { NONE, QUEUED, PROCESSING, COMPLETED, FAILED }

/** Work is still expected to update this note's transcription. */
fun TranscriptionStatus.isPending(): Boolean = this == TranscriptionStatus.QUEUED || this == TranscriptionStatus.PROCESSING

data class Note(
    val title: String, val body: String, val mood: String, val folder: String,
    val time: Long, val audioUri: String = "", val audioSource: AudioSource = AudioSource.UNKNOWN,
    val jobId: String = "", val transcription: TranscriptionStatus = TranscriptionStatus.NONE,
    val transcriptionError: String = "",
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
                val transcription = TranscriptionStatus.entries.firstOrNull { status -> status.name == it.optString("transcription") } ?: TranscriptionStatus.NONE
                Note(
                    it.getString("title"), it.getString("body"), it.getString("mood"), it.getString("folder"), it.getLong("time"),
                    audioUri, source, it.optString("jobId", ""), transcription, it.optString("transcriptionError", ""),
                )
            }
        }
    }

    fun save(notes: List<Note>): Boolean = synchronized(lock) { write(notes) }

    /**
     * Saves UI edits without reverting a transcription a worker wrote after the UI read its list.
     * Returns the merged list, or null when a read or write failed.
     */
    fun savePreservingTranscriptions(notes: List<Note>): List<Note>? = synchronized(lock) {
        val stored = runCatching { load() }.getOrElse { return null }
        val storedByJob = stored.filter { it.jobId.isNotEmpty() }.associateBy { it.jobId }
        val merged = notes.map { note ->
            if (!note.transcription.isPending()) note
            else storedByJob[note.jobId]?.takeIf { latest ->
                latest.transcription != note.transcription || latest.body != note.body || latest.transcriptionError != note.transcriptionError
            }?.let { latest ->
                note.copy(body = latest.body, transcription = latest.transcription, transcriptionError = latest.transcriptionError)
            } ?: note
        }
        if (write(merged)) merged else null
    }

    fun findNote(jobId: String): Note? = synchronized(lock) { load().firstOrNull { it.jobId == jobId } }

    /** Applies a worker update only while the note still carries the job that produced it. */
    fun updateTranscription(jobId: String, transform: (Note) -> Note): Boolean = synchronized(lock) {
        val notes = load()
        val index = notes.indexOfFirst { it.jobId == jobId }
        if (index < 0) false
        else write(notes.toMutableList().apply { this[index] = transform(this[index]) })
    }

    // KTX edit() drops commit()'s result; this write must report failure to callers.
    @Suppress("UseKtx")
    private fun write(notes: List<Note>): Boolean {
        val items = JSONArray()
        notes.forEach {
            items.put(JSONObject().put("title", it.title).put("body", it.body).put("mood", it.mood).put("folder", it.folder).put("time", it.time).put("audioUri", it.audioUri).put("audioSource", it.audioSource.name).put("jobId", it.jobId).put("transcription", it.transcription.name).put("transcriptionError", it.transcriptionError))
        }
        // ponytail: synchronous small-library write; use a database and background writes as the library grows.
        return prefs.edit().putString("items", items.toString()).commit()
    }

    private companion object {
        // WorkManager runs in the app process, so one lock serializes worker and UI read-modify-write cycles.
        val lock = Any()
    }
}
