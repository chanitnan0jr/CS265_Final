package com.example.cs265final.feature.home;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.cs265final.MainActivity;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioSource;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.TranscriptionStatus;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomeViewTest {
  @Test
  public void actionsAndRecentNotesUseCurrentInstances() {
    Note first =
        new Note(
            "id_1",
            "First",
            "body",
            "calm",
            "Work",
            1L,
            "",
            AudioSource.UNKNOWN,
            "",
            TranscriptionStatus.QUEUED,
            "");
    Note second = new Note("Second", "body", "calm", "Home", 2L);
    Note third = new Note("Third", "body", "calm", "Home", 3L);
    Note fourth = new Note("Fourth", "body", "calm", "Home", 4L);
    AtomicInteger newCount = new AtomicInteger();
    AtomicInteger uploadCount = new AtomicInteger();
    AtomicInteger allCount = new AtomicInteger();
    AtomicReference<Note> selected = new AtomicReference<>();
    AtomicReference<Note> deleteRequested = new AtomicReference<>();
    AtomicReference<HomeView> homeRef = new AtomicReference<>();
    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            HomeView home = new HomeView(activity);
            home.setOnNew(newCount::incrementAndGet);
            home.setOnUpload(uploadCount::incrementAndGet);
            home.setOnViewAll(allCount::incrementAndGet);
            home.setOnSelect(selected::set);
            home.setOnDelete(deleteRequested::set);
            home.bind(Arrays.asList(first, second, third, fourth), format);
            homeRef.set(home);

            ScrollView scroll = new ScrollView(activity);
            scroll.addView(home);
            activity.setContentView(scroll);

            home.findViewById(R.id.button_home_record).performClick();
            home.findViewById(R.id.button_home_new_note).performClick();
            home.findViewById(R.id.button_home_upload).performClick();
            home.findViewById(R.id.button_home_view_all).performClick();
            assertEquals(2, newCount.get());
            assertEquals(1, uploadCount.get());
            assertEquals(1, allCount.get());

            LinearLayout recent = home.findViewById(R.id.home_recent_notes_container);
            assertEquals(3, recent.getChildCount());
            assertEquals(View.GONE, home.findViewById(R.id.text_home_empty).getVisibility());
            View row = recent.getChildAt(0);
            assertEquals(
                View.VISIBLE, row.findViewById(R.id.progress_search_result).getVisibility());
            row.findViewById(R.id.search_result_card).performClick();
            assertSame(first, selected.get());

            home.bind(Collections.singletonList(first), format);
          });

      onView(withId(R.id.button_search_result_options)).perform(scrollTo(), click());
      String deleteLabel =
          InstrumentationRegistry.getInstrumentation()
              .getTargetContext()
              .getString(R.string.note_delete_action);
      onView(withText(deleteLabel)).inRoot(isPlatformPopup()).perform(click());

      scenario.onActivity(
          activity -> {
            assertSame(first, deleteRequested.get());
            HomeView home = homeRef.get();
            home.bind(Collections.emptyList(), format);
            assertEquals(View.VISIBLE, home.findViewById(R.id.text_home_empty).getVisibility());
            assertEquals(
                0,
                ((LinearLayout) home.findViewById(R.id.home_recent_notes_container))
                    .getChildCount());
          });
    }
  }

  @Test
  public void rebindingUpdatesTheDisplayedWorkerStatus() {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              HomeView home =
                  new HomeView(InstrumentationRegistry.getInstrumentation().getTargetContext());
              SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
              Note pending =
                  new Note(
                      "id_1",
                      "Audio",
                      "",
                      "calm",
                      "Work",
                      1L,
                      "",
                      AudioSource.UNKNOWN,
                      "",
                      TranscriptionStatus.QUEUED,
                      "");
              home.bind(Collections.singletonList(pending), format);
              home.bind(
                  Collections.singletonList(
                      pending.withTranscription(
                          "Finished", "", TranscriptionStatus.COMPLETED, "")),
                  format);
              LinearLayout recent = home.findViewById(R.id.home_recent_notes_container);
              View row = recent.getChildAt(0);
              assertEquals(
                  "Finished",
                  ((TextView) row.findViewById(R.id.text_search_result_body)).getText().toString());
              assertEquals(
                  View.GONE, row.findViewById(R.id.progress_search_result).getVisibility());
            });
  }
}
