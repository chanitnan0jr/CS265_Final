package com.example.cs265final.feature;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.cs265final.MainActivity;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioSource;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.NoteStorage;
import com.example.cs265final.data.TranscriptionStatus;
import com.example.cs265final.feature.calendar.CalendarView;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CalendarAndDetailTest {
  private Context context;
  private SharedPreferences prefs;

  @Before
  public void setup() {
    context = ApplicationProvider.getApplicationContext();
    prefs = context.getSharedPreferences(NoteStorage.PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().clear().commit();
  }

  @After
  public void teardown() {
    prefs.edit().clear().commit();
  }

  @Test
  public void calendarFiltersNotesBySelectedDateAndHandlesEmptyDay() {
    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
    Calendar cal = Calendar.getInstance();
    cal.set(2026, Calendar.NOVEMBER, 17, 9, 0, 0);
    long nov17 = cal.getTimeInMillis();
    cal.set(2026, Calendar.NOVEMBER, 18, 9, 0, 0);
    long nov18 = cal.getTimeInMillis();

    Note first = new Note("Day 17", "Text", "ปกติ", "การเรียน", nov17);
    Note second = new Note("Day 18", "Text", "ดี", "ไอเดีย", nov18);

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            CalendarView view = new CalendarView(activity);
            view.bind(Arrays.asList(first, second), format, format.format(new Date(nov17)));
            ScrollView scroll = new ScrollView(activity);
            scroll.addView(view);
            activity.setContentView(scroll);

            LinearLayout notesContainer = view.findViewById(R.id.calendar_notes_container);
            assertEquals(1, notesContainer.getChildCount());
          });
      onView(withText("Day 17")).check(matches(isDisplayed()));

      scenario.onActivity(
          activity -> {
            CalendarView view = new CalendarView(activity);
            cal.set(2026, Calendar.NOVEMBER, 20, 9, 0, 0);
            view.bind(
                Arrays.asList(first, second), format, format.format(cal.getTime()));
            ScrollView scroll = new ScrollView(activity);
            scroll.addView(view);
            activity.setContentView(scroll);
          });
      onView(withText(R.string.calendar_empty)).check(matches(isDisplayed()));
    }
  }

  @Test
  public void detailRenameValidatesBlankTitleAndRetriesFailedTranscription() {
    Note failed =
        new Note(
            "id_fail",
            "Failed Audio",
            "",
            "ปกติ",
            "ไฟล์ที่อัปโหลด",
            System.currentTimeMillis(),
            "content://audio/9",
            AudioSource.UPLOADED,
            "job_fail",
            TranscriptionStatus.FAILED,
            "Server error");
    new NoteStorage(context).save(Collections.singletonList(failed));

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onView(withText("Failed Audio")).perform(click());
      onView(withId(R.id.text_detail_transcription_error))
          .check(matches(withText("Server error")));

      // Inline rename rejects blank input and succeeds on valid title
      onView(withId(R.id.button_detail_edit)).perform(scrollTo(), click());
      onView(withId(R.id.input_detail_rename)).perform(scrollTo(), replaceText("   "));
      onView(withId(R.id.button_detail_rename_save)).perform(scrollTo(), click());
      onView(withId(R.id.text_detail_rename_error))
          .check(matches(withText(R.string.detail_rename_empty_error)));

      onView(withId(R.id.input_detail_rename)).perform(scrollTo(), replaceText("Renamed Audio"));
      onView(withId(R.id.button_detail_rename_save)).perform(scrollTo(), click());
      onView(withId(R.id.text_detail_title)).check(matches(withText("Renamed Audio")));

      // Retry assigns a new job ID and dispatches WorkManager
      onView(withId(R.id.button_detail_retry)).perform(scrollTo(), click());
      try {
        List<Note> updated = new NoteStorage(context).load();
        assertEquals(1, updated.size());
        assertFalse("job_fail".equals(updated.get(0).getJobId()));
      } catch (JSONException e) {
        throw new AssertionError(e);
      }
    }
  }
}
