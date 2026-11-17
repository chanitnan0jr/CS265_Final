package com.example.cs265final.feature;

import static org.junit.Assert.assertEquals;

import android.widget.EditText;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.cs265final.MainActivity;
import com.example.cs265final.R;
import com.example.cs265final.data.FolderStorage;
import com.example.cs265final.feature.recording.RecordingView;
import com.example.cs265final.ui.UiLabels;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RecordingViewTest {
  @Test
  public void noteComposerForwardsInputAndActions() {
    AtomicReference<String> title = new AtomicReference<>("");
    AtomicReference<String> transcript = new AtomicReference<>("");
    AtomicReference<String> mood = new AtomicReference<>(UiLabels.MOOD_NORMAL);
    AtomicReference<String> folder = new AtomicReference<>("");
    AtomicInteger micClicks = new AtomicInteger();
    AtomicReference<String> savedTitle = new AtomicReference<>("");
    AtomicReference<String> savedTranscript = new AtomicReference<>("");

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            RecordingView view = new RecordingView(activity);
            view.setOnTitleChanged(title::set);
            view.setOnTranscriptChanged(transcript::set);
            view.setOnMoodChanged(mood::set);
            view.setOnFolderChanged(folder::set);
            view.setOnToggleMic(micClicks::incrementAndGet);
            view.setOnSave(
                () -> {
                  savedTitle.set(title.get());
                  savedTranscript.set(transcript.get());
                });
            view.bind(
                "",
                "",
                false,
                UiLabels.MOOD_NORMAL,
                "",
                FolderStorage.DEFAULT_FOLDERS,
                "");

            ScrollView scroll = new ScrollView(activity);
            scroll.addView(view);
            activity.setContentView(scroll);

            ((EditText) view.findViewById(R.id.input_recording_title)).setText("Test note");
            ((EditText) view.findViewById(R.id.input_recording_transcript))
                .setText("Test transcript");
            view.findViewById(R.id.button_recording_mic).performClick();
            view.findViewById(R.id.button_recording_save).performClick();

            assertEquals(1, micClicks.get());
            assertEquals("Test note", savedTitle.get());
            assertEquals("Test transcript", savedTranscript.get());
          });
    }
  }
}
