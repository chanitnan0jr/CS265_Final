package com.example.cs265final

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WavFileTest {
    @Test fun writesPcmHeaderAndPayload() {
        val file = kotlin.io.path.createTempFile("voice2note", ".wav").toFile()
        try {
            val pcm = byteArrayOf(1, 2, 3, 4)
            val bytes = writeWavFile(file, pcm).readBytes()
            val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
            assertEquals(36 + pcm.size, header.getInt(4))
            assertEquals(16_000, header.getInt(24))
            assertEquals(pcm.size, header.getInt(40))
            assertArrayEquals(pcm, bytes.copyOfRange(44, bytes.size))
        } finally {
            file.delete()
        }
    }
    @Test fun onlyManagedRecordingsAreEligibleForDeletion() {
        val directory = kotlin.io.path.createTempDirectory("voice2note").toFile()
        try {
            assertTrue(isManagedRecording(File(directory, "note_123.wav"), directory))
            assertFalse(isManagedRecording(File(directory, "other.wav"), directory))
            assertFalse(isManagedRecording(File(directory, "note_123.mp3"), directory))
            assertFalse(isManagedRecording(File(directory, "../note_outside.wav"), directory))
        } finally {
            directory.delete()
        }
    }
}
