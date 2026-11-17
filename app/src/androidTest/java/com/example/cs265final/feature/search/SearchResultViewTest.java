package com.example.cs265final.feature.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.cs265final.MainActivity;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioSource;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.TranscriptionStatus;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SearchResultViewTest {
  @Test
  public void selectionAndMenuKeepTheOriginalNoteAcrossRebinding() {
    Note first = new Note("First", "body", "calm", "Work", 1L);
    Note second = new Note("Second", "body", "calm", "Work", 2L);
    AtomicReference<Note> selected = new AtomicReference<>();
    AtomicReference<Note> deleted = new AtomicReference<>();
    AtomicReference<SearchResultView> resultRef = new AtomicReference<>();

    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> {
            SearchResultView view = new SearchResultView(activity);
            view.setOnSelect(selected::set);
            view.setOnDelete(deleted::set);
            view.bind(first, "01/01/1970");
            resultRef.set(view);
            activity.setContentView(view);
          });

      onView(withId(R.id.search_result_card)).perform(click());
      scenario.onActivity(activity -> assertSame(first, selected.get()));
      onView(withId(R.id.button_search_result_options)).perform(click());
      scenario.onActivity(activity -> resultRef.get().bind(second, "01/01/1970"));
      String delete =
          InstrumentationRegistry.getInstrumentation()
              .getTargetContext()
              .getString(R.string.note_delete_action);
      onView(withText(delete)).inRoot(isPlatformPopup()).perform(click());
      scenario.onActivity(activity -> assertSame(first, deleted.get()));
    }
  }

  @Test
  public void bindsTranscriptionStatusAndHidesBlankBody() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              SearchResultView row = new SearchResultView(context);
              TextView body = row.findViewById(R.id.text_search_result_body);
              View progress = row.findViewById(R.id.progress_search_result);
              Note queued =
                  new Note(
                      "id_1",
                      "Audio",
                      "typed",
                      "calm",
                      "Work",
                      1L,
                      "",
                      AudioSource.UNKNOWN,
                      "",
                      TranscriptionStatus.QUEUED,
                      "");
              row.bind(queued, "01/01/1970");
              assertEquals(context.getString(R.string.note_status_queued), body.getText().toString());
              assertEquals(View.VISIBLE, progress.getVisibility());

              row.bind(
                  queued.withTranscription("typed", "", TranscriptionStatus.FAILED, ""),
                  "01/01/1970");
              assertEquals(context.getString(R.string.note_status_failed), body.getText().toString());
              assertEquals(View.GONE, progress.getVisibility());

              row.bind(
                  queued.withTranscription("\u00a0\t", "", TranscriptionStatus.NONE, ""),
                  "01/01/1970");
              assertEquals(View.GONE, body.getVisibility());
            });
  }
}
