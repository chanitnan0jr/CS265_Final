package com.example.cs265final.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Instrumentation;
import android.net.Uri;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.cs265final.MainActivity;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioSource;
import com.example.cs265final.data.AudioUtils;
import com.example.cs265final.data.Note;
import com.example.cs265final.feature.notes.NoteDetailView;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NoteDetailLifecycleTest {
  private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
  private final File file =
      new File(instrumentation.getTargetContext().getCacheDir(), "detail-lifecycle-test.mp3");

  @After
  public void cleanup() {
    file.delete();
  }

  @Test
  public void backgroundPausesPlaybackUntilTheUserResumes() throws Exception {
    ByteBuffer pcm = ByteBuffer.allocate(16_000 * 10 * 2).order(ByteOrder.LITTLE_ENDIAN);
    for (int sample = 0; sample < 16_000 * 10; sample++) {
      short val = (short) (Math.sin(2.0 * Math.PI * 440.0 * sample / 16_000.0) * 16_000.0);
      pcm.putShort(val);
    }
    AudioUtils.writeMp3File(file, pcm.array());

    final LifecycleRegistry[] registryHolder = new LifecycleRegistry[1];
    LifecycleOwner owner =
        new LifecycleOwner() {
          final LifecycleRegistry registry = new LifecycleRegistry(this);

          {
            registryHolder[0] = registry;
          }

          @NonNull
          @Override
          public Lifecycle getLifecycle() {
            return registry;
          }
        };

    AtomicReference<NoteDetailView> viewRef = new AtomicReference<>();
    Note note =
        new Note(
            "Audio",
            "",
            "ปกติ",
            "การเรียน",
            1L,
            Uri.fromFile(file).toString(),
            AudioSource.RECORDED);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            registryHolder[0].setCurrentState(Lifecycle.State.RESUMED);
            NoteDetailView view = new NoteDetailView(activity);
            view.bindLifecycle(owner.getLifecycle());
            view.bind(note, new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT));
            viewRef.set(view);
            ScrollView scroll = new ScrollView(activity);
            scroll.addView(view);
            activity.setContentView(scroll);
          });

      waitUntil(
          10_000L,
          () -> {
            TextView timer = viewRef.get().findViewById(R.id.text_detail_timer);
            return !"0:00".equals(timer.getText().toString());
          });

      scenario.onActivity(
          activity -> {
            Button listen = viewRef.get().findViewById(R.id.button_detail_listen);
            listen.performClick();
            assertEquals(
                activity.getString(R.string.detail_stop_listening), listen.getText().toString());
          });

      waitUntil(
          5_000L,
          () -> {
            TextView timer = viewRef.get().findViewById(R.id.text_detail_timer);
            return "0:01".equals(timer.getText().toString());
          });

      scenario.onActivity(
          activity -> {
            registryHolder[0].setCurrentState(Lifecycle.State.CREATED);
            Button listen = viewRef.get().findViewById(R.id.button_detail_listen);
            TextView timer = viewRef.get().findViewById(R.id.text_detail_timer);
            assertEquals(
                activity.getString(R.string.detail_listen_original), listen.getText().toString());
            assertFalse("0:00".equals(timer.getText().toString()));
          });

      scenario.onActivity(
          activity -> {
            registryHolder[0].setCurrentState(Lifecycle.State.RESUMED);
            Button listen = viewRef.get().findViewById(R.id.button_detail_listen);
            listen.performClick();
            assertEquals(
                activity.getString(R.string.detail_stop_listening), listen.getText().toString());
          });

      waitUntil(
          5_000L,
          () -> {
            TextView timer = viewRef.get().findViewById(R.id.text_detail_timer);
            return "0:02".equals(timer.getText().toString());
          });
    }
  }

  @Test
  public void reattachingViewStillPausesPlaybackOnBackground() throws Exception {
    ByteBuffer pcm = ByteBuffer.allocate(16_000 * 6 * 2).order(ByteOrder.LITTLE_ENDIAN);
    for (int sample = 0; sample < 16_000 * 6; sample++) {
      short val = (short) (Math.sin(2.0 * Math.PI * 440.0 * sample / 16_000.0) * 16_000.0);
      pcm.putShort(val);
    }
    AudioUtils.writeMp3File(file, pcm.array());

    final LifecycleRegistry[] registryHolder = new LifecycleRegistry[1];
    LifecycleOwner owner =
        new LifecycleOwner() {
          final LifecycleRegistry registry = new LifecycleRegistry(this);

          {
            registryHolder[0] = registry;
          }

          @NonNull
          @Override
          public Lifecycle getLifecycle() {
            return registry;
          }
        };

    AtomicReference<NoteDetailView> viewRef = new AtomicReference<>();
    Note note =
        new Note(
            "Audio Reattach",
            "",
            "ปกติ",
            "การเรียน",
            2L,
            Uri.fromFile(file).toString(),
            AudioSource.RECORDED);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            registryHolder[0].setCurrentState(Lifecycle.State.RESUMED);
            NoteDetailView view = new NoteDetailView(activity);
            view.bindLifecycle(owner.getLifecycle());
            ScrollView scroll = new ScrollView(activity);
            scroll.addView(view);
            activity.setContentView(scroll);
            viewRef.set(view);

            // Detach and reattach the same view, mimicking tab/detail reuse in MainActivity.
            scroll.removeView(view);
            scroll.addView(view);
            view.bind(note, new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT));
          });

      waitUntil(
          10_000L,
          () -> {
            TextView timer = viewRef.get().findViewById(R.id.text_detail_timer);
            return !"0:00".equals(timer.getText().toString());
          });

      scenario.onActivity(
          activity -> {
            Button listen = viewRef.get().findViewById(R.id.button_detail_listen);
            listen.performClick();
            assertEquals(
                activity.getString(R.string.detail_stop_listening), listen.getText().toString());
          });

      waitUntil(
          5_000L,
          () -> {
            TextView timer = viewRef.get().findViewById(R.id.text_detail_timer);
            return "0:01".equals(timer.getText().toString());
          });

      scenario.onActivity(
          activity -> {
            registryHolder[0].setCurrentState(Lifecycle.State.CREATED);
            Button listen = viewRef.get().findViewById(R.id.button_detail_listen);
            assertEquals(
                activity.getString(R.string.detail_listen_original), listen.getText().toString());
          });
    }
  }

  private void waitUntil(long timeoutMs, Callable<Boolean> condition) throws InterruptedException {
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
