package com.example.cs265final.transcription

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import org.json.JSONObject

/** [retryable] is false for bad input or server rejections, where re-uploading cannot help. */
internal class TranscriptionException(message: String, val retryable: Boolean) : Exception(message)

internal data class TranscriptionResponse(val status: String, val text: String = "", val error: String = "")

/** Talks to the transcription API in Voice2Note_Pack/Roadmap-Plan.md (section 4.4). */
internal object TranscriptionClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val MAX_RESPONSE_BYTES = 1_000_000

    fun submit(baseUrl: String, fileName: String, mimeType: String, language: String, audio: InputStream): String {
        val boundary = "Voice2Note-${UUID.randomUUID()}"
        val connection = open("${normalizeBaseUrl(baseUrl)}/api/v1/transcriptions") {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setChunkedStreamingMode(16_384)
        }
        try {
            connection.outputStream.buffered().use { out ->
                writeField(out, boundary, "language", language)
                writeFile(out, boundary, "file", fileName, mimeType, audio)
                out.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_ACCEPTED) {
                return JSONObject(body).optText("jobId").takeIf { it.isNotBlank() }
                    ?: throw TranscriptionException("เซิร์ฟเวอร์ไม่ได้ส่งรหัสงานกลับมา", retryable = false)
            }
            throw httpError(code)
        } catch (e: TranscriptionException) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw TranscriptionException("รูปแบบข้อมูลจากเซิร์ฟเวอร์ไม่ถูกต้อง", retryable = false)
        } finally {
            connection.disconnect()
        }
    }

    fun poll(baseUrl: String, jobId: String): TranscriptionResponse {
        val encodedJob = URLEncoder.encode(jobId, Charsets.UTF_8.name()).replace("+", "%20")
        val connection = open("${normalizeBaseUrl(baseUrl)}/api/v1/transcriptions/$encodedJob") {
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(body)
                return TranscriptionResponse(
                    status = json.optText("status").lowercase(Locale.ROOT),
                    text = json.optText("text"),
                    error = json.optText("error"),
                )
            }
            throw httpError(code)
        } catch (e: TranscriptionException) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw TranscriptionException("รูปแบบข้อมูลจากเซิร์ฟเวอร์ไม่ถูกต้อง", retryable = false)
        } finally {
            connection.disconnect()
        }
    }

    internal fun mimeTypeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "wav" -> "audio/wav"
        "aac" -> "audio/aac"
        "ogg", "opus" -> "audio/ogg"
        else -> "application/octet-stream"
    }

    private fun normalizeBaseUrl(value: String): String {
        val base = value.trim().trimEnd('/')
        if (!base.startsWith("https://") && !base.startsWith("http://")) {
            throw TranscriptionException("ยังไม่ได้ตั้งค่าเซิร์ฟเวอร์ถอดข้อความ", retryable = false)
        }
        return base
    }

    private fun open(url: String, configure: HttpURLConnection.() -> Unit = {}): HttpURLConnection {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw TranscriptionException("ที่อยู่เซิร์ฟเวอร์ไม่ถูกต้อง", retryable = false)
        }
        return connection.apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            configure()
        }
    }

    private fun writeField(out: OutputStream, boundary: String, name: String, value: String) {
        out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray(Charsets.UTF_8))
    }

    private fun writeFile(out: OutputStream, boundary: String, name: String, fileName: String, mimeType: String, audio: InputStream) {
        val safeName = fileName.replace("\\", "_").replace("\"", "_").replace("\r", " ").replace("\n", " ").ifBlank { "audio" }
        val safeMime = mimeType.replace("\r", "").replace("\n", "").ifBlank { "application/octet-stream" }
        out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; filename=\"$safeName\"\r\nContent-Type: $safeMime\r\n\r\n".toByteArray(Charsets.UTF_8))
        audio.copyTo(out)
        out.write("\r\n".toByteArray(Charsets.UTF_8))
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = (if (code in 200..299) connection.inputStream else connection.errorStream) ?: return ""
        stream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8_192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_RESPONSE_BYTES) throw TranscriptionException("ข้อมูลจากเซิร์ฟเวอร์ยาวเกินไป", retryable = false)
                out.write(buffer, 0, read)
            }
            return out.toString(Charsets.UTF_8.name())
        }
    }

    private fun httpError(code: Int): TranscriptionException {
        val message = when {
            code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN -> "เซิร์ฟเวอร์ปฏิเสธการเข้าถึง ($code)"
            code == 413 -> "ไฟล์เสียงใหญ่เกินไป ($code)"
            code in 400..499 -> "เซิร์ฟเวอร์ปฏิเสธคำขอ ($code)"
            else -> "เซิร์ฟเวอร์ขัดข้อง ($code)"
        }
        // 429 is temporary; 4xx input/job errors cannot be fixed by re-uploading.
        return TranscriptionException(message, retryable = code >= 500 || code == HttpURLConnection.HTTP_CLIENT_TIMEOUT || code == 429)
    }

    private fun JSONObject.optText(key: String): String = if (isNull(key)) "" else optString(key)
}
