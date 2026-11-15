package com.example.cs265final.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechDraftTest {
    @Test fun resultsReplacePartialsAndIgnoreCancelledSessions() {
        val draft = SpeechDraft()
        val first = draft.start("Existing note  ")
        assertEquals("Existing note hello", draft.result(first, "hello"))
        assertEquals("Existing note hello again", draft.result(first, "hello again"))
        draft.cancel()
        assertNull(draft.result(first, "late result"))
        val second = draft.start("Existing note hello again")
        assertTrue(draft.isCurrent(second))
        assertNull(draft.result(first, "older result"))
        assertEquals("Existing note hello again next", draft.result(second, "next"))
        assertEquals("Existing note hello again", draft.result(second, ""))
    }
}
