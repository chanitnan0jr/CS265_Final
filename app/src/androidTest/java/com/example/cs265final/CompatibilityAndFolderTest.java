package com.example.cs265final;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.cs265final.data.FolderStorage;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.NoteStorage;
import com.example.cs265final.ui.UiLabels;
import java.util.Collections;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CompatibilityAndFolderTest {
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
  public void thaiAndEnglishResourcesResolveDistinctLabels() {
    Configuration thConfig = new Configuration(context.getResources().getConfiguration());
    thConfig.setLocale(Locale.forLanguageTag("th"));
    Context thContext = context.createConfigurationContext(thConfig);

    Configuration enConfig = new Configuration(context.getResources().getConfiguration());
    enConfig.setLocale(Locale.ENGLISH);
    Context enContext = context.createConfigurationContext(enConfig);

    assertEquals("เริ่มบันทึกเสียง", thContext.getString(R.string.home_record));
    assertEquals("Start recording", enContext.getString(R.string.home_record));
    assertEquals("คลังโน้ต", thContext.getString(R.string.nav_library));
    assertEquals("Library", enContext.getString(R.string.nav_library));
    assertEquals("การเรียน", UiLabels.folderLabel(thContext, "การเรียน"));
    assertEquals("Study", UiLabels.folderLabel(enContext, "การเรียน"));
  }

  @Test
  public void dayAndNightThemesResolveReadableContrastColors() {
    Configuration dayConfig = new Configuration(context.getResources().getConfiguration());
    dayConfig.uiMode =
        (dayConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
    Context dayContext = context.createConfigurationContext(dayConfig);

    Configuration nightConfig = new Configuration(context.getResources().getConfiguration());
    nightConfig.uiMode =
        (nightConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
    Context nightContext = context.createConfigurationContext(nightConfig);

    int dayBg = ContextCompat.getColor(dayContext, R.color.app_background);
    int nightBg = ContextCompat.getColor(nightContext, R.color.app_background);
    int dayText = ContextCompat.getColor(dayContext, R.color.text_primary);
    int nightText = ContextCompat.getColor(nightContext, R.color.text_primary);

    assertNotEquals(dayBg, nightBg);
    assertNotEquals(dayText, nightText);
    assertNotEquals(dayBg, dayText);
    assertNotEquals(nightBg, nightText);
  }

  @Test
  public void folderRenameAndDeleteConfirmationPreserveOrMoveNotes() throws Exception {
    Note seeded = new Note("Lecture 1", "Summary", "ปกติ", "Podcasts", System.currentTimeMillis());
    new NoteStorage(context).save(Collections.singletonList(seeded));
    new FolderStorage(context).save(Collections.singletonList("Podcasts"));

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onView(withId(R.id.nav_library)).perform(click());
      onView(withText("Podcasts")).check(matches(isDisplayed()));
      onView(withText("Lecture 1")).perform(scrollTo()).check(matches(isDisplayed()));

      // Rename folder -> updates card and assigned note
      String optionsDesc = context.getString(R.string.folder_options_desc, "Podcasts");
      onView(withContentDescription(optionsDesc)).perform(scrollTo(), click());
      onView(withText(R.string.folder_rename_action)).inRoot(isPlatformPopup()).perform(click());
      onView(withId(R.id.input_folder_name)).inRoot(isDialog()).perform(replaceText("AudioNotes"));
      onView(withText(R.string.folder_save_action)).inRoot(isDialog()).perform(click());
      onView(withText("AudioNotes")).check(matches(isDisplayed()));

      // Delete folder Cancel -> folder remains
      String renamedDesc = context.getString(R.string.folder_options_desc, "AudioNotes");
      onView(withContentDescription(renamedDesc)).perform(scrollTo(), click());
      onView(withText(R.string.folder_delete_action)).inRoot(isPlatformPopup()).perform(click());
      onView(withText(R.string.folder_cancel_action)).inRoot(isDialog()).perform(click());
      onView(withText("AudioNotes")).check(matches(isDisplayed()));

      // Delete folder Confirm -> folder removed and note moved to General
      onView(withContentDescription(renamedDesc)).perform(scrollTo(), click());
      onView(withText(R.string.folder_delete_action)).inRoot(isPlatformPopup()).perform(click());
      onView(withText(R.string.folder_delete_confirm)).inRoot(isDialog()).perform(click());
      onView(withText("AudioNotes")).check(doesNotExist());
      onView(withText("Lecture 1")).perform(scrollTo()).check(matches(isDisplayed()));
    }
  }
}
