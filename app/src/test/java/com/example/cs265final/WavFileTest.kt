package com.example.cs265final

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

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
    @Test fun writesMp3FrameStreamFromPcm() {
        val file = kotlin.io.path.createTempFile("voice2note", ".mp3").toFile()
        try {
            val samples = 16_000
            val pcm = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until samples) {
                val sample = (sin(2.0 * PI * 440.0 * i / 16_000.0) * 16_000.0).toInt().toShort()
                pcm.putShort(sample)
            }
            val bytes = writeMp3File(file, pcm.array()).readBytes()
            assertTrue("MP3 output should not be empty", bytes.size > 1_000)
            assertEquals(0xFF, bytes[0].toInt() and 0xFF)
            assertEquals(0xE0, bytes[1].toInt() and 0xE0)
        } finally {
            file.delete()
        }
    }
    @Test fun onlyManagedRecordingsAreEligibleForDeletion() {
        val directory = kotlin.io.path.createTempDirectory("voice2note").toFile()
        try {
            assertTrue(isManagedRecording(File(directory, "note_123.mp3"), directory))
            assertTrue(isManagedRecording(File(directory, "note_123.wav"), directory))
            assertFalse(isManagedRecording(File(directory, "other.mp3"), directory))
            assertFalse(isManagedRecording(File(directory, "other.wav"), directory))
            assertFalse(isManagedRecording(File(directory, "../note_outside.mp3"), directory))
            assertFalse(isManagedRecording(File(directory, "../note_outside.wav"), directory))
        } finally {
            directory.delete()
        }
    }
    @Test fun detectsAudibleNonZeroPcm() {
        assertFalse(hasNonZeroPcm(ByteArray(32)))
        assertTrue(hasNonZeroPcm(byteArrayOf(0, 1)))
    }
    @Test fun resamplesMonoPcmTo16kHz() {
        val pcm = ByteBuffer.allocate(800 * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(800) { pcm.putShort((10_000 * sin(2 * PI * 1_000 * it / 8_000)).toInt().toShort()) }
        val result = resampleMonoPcm16(pcm.array(), 8_000, 16_000)
        assertEquals(1_600 * 2, result.size)
        val samples = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        for (i in 40 until 1_560) {
            val expected = 10_000 * sin(2 * PI * 1_000 * i / 16_000)
            assertTrue("Sample $i deviates from a smooth tone", kotlin.math.abs(samples.get(i) - expected) < 100)
        }
    }
}
