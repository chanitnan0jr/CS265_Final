package com.example.cs265final.feature;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.cs265final.MainActivity;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioUtils;
import com.example.cs265final.feature.playback.PlaybackView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PlaybackViewTest {
  private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
  private final File file =
      new File(instrumentation.getTargetContext().getCacheDir(), "playback-test.mp3");

  @After
  public void cleanup() {
    file.delete();
  }

  @Test
  public void mp3CanBePreparedAndPlayed() throws Exception {
    try (InputStream input = instrumentation.getContext().getAssets().open("thai-speech-sample.mp3");
        FileOutputStream output = new FileOutputStream(file)) {
      byte[] buf = new byte[4096];
      int n;
      while ((n = input.read(buf)) >= 0) {
        output.write(buf, 0, n);
      }
    }
    verifyPreparedPlayback();
  }

  @Test
  public void encodedPcmMp3CanBePreparedAndPlayed() throws Exception {
    int samples = 24_000;
    ByteBuffer pcm = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < samples; i++) {
      short sample = (short) (Math.sin(2.0 * Math.PI * 440.0 * i / 16_000.0) * 16_000.0);
      pcm.putShort(sample);
    }
    AudioUtils.writeMp3File(file, pcm.array());
    verifyPreparedPlayback();
  }

  private void verifyPreparedPlayback() throws InterruptedException {
    AtomicReference<PlaybackView> viewRef = new AtomicReference<>();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            PlaybackView view = new PlaybackView(activity);
            view.bind(Uri.fromFile(file));
            viewRef.set(view);
            activity.setContentView(view);
          });

      waitUntil(
          10_000L,
          () -> {
            PlaybackView view = viewRef.get();
            Button toggle = view.findViewById(R.id.button_playback_toggle);
            return toggle.isEnabled();
          });

      scenario.onActivity(
          activity -> {
            Button toggle = viewRef.get().findViewById(R.id.button_playback_toggle);
            toggle.performClick();
            assertEquals(activity.getString(R.string.playback_pause), toggle.getText().toString());
          });

      waitUntil(
          5_000L,
          () -> {
            TextView time = viewRef.get().findViewById(R.id.text_playback_time);
            return time.getText().toString().startsWith("0:01 /");
          });

      scenario.onActivity(
          activity -> {
            Button toggle = viewRef.get().findViewById(R.id.button_playback_toggle);
            toggle.performClick();
            assertEquals(activity.getString(R.string.playback_play), toggle.getText().toString());
          });
    }
  }

  private void waitUntil(long timeoutMs, java.util.concurrent.Callable<Boolean> condition)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      final boolean[] met = new boolean[1];
      instrumentation.runOnMainSync(
          () -> {
            try {
              met[0] = Boolean.TRUE.equals(condition.call());
            } catch (Exception e) {
              met[0] = false;
            }
          });
      if (met[0]) {
        return;
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Condition not met within " + timeoutMs + "ms");
  }
}
