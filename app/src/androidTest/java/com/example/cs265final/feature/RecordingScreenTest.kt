package com.example.cs265final.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.cs265final.feature.recording.RecordingScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecordingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noteComposerForwardsInputAndActions() {
        var title by mutableStateOf("")
        var transcript by mutableStateOf("")
        var mood by mutableStateOf("สงบ")
        var folder by mutableStateOf("")
        var micClicks = 0
        var savedTitle = ""
        var savedTranscript = ""
        compose.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RecordingScreen(
                    title = title, onTitleChange = { title = it },
                    transcript = transcript, onTranscriptChange = { transcript = it },
                    listening = false, mood = mood, onMoodChange = { mood = it },
                    folder = folder, onFolderChange = { folder = it },
                    onToggleMic = { micClicks++ },
                    onSave = { savedTitle = title; savedTranscript = transcript },
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithText("ชื่อโน้ต").performTextInput("Test note")
        compose.onNodeWithText("พูดได้เลย เราจะช่วยจดให้...").performTextInput("Test transcript")
        compose.onNodeWithContentDescription("เริ่มพูดภาษาไทย").performScrollTo().performClick()
        compose.onNodeWithText("บันทึกโน้ต").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, micClicks)
            assertEquals("Test note", savedTitle)
            assertEquals("Test transcript", savedTranscript)
        }
    }
}
