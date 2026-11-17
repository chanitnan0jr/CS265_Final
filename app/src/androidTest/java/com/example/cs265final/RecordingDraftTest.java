package com.example.cs265final;

import static org.junit.Assert.assertArrayEquals;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.cs265final.speech.RecordingDraftViewModel;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RecordingDraftTest {
  @Test
  public void capturedAudioSurvivesActivityRecreation() {
    byte[] pcm = new byte[] {1, 2, 3, 4};
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(pcm, 0, pcm.length);
            new ViewModelProvider(activity).get(RecordingDraftViewModel.class).setAudioBuffer(buffer);
          });
      scenario.recreate();
      scenario.onActivity(
          activity -> {
            ByteArrayOutputStream restored =
                new ViewModelProvider(activity).get(RecordingDraftViewModel.class).getAudioBuffer();
            assertArrayEquals(pcm, restored != null ? restored.toByteArray() : null);
          });
    }
  }
}
