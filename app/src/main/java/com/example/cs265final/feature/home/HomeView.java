package com.example.cs265final.feature.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.example.cs265final.R;
import com.example.cs265final.data.Note;
import com.example.cs265final.feature.search.SearchResultView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/** Home actions and recent notes; the activity owns navigation and persistence. */
public final class HomeView extends LinearLayout {
  private static final int RECENT_LIMIT = 3;
  private final View empty;
  private final LinearLayout recentNotes;
  private Runnable onNew;
  private Runnable onUpload;
  private Runnable onViewAll;
  private Consumer<Note> onSelect;
  private Consumer<Note> onDelete;
  private Consumer<Note> onMoveFolder;

  public HomeView(Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_home, this, true);
    empty = findViewById(R.id.text_home_empty);
    recentNotes = findViewById(R.id.home_recent_notes_container);
    findViewById(R.id.button_home_record).setOnClickListener(view -> run(onNew));
    findViewById(R.id.button_home_new_note).setOnClickListener(view -> run(onNew));
    findViewById(R.id.button_home_upload).setOnClickListener(view -> run(onUpload));
    findViewById(R.id.button_home_view_all).setOnClickListener(view -> run(onViewAll));
  }

  public void setOnNew(Runnable action) {
    onNew = action;
  }

  public void setOnUpload(Runnable action) {
    onUpload = action;
  }

  public void setOnViewAll(Runnable action) {
    onViewAll = action;
  }

  public void setOnSelect(Consumer<Note> action) {
    onSelect = action;
  }

  public void setOnDelete(Consumer<Note> action) {
    onDelete = action;
  }

  public void setOnMoveFolder(Consumer<Note> action) {
    onMoveFolder = action;
  }

  /** Keeps the same note instances, including worker status, as the activity's current list. */
  public void bind(List<Note> notes, DateFormat dateFormat) {
    int count = Math.min(notes.size(), RECENT_LIMIT);
    empty.setVisibility(count == 0 ? VISIBLE : GONE);
    while (recentNotes.getChildCount() > count) {
      recentNotes.removeViewAt(recentNotes.getChildCount() - 1);
    }
    while (recentNotes.getChildCount() < count) {
      SearchResultView row = new SearchResultView(getContext());
      row.setOnSelect(note -> {
        if (onSelect != null) {
          onSelect.accept(note);
        }
      });
      row.setOnDelete(note -> {
        if (onDelete != null) {
          onDelete.accept(note);
        }
      });
      row.setOnMoveFolder(note -> {
        if (onMoveFolder != null) {
          onMoveFolder.accept(note);
        }
      });
      LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      params.topMargin = getResources().getDimensionPixelSize(R.dimen.home_note_spacing);
      recentNotes.addView(row, params);
    }
    for (int i = 0; i < count; i++) {
      Note note = notes.get(i);
      ((SearchResultView) recentNotes.getChildAt(i)).bind(
          note, dateFormat.format(new Date(note.getTime())));
    }
  }

  private static void run(Runnable action) {
    if (action != null) {
      action.run();
    }
  }
}
