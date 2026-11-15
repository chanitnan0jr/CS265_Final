package com.example.cs265final.transcription

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cs265final.BuildConfig
import com.example.cs265final.data.NoteStorage
import com.example.cs265final.data.TranscriptionStatus
import com.example.cs265final.data.contentDisplayName
import com.example.cs265final.data.isPending
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Uploads an audio note and polls the transcription API; the note is updated in storage. */
class TranscriptionWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID)?.takeIf { it.isNotBlank() } ?: return Result.failure()
        val storage = NoteStorage(applicationContext)
        val note = runCatching { storage.findNote(jobId) }.getOrNull() ?: return Result.success()
        if (!note.transcription.isPending()) return Result.success()
        if (note.audioUri.isBlank()) return fail(storage, jobId, "ไม่พบไฟล์เสียงสำหรับถอดข้อความ")

        val baseUrl = BuildConfig.TRANSCRIPTION_BASE_URL.trim()
        if (baseUrl.isEmpty()) return fail(storage, jobId, "ยังไม่ได้ตั้งค่าเซิร์ฟเวอร์ถอดข้อความ")

        val marked = runCatching {
            storage.updateTranscription(jobId) { it.copy(transcription = TranscriptionStatus.PROCESSING, transcriptionError = "") }
        }.getOrDefault(false)
        if (!marked) return Result.success()

        return try {
            val response = transcribe(baseUrl, note.audioUri)
            if (response.text.isBlank()) return fail(storage, jobId, "ไม่พบข้อความในไฟล์เสียง")
            val saved = runCatching {
                storage.updateTranscription(jobId) {
                    it.copy(body = mergeTranscript(it.body, response.text), transcription = TranscriptionStatus.COMPLETED, transcriptionError = "")
                }
            }.getOrDefault(false)
            if (saved) Result.success()
            else if (runAttemptCount < MAX_ATTEMPTS) Result.retry()
            else fail(storage, jobId, "ไม่สามารถบันทึกข้อความได้")
        } catch (e: CancellationException) {
            throw e
        } catch (e: TranscriptionException) {
            Log.w(TAG, "Transcription failed (retryable=${e.retryable})")
            if (e.retryable && runAttemptCount < MAX_ATTEMPTS) Result.retry()
            else fail(storage, jobId, e.message ?: "ถอดข้อความไม่สำเร็จ")
        } catch (_: SecurityException) {
            Log.w(TAG, "Transcription cannot read the audio file")
            fail(storage, jobId, "ไม่สามารถเข้าถึงไฟล์เสียงได้ กรุณาเลือกไฟล์อีกครั้ง")
        } catch (_: IOException) {
            Log.w(TAG, "Transcription network error")
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry()
            else fail(storage, jobId, "เชื่อมต่อเซิร์ฟเวอร์ไม่ได้")
        } catch (e: Exception) {
            Log.w(TAG, "Transcription error: ${e.javaClass.simpleName}")
            fail(storage, jobId, "ถอดข้อความไม่สำเร็จ")
        }
    }

    private suspend fun transcribe(baseUrl: String, audioUri: String): TranscriptionResponse = withContext<TranscriptionResponse>(Dispatchers.IO) {
        val uri = audioUri.toUri()
        val resolver = applicationContext.contentResolver
        val fileName = contentDisplayName(resolver, uri) ?: "audio"
        val mimeType = resolver.getType(uri) ?: TranscriptionClient.mimeTypeFor(fileName)
        val input = try {
            resolver.openInputStream(uri)
        } catch (_: FileNotFoundException) {
            throw TranscriptionException("ไม่พบไฟล์เสียงหรือไฟล์ถูกย้ายแล้ว", retryable = false)
        } catch (_: SecurityException) {
            throw TranscriptionException("ไม่สามารถเข้าถึงไฟล์เสียงได้ กรุณาเลือกไฟล์อีกครั้ง", retryable = false)
        }
        val remoteJob = input?.use { stream ->
            TranscriptionClient.submit(baseUrl, fileName, mimeType, "th", stream)
        } ?: throw TranscriptionException("ไม่สามารถเปิดไฟล์เสียงได้", retryable = false)

        var waited = 0L
        while (waited <= MAX_POLL_MS) {
            if (isStopped) throw TranscriptionException("งานถูกหยุดชั่วคราว", retryable = true)
            val response = TranscriptionClient.poll(baseUrl, remoteJob)
            when (response.status) {
                "completed" -> return@withContext response
                "failed" -> throw TranscriptionException(response.error.ifBlank { "เซิร์ฟเวอร์ถอดข้อความไม่สำเร็จ" }, retryable = false)
                "queued", "processing" -> Unit
                else -> throw TranscriptionException("สถานะงานจากเซิร์ฟเวอร์ไม่ถูกต้อง", retryable = false)
            }
            delay(POLL_INTERVAL_MS.milliseconds)
            waited += POLL_INTERVAL_MS
        }
        throw TranscriptionException("ใช้เวลาถอดข้อความนานเกินไป", retryable = false)
    }

    private fun fail(storage: NoteStorage, jobId: String, message: String): Result {
        runCatching {
            storage.updateTranscription(jobId) { it.copy(transcription = TranscriptionStatus.FAILED, transcriptionError = message) }
        }
        return Result.failure()
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        private const val TAG = "Voice2NoteTranscription"
        private const val MAX_ATTEMPTS = 3
        private const val POLL_INTERVAL_MS = 2_000L
        private const val MAX_POLL_MS = 5 * 60_000L
    }
}

/** A fresh upload gets the transcript; an existing typed body is kept. */
internal fun mergeTranscript(body: String, transcript: String): String =
    if (body.isBlank()) transcript else body.trimEnd() + "\n\n" + transcript
