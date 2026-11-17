package com.example.cs265final;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.cs265final.data.NoteStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityFlowsTest {
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
  public void fullNoteCreateSearchDetailDeleteAndBackFlow() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      // Tab navigation -> Back returns to Home (Library & Calendar)
      onView(withId(R.id.nav_library)).perform(click());
      onView(withId(R.id.button_library_new_folder)).check(matches(isDisplayed()));
      pressBack();
      onView(withId(R.id.button_home_record)).check(matches(isDisplayed()));

      onView(withId(R.id.nav_calendar)).perform(click());
      onView(withId(R.id.button_calendar_today)).check(matches(isDisplayed()));
      pressBack();
      onView(withId(R.id.button_home_record)).check(matches(isDisplayed()));

      // Create note flow
      onView(withId(R.id.button_home_new_note)).perform(click());
      onView(withId(R.id.input_recording_title)).perform(scrollTo(), replaceText("CS265 Note"));
      onView(withId(R.id.input_recording_transcript))
          .perform(scrollTo(), replaceText("บทเรียน Android Java XML"));
      onView(withId(R.id.button_recording_save)).perform(scrollTo(), click());

      // Back on Home with recent note visible
      onView(withText("CS265 Note")).check(matches(isDisplayed()));

      // Search tab finds the saved note
      onView(withId(R.id.nav_search)).perform(click());
      onView(withId(R.id.input_search_query)).perform(replaceText("Android"));
      onView(withText("CS265 Note")).check(matches(isDisplayed()));

      // Open detail and cancel delete, then confirm delete
      onView(withText("CS265 Note")).perform(click());
      onView(withId(R.id.text_detail_title)).check(matches(withText("CS265 Note")));
      onView(withId(R.id.button_detail_options)).perform(click());
      onView(withText(R.string.note_delete_action)).perform(click());
      onView(withText(R.string.delete_note_cancel)).inRoot(isDialog()).perform(click());
      onView(withId(R.id.text_detail_title)).check(matches(withText("CS265 Note")));

      onView(withId(R.id.button_detail_options)).perform(click());
      onView(withText(R.string.note_delete_action)).perform(click());
      onView(withText(R.string.delete_note_confirm)).inRoot(isDialog()).perform(click());

      // Deleting from Detail opened via Search returns to Search; Back from Search returns to Home
      onView(withId(R.id.input_search_query)).check(matches(isDisplayed()));
      pressBack();
      onView(withId(R.id.button_home_record)).check(matches(isDisplayed()));

      // Home exit confirmation Cancel stays in app
      pressBack();
      onView(withText(R.string.exit_confirm_title)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withText(R.string.exit_confirm_no)).inRoot(isDialog()).perform(click());
      onView(withId(R.id.nav_home)).check(matches(isDisplayed()));
    }
  }

  @Test
  public void backOnRecordingWithUnsavedDraftAsksConfirmationAndRecreatePreservesDraft() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onView(withId(R.id.button_home_new_note)).perform(click());
      onView(withId(R.id.input_recording_transcript))
          .perform(scrollTo(), replaceText("Unsaved draft text"));

      // Configuration change preserves unsaved draft
      scenario.recreate();
      onView(withId(R.id.input_recording_transcript))
          .check(matches(withText("Unsaved draft text")));

      // Back shows discard draft confirmation; Cancel keeps editing
      pressBack();
      onView(withText(R.string.discard_draft_title)).inRoot(isDialog()).check(matches(isDisplayed()));
      onView(withText(R.string.discard_draft_cancel)).inRoot(isDialog()).perform(click());
      onView(withId(R.id.input_recording_transcript))
          .check(matches(withText("Unsaved draft text")));

      // Confirm discards draft and returns to Home
      pressBack();
      onView(withText(R.string.discard_draft_confirm)).inRoot(isDialog()).perform(click());
      onView(withId(R.id.button_home_record)).check(matches(isDisplayed()));
      assertTrue(true);
    }
  }

  @Test
  public void folderCreationAndScopedSearchFilterMatchingNotes() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      // Create a custom folder from Library
      onView(withId(R.id.nav_library)).perform(click());
      onView(withId(R.id.button_library_new_folder)).perform(click());
      onView(withId(R.id.input_folder_name)).inRoot(isDialog()).perform(replaceText("Exams"));
      onView(withText(R.string.folder_save_action)).inRoot(isDialog()).perform(click());
      onView(withText("Exams")).check(matches(isDisplayed()));

      // Create a note assigned to Exams and another in default Study folder
      onView(withId(R.id.nav_home)).perform(click());
      onView(withId(R.id.button_home_new_note)).perform(click());
      onView(withId(R.id.input_recording_title)).perform(scrollTo(), replaceText("Final Review"));
      onView(withId(R.id.input_recording_transcript))
          .perform(scrollTo(), replaceText("Android lifecycle"));
      onView(withId(R.id.input_recording_folder)).perform(scrollTo(), replaceText("Exams"));
      onView(withId(R.id.button_recording_save)).perform(scrollTo(), click());

      onView(withId(R.id.button_home_new_note)).perform(click());
      onView(withId(R.id.input_recording_title)).perform(scrollTo(), replaceText("Other Note"));
      onView(withId(R.id.input_recording_transcript))
          .perform(scrollTo(), replaceText("Android layout"));
      onView(withId(R.id.button_recording_save)).perform(scrollTo(), click());

      // Search across all folders matches both, then scope to "Exams" matches only Final Review
      onView(withId(R.id.nav_search)).perform(click());
      onView(withId(R.id.input_search_query)).perform(replaceText("Android"));
      onView(withText("Final Review")).check(matches(isDisplayed()));
      onView(withText("Other Note")).check(matches(isDisplayed()));

      onView(withId(R.id.spinner_search_folder)).perform(click());
      onData(allOf(is(instanceOf(String.class)), is("Exams"))).perform(click());
      onView(withText("Final Review")).check(matches(isDisplayed()));
      onView(withText("Other Note")).check(doesNotExist());
    }
  }
}
