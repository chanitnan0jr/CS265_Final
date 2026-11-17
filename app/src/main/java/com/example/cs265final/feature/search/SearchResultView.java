package com.example.cs265final.feature.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.cs265final.R;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.TranscriptionStatus;
import com.example.cs265final.ui.UiLabels;
import java.util.function.Consumer;

/** Displays a search match or note row; the host owns navigation and persistence. */
public final class SearchResultView extends LinearLayout {
  private final TextView title;
  private final TextView body;
  private final TextView metadata;
  private final View options;
  private final View progress;
  private Note note;
  private Consumer<Note> onSelect;
  private Consumer<Note> onDelete;
  private Consumer<Note> onMoveFolder;

  public SearchResultView(Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_search_result, this, true);
    View card = findViewById(R.id.search_result_card);
    title = findViewById(R.id.text_search_result_title);
    body = findViewById(R.id.text_search_result_body);
    metadata = findViewById(R.id.text_search_result_metadata);
    options = findViewById(R.id.button_search_result_options);
    progress = findViewById(R.id.progress_search_result);
    card.setOnClickListener(view -> {
      if (note != null && onSelect != null) {
        onSelect.accept(note);
      }
    });
    options.setOnClickListener(view -> {
      if (note == null || (onDelete == null && onMoveFolder == null)) {
        return;
      }
      Note target = note;
      PopupMenu menu = new PopupMenu(getContext(), options);
      if (onMoveFolder != null) {
        menu.getMenu().add(R.string.note_move_folder_action).setOnMenuItemClickListener(item -> {
          onMoveFolder.accept(target);
          return true;
        });
      }
      if (onDelete != null) {
        menu.getMenu().add(R.string.note_delete_action).setOnMenuItemClickListener(item -> {
          // Keep the original target if the list is rebound while the menu is open.
          onDelete.accept(target);
          return true;
        });
      }
      menu.show();
    });
  }

  public void setOnSelect(Consumer<Note> listener) {
    onSelect = listener;
  }

  public void setOnDelete(Consumer<Note> listener) {
    onDelete = listener;
  }

  public void setOnMoveFolder(Consumer<Note> listener) {
    onMoveFolder = listener;
  }

  /** Binds the original note instance, including current worker status. */
  public void bind(Note value, String date) {
    note = value;
    title.setText(value.getTitle());
    String folderDisplay = UiLabels.folderLabel(getContext(), value.getFolder());
    metadata.setText(getContext().getString(R.string.note_date_folder, date, folderDisplay));
    options.setContentDescription(getContext().getString(R.string.note_options, value.getTitle()));
    TranscriptionStatus status = value.getTranscription();
    int statusText = 0;
    if (status == TranscriptionStatus.QUEUED) {
      statusText = R.string.note_status_queued;
    } else if (status == TranscriptionStatus.PROCESSING) {
      statusText = R.string.note_status_processing;
    } else if (status == TranscriptionStatus.FAILED) {
      statusText = R.string.note_status_failed;
    }
    if (statusText != 0) {
      body.setText(statusText);
      body.setTextColor(ContextCompat.getColor(getContext(), status == TranscriptionStatus.FAILED
          ? R.color.search_result_failed : R.color.search_result_status));
      body.setVisibility(VISIBLE);
    } else {
      body.setText(value.getBody());
      body.setTextColor(ContextCompat.getColor(getContext(), R.color.search_result_secondary));
      body.setVisibility(isBlank(value.getBody()) ? GONE : VISIBLE);
    }
    progress.setVisibility(status == TranscriptionStatus.QUEUED
        || status == TranscriptionStatus.PROCESSING ? VISIBLE : GONE);
  }

  private static boolean isBlank(String value) {
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      if (!Character.isWhitespace(character) && !Character.isSpaceChar(character)) {
        return false;
      }
    }
    return true;
  }
}
