package com.example.cs265final.transcription

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionTextTest {
    @Test fun aFreshUploadGetsTheTranscript() {
        assertEquals("ข้อความ", mergeTranscript("", "ข้อความ"))
    }

    @Test fun aTypedBodyIsKeptAndTheTranscriptIsAppended() {
        assertEquals("โน้ตที่พิมพ์ไว้\n\nข้อความ", mergeTranscript("โน้ตที่พิมพ์ไว้\n", "ข้อความ"))
    }
}
