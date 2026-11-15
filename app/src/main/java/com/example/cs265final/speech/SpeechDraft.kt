package com.example.cs265final.speech

/** Keeps recognition results scoped to the session that produced them. */
class SpeechDraft {
    private var generation = 0
    private var prefix = ""

    fun start(existingText: String): Int {
        prefix = existingText.trimEnd()
        return ++generation
    }

    fun isCurrent(session: Int): Boolean = session == generation

    fun result(session: Int, recognized: String): String? =
        if (isCurrent(session)) listOf(prefix, recognized).filter(String::isNotBlank).joinToString(" ") else null

    fun cancel() { generation++ }
}
