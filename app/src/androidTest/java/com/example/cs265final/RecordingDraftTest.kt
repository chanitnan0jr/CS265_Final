package com.example.cs265final

import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingDraftTest {
    @Test fun capturedAudioSurvivesActivityRecreation() {
        val pcm = byteArrayOf(1, 2, 3, 4)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { ViewModelProvider(it)[RecordingDraft::class.java].audioBuffer = ByteArrayOutputStream().apply { write(pcm) } }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertArrayEquals(pcm, ViewModelProvider(activity)[RecordingDraft::class.java].audioBuffer?.toByteArray())
            }
        }
    }
}
