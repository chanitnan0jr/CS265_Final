package com.example.cs265final.transcription

import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TranscriptionClientTest {
    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private val requestBody = AtomicReference("")
    private val contentType = AtomicReference("")

    @Before fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @After fun stopServer() {
        server.stop(0)
    }

    private fun respond(path: String, status: Int, response: String) {
        server.createContext(path) { exchange ->
            contentType.set(exchange.requestHeaders.getFirst("Content-Type").orEmpty())
            requestBody.set(exchange.requestBody.readBytes().toString(Charsets.UTF_8))
            val bytes = response.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    @Test fun submitsMultipartAudioAndReturnsJobId() {
        respond("/api/v1/transcriptions", 202, """{"jobId":"job_1","status":"queued"}""")
        val audio = "ID3-audio-bytes".toByteArray(Charsets.UTF_8)
        val jobId = TranscriptionClient.submit(baseUrl, "note.mp3", "audio/mpeg", "th", ByteArrayInputStream(audio))

        assertEquals("job_1", jobId)
        assertTrue(contentType.get().startsWith("multipart/form-data; boundary="))
        val body = requestBody.get()
        assertTrue(body.contains("name=\"language\""))
        assertTrue(body.contains("\r\n\r\nth\r\n"))
        assertTrue(body.contains("name=\"file\"; filename=\"note.mp3\""))
        assertTrue(body.contains("Content-Type: audio/mpeg"))
        assertTrue(body.contains("ID3-audio-bytes"))
    }

    @Test fun pollParsesCompletedTranscriptAndNullFields() {
        respond(
            "/api/v1/transcriptions/job_1",
            200,
            """{"jobId":"job_1","status":"completed","text":"สวัสดี","durationSec":4,"segments":[],"error":null}""",
        )
        val response = TranscriptionClient.poll(baseUrl, "job_1")

        assertEquals("completed", response.status)
        assertEquals("สวัสดี", response.text)
        assertEquals("", response.error)
    }

    @Test fun pollParsesFailedJob() {
        respond("/api/v1/transcriptions/job_1", 200, """{"status":"failed","error":"bad audio"}""")
        val response = TranscriptionClient.poll(baseUrl, "job_1")

        assertEquals("failed", response.status)
        assertEquals("bad audio", response.error)
    }

    @Test fun serverErrorsAreRetryable() {
        respond("/api/v1/transcriptions/job_1", 503, """{"error":"down"}""")
        val error = assertThrows(TranscriptionException::class.java) { TranscriptionClient.poll(baseUrl, "job_1") }
        assertTrue(error.retryable)
    }

    @Test fun clientErrorsAreNotRetried() {
        respond("/api/v1/transcriptions/job_1", 400, """{"error":"bad request"}""")
        val error = assertThrows(TranscriptionException::class.java) { TranscriptionClient.poll(baseUrl, "job_1") }
        assertFalse(error.retryable)
    }

    @Test fun missingServerConfigurationIsNotRetried() {
        val error = assertThrows(TranscriptionException::class.java) { TranscriptionClient.poll("", "job_1") }
        assertFalse(error.retryable)
    }

    @Test fun mimeTypeFallsBackToTheFileExtension() {
        assertEquals("audio/mpeg", TranscriptionClient.mimeTypeFor("A.MP3"))
        assertEquals("audio/mp4", TranscriptionClient.mimeTypeFor("voice.m4a"))
        assertEquals("audio/wav", TranscriptionClient.mimeTypeFor("voice.wav"))
        assertEquals("application/octet-stream", TranscriptionClient.mimeTypeFor("voice.flac"))
    }
}
