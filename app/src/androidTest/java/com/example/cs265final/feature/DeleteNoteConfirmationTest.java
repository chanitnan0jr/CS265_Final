package com.example.cs265final.feature;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.cs265final.MainActivity;
import com.example.cs265final.R;
import com.example.cs265final.feature.notes.DeleteNoteConfirmation;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeleteNoteConfirmationTest {
  @Test
  public void cancelConfirmAndBackCallOnlyTheirRespectiveActions() throws Exception {
    AtomicInteger confirmed = new AtomicInteger();
    AtomicInteger cancelled = new AtomicInteger();
    AtomicReference<AlertDialog> dialogRef = new AtomicReference<>();
    final CountDownLatch[] dismissLatch = new CountDownLatch[] {new CountDownLatch(1)};

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      final String[] labels = new String[2];
      scenario.onActivity(
          activity -> {
            labels[0] = activity.getString(R.string.delete_note_cancel);
            labels[1] = activity.getString(R.string.delete_note_confirm);
            AlertDialog dialog =
                DeleteNoteConfirmation.show(
                    activity, "First", confirmed::incrementAndGet, cancelled::incrementAndGet);
            dialog.setOnDismissListener(ignored -> dismissLatch[0].countDown());
            dialogRef.set(dialog);
          });

      waitForDialogFocus(dialogRef.get());
      onView(withText(labels[0])).inRoot(isDialog()).perform(click());
      assertTrue(dismissLatch[0].await(2, TimeUnit.SECONDS));

      scenario.onActivity(
          activity -> {
            assertEquals(0, confirmed.get());
            assertEquals(1, cancelled.get());
            dismissLatch[0] = new CountDownLatch(1);
            AlertDialog dialog =
                DeleteNoteConfirmation.show(
                    activity, "First", confirmed::incrementAndGet, cancelled::incrementAndGet);
            dialog.setOnDismissListener(ignored -> dismissLatch[0].countDown());
            dialogRef.set(dialog);
          });

      waitForDialogFocus(dialogRef.get());
      onView(withText(labels[1])).inRoot(isDialog()).perform(click());
      assertTrue(dismissLatch[0].await(2, TimeUnit.SECONDS));

      scenario.onActivity(
          activity -> {
            assertEquals(1, confirmed.get());
            assertEquals(1, cancelled.get());
            dismissLatch[0] = new CountDownLatch(1);
            AlertDialog dialog =
                DeleteNoteConfirmation.show(
                    activity, "First", confirmed::incrementAndGet, cancelled::incrementAndGet);
            dialog.setOnDismissListener(ignored -> dismissLatch[0].countDown());
            dialogRef.set(dialog);
          });

      waitForDialogFocus(dialogRef.get());
      pressBack();
      assertTrue(dismissLatch[0].await(2, TimeUnit.SECONDS));

      scenario.onActivity(
          activity -> {
            assertEquals(1, confirmed.get());
            assertEquals(2, cancelled.get());
          });
    }
  }

  private static void waitForDialogFocus(AlertDialog dialog) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 3_000L;
    while (System.currentTimeMillis() < deadline) {
      final boolean[] ready = new boolean[1];
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(
              () -> {
                View decor = dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null;
                ready[0] = dialog.isShowing() && decor != null && decor.hasWindowFocus();
              });
      if (ready[0]) {
        return;
      }
      Thread.sleep(50);
    }
  }
}
