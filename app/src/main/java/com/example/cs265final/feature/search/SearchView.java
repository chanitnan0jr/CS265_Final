package com.example.cs265final.feature.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.cs265final.R;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.NoteFilters;
import com.example.cs265final.ui.UiLabels;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/** Search screen supporting title/body queries and optional folder scoping. */
public final class SearchView extends LinearLayout {
  private final SearchControls searchControls;
  private final Spinner folderSpinner;
  private final TextView resultCount;
  private final TextView emptyView;
  private final LinearLayout resultsContainer;

  private final List<String> spinnerKeys = new ArrayList<>();
  private boolean updatingSpinner;
  @Nullable private String selectedFolder;
  private String currentQuery = "";
  private Consumer<String> onQueryChanged;
  private Consumer<String> onFolderChanged;
  private Consumer<Note> onSelect;
  private Consumer<Note> onDelete;
  private Consumer<Note> onMoveFolder;

  public SearchView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_search, this, true);
    searchControls = findViewById(R.id.search_controls);
    folderSpinner = findViewById(R.id.spinner_search_folder);
    resultCount = findViewById(R.id.text_search_result_count);
    emptyView = findViewById(R.id.text_search_empty);
    resultsContainer = findViewById(R.id.search_results_container);

    searchControls.setOnQueryChanged(
        value -> {
          currentQuery = value;
          if (onQueryChanged != null) {
            onQueryChanged.accept(value);
          }
        });

    folderSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(
              AdapterView<?> parent, View view, int position, long id) {
            if (updatingSpinner || position < 0 || position >= spinnerKeys.size()) {
              return;
            }
            String chosen = spinnerKeys.get(position);
            selectedFolder = chosen;
            if (onFolderChanged != null) {
              onFolderChanged.accept(chosen);
            }
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  public void setOnQueryChanged(Consumer<String> listener) {
    onQueryChanged = listener;
  }

  public void setOnFolderChanged(Consumer<String> listener) {
    onFolderChanged = listener;
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

  public void bind(
      @NonNull List<Note> allNotes,
      @NonNull List<String> folders,
      @NonNull String query,
      @Nullable String folderFilter,
      @NonNull DateFormat dateFormat) {
    currentQuery = query;
    selectedFolder = folderFilter;
    searchControls.setQuery(query);
    bindFolderSpinner(folders, folderFilter);

    List<Note> matches =
        NoteFilters.filterNotes(allNotes, currentQuery, selectedFolder, null, null, null);
    resultCount.setText(getContext().getString(R.string.search_result_count, matches.size()));
    if (matches.isEmpty()) {
      emptyView.setText(
          allNotes.isEmpty() ? R.string.search_no_notes_yet : R.string.search_no_results);
      emptyView.setVisibility(VISIBLE);
    } else {
      emptyView.setVisibility(GONE);
    }

    int count = matches.size();
    while (resultsContainer.getChildCount() > count) {
      resultsContainer.removeViewAt(resultsContainer.getChildCount() - 1);
    }
    while (resultsContainer.getChildCount() < count) {
      SearchResultView row = new SearchResultView(getContext());
      row.setOnSelect(
          note -> {
            if (onSelect != null) {
              onSelect.accept(note);
            }
          });
      row.setOnDelete(
          note -> {
            if (onDelete != null) {
              onDelete.accept(note);
            }
          });
      row.setOnMoveFolder(
          note -> {
            if (onMoveFolder != null) {
              onMoveFolder.accept(note);
            }
          });
      LayoutParams params =
          new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      params.topMargin = getResources().getDimensionPixelSize(R.dimen.home_note_spacing);
      resultsContainer.addView(row, params);
    }
    for (int i = 0; i < count; i++) {
      Note note = matches.get(i);
      ((SearchResultView) resultsContainer.getChildAt(i))
          .bind(note, dateFormat.format(new Date(note.getTime())));
    }
  }

  private void bindFolderSpinner(List<String> folders, @Nullable String activeFolder) {
    List<String> nextKeys = new ArrayList<>();
    nextKeys.add(null);
    nextKeys.addAll(folders);
    boolean keysChanged = !nextKeys.equals(spinnerKeys);
    int targetIndex = 0;
    if (activeFolder != null) {
      int idx = nextKeys.indexOf(activeFolder);
      if (idx >= 0) {
        targetIndex = idx;
      }
    }
    if (keysChanged) {
      spinnerKeys.clear();
      spinnerKeys.addAll(nextKeys);
      List<String> labels = new ArrayList<>(nextKeys.size());
      labels.add(getContext().getString(R.string.search_folder_all));
      for (int i = 1; i < nextKeys.size(); i++) {
        labels.add(UiLabels.folderLabel(getContext(), nextKeys.get(i)));
      }
      updatingSpinner = true;
      try {
        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderSpinner.setAdapter(adapter);
        folderSpinner.setSelection(targetIndex, false);
      } finally {
        updatingSpinner = false;
      }
    } else if (folderSpinner.getSelectedItemPosition() != targetIndex) {
      updatingSpinner = true;
      try {
        folderSpinner.setSelection(targetIndex, false);
      } finally {
        updatingSpinner = false;
      }
    }
  }
}
