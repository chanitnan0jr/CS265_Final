package com.example.cs265final.feature

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cs265final.feature.playback.PlaybackScreen
import java.io.File
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PlaybackScreenTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = File(instrumentation.targetContext.cacheDir, "playback-test.mp3")

    @After fun cleanup() { file.delete() }

    @Test fun mp3CanBePreparedAndPlayed() {
        instrumentation.context.assets.open("thai-speech-sample.mp3").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        compose.setContent { PlaybackScreen(Uri.fromFile(file), onBack = {}) }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("เล่น")).fetchSemanticsNodes().isNotEmpty()
                && compose.onAllNodes(androidx.compose.ui.test.hasText("กำลังเปิดไฟล์เสียง...")).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("เล่น").performClick()
        compose.onNodeWithText("หยุดชั่วคราว").assertExists()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("0:01 /", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("หยุดชั่วคราว").performClick()
        compose.onNodeWithText("เล่น").assertExists()
    }
}
