package com.example.cs265final.feature.notes;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.cs265final.R;
import com.example.cs265final.data.AudioSource;
import com.example.cs265final.data.FolderStorage;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.NoteFilters;
import com.example.cs265final.feature.search.SearchResultView;
import com.example.cs265final.ui.UiLabels;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Note library screen with source filter, folder management, and filtered notes list. */
public final class LibraryView extends LinearLayout {
  private final Button chipAll;
  private final Button chipRecorded;
  private final Button chipUploaded;
  private final Button showAllFoldersButton;
  private final LinearLayout foldersContainer;
  private final TextView notesHeading;
  private final TextView emptyView;
  private final LinearLayout notesContainer;

  @Nullable private AudioSource activeSource;
  @Nullable private String selectedFolder;
  private List<Note> lastNotes = new ArrayList<>();
  private List<String> lastFolders = new ArrayList<>();
  @Nullable private DateFormat lastDateFormat;

  private Runnable onSearchClick;
  private Runnable onCreateFolderClick;
  private Consumer<String> onRenameFolderClick;
  private Consumer<String> onDeleteFolderClick;
  private Consumer<Note> onSelectNote;
  private Consumer<Note> onDeleteNote;
  private Consumer<Note> onMoveNoteFolder;

  public LibraryView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_library, this, true);
    chipAll = findViewById(R.id.chip_source_all);
    chipRecorded = findViewById(R.id.chip_source_recorded);
    chipUploaded = findViewById(R.id.chip_source_uploaded);
    showAllFoldersButton = findViewById(R.id.button_library_show_all);
    foldersContainer = findViewById(R.id.library_folders_container);
    notesHeading = findViewById(R.id.text_library_notes_heading);
    emptyView = findViewById(R.id.text_library_empty);
    notesContainer = findViewById(R.id.library_notes_container);

    findViewById(R.id.button_library_search)
        .setOnClickListener(
            view -> {
              if (onSearchClick != null) {
                onSearchClick.run();
              }
            });
    findViewById(R.id.button_library_new_folder)
        .setOnClickListener(
            view -> {
              if (onCreateFolderClick != null) {
                onCreateFolderClick.run();
              }
            });
    showAllFoldersButton.setOnClickListener(
        view -> {
          selectedFolder = null;
          refresh();
        });

    chipAll.setOnClickListener(
        view -> {
          activeSource = null;
          selectedFolder = null;
          refresh();
        });
    chipRecorded.setOnClickListener(
        view -> {
          activeSource = AudioSource.RECORDED;
          refresh();
        });
    chipUploaded.setOnClickListener(
        view -> {
          activeSource = AudioSource.UPLOADED;
          refresh();
        });
  }

  public void setOnSearchClick(Runnable action) {
    onSearchClick = action;
  }

  public void setOnCreateFolderClick(Runnable action) {
    onCreateFolderClick = action;
  }

  public void setOnRenameFolderClick(Consumer<String> action) {
    onRenameFolderClick = action;
  }

  public void setOnDeleteFolderClick(Consumer<String> action) {
    onDeleteFolderClick = action;
  }

  public void setOnSelectNote(Consumer<Note> action) {
    onSelectNote = action;
  }

  public void setOnDeleteNote(Consumer<Note> action) {
    onDeleteNote = action;
  }

  public void setOnMoveNoteFolder(Consumer<Note> action) {
    onMoveNoteFolder = action;
  }

  public void bind(
      @NonNull List<Note> notes,
      @NonNull List<String> folders,
      @NonNull DateFormat dateFormat) {
    this.lastNotes = notes;
    this.lastFolders = folders;
    this.lastDateFormat = dateFormat;
    if (selectedFolder != null && !folders.contains(selectedFolder)) {
      selectedFolder = null;
    }
    refresh();
  }

  private void refresh() {
    if (lastDateFormat == null) {
      return;
    }
    updateSourceChips();
    showAllFoldersButton.setVisibility(selectedFolder != null ? VISIBLE : GONE);
    renderFolders();
    renderNotes();
  }

  private void updateSourceChips() {
    styleChip(chipAll, activeSource == null);
    styleChip(chipRecorded, activeSource == AudioSource.RECORDED);
    styleChip(chipUploaded, activeSource == AudioSource.UPLOADED);
  }

  private void styleChip(Button chip, boolean selected) {
    chip.setBackgroundResource(
        selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
    chip.setTextColor(
        ContextCompat.getColor(
            getContext(), selected ? R.color.primary_coral : R.color.text_secondary));
  }

  private void renderFolders() {
    foldersContainer.removeAllViews();
    Map<String, Integer> counts = new HashMap<>();
    for (Note note : lastNotes) {
      String folder = note.getFolder();
      Integer current = counts.get(folder);
      counts.put(folder, current == null ? 1 : current + 1);
    }

    LayoutInflater inflater = LayoutInflater.from(getContext());
    int spacing = Math.round(10 * getResources().getDisplayMetrics().density);
    for (int i = 0; i < lastFolders.size(); i += 2) {
      LinearLayout row = new LinearLayout(getContext());
      row.setOrientation(HORIZONTAL);
      LayoutParams rowParams =
          new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      if (i > 0) {
        rowParams.topMargin = spacing;
      }

      row.addView(createFolderCard(inflater, lastFolders.get(i), counts), cardLayoutParams(0));
      if (i + 1 < lastFolders.size()) {
        row.addView(
            createFolderCard(inflater, lastFolders.get(i + 1), counts), cardLayoutParams(spacing));
      } else {
        // Space reports 0 under AT_MOST, whereas a plain View expands to fill the parent height.
        Space spacer = new Space(getContext());
        LayoutParams spacerParams = new LayoutParams(0, 0, 1f);
        spacerParams.setMarginStart(spacing);
        row.addView(spacer, spacerParams);
      }
      foldersContainer.addView(row, rowParams);
    }
  }

  private LayoutParams cardLayoutParams(int startMargin) {
    LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    params.setMarginStart(startMargin);
    return params;
  }

  private View createFolderCard(
      LayoutInflater inflater, String folderName, Map<String, Integer> counts) {
    View card = inflater.inflate(R.layout.item_folder_card, foldersContainer, false);
    View root = card.findViewById(R.id.folder_card_root);
    TextView nameView = card.findViewById(R.id.text_folder_name);
    TextView countView = card.findViewById(R.id.text_folder_count);
    View optionsBtn = card.findViewById(R.id.button_folder_options);

    boolean isSelected = folderName.equals(selectedFolder);
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(18 * getResources().getDisplayMetrics().density);
    bg.setColor(ContextCompat.getColor(getContext(), UiLabels.folderTintRes(folderName)));
    int strokeWidth = Math.round((isSelected ? 2f : 1f) * getResources().getDisplayMetrics().density);
    int strokeColor =
        ContextCompat.getColor(
            getContext(), isSelected ? R.color.primary_coral : R.color.border_soft);
    bg.setStroke(strokeWidth, strokeColor);
    root.setBackground(bg);

    String displayName = UiLabels.folderLabel(getContext(), folderName);
    nameView.setText(displayName);
    int itemCount = counts.containsKey(folderName) ? counts.get(folderName) : 0;
    countView.setText(getContext().getString(R.string.folder_item_count, itemCount));
    optionsBtn.setContentDescription(
        getContext().getString(R.string.folder_options_desc, displayName));

    root.setOnClickListener(
        view -> {
          selectedFolder = isSelected ? null : folderName;
          refresh();
        });
    optionsBtn.setOnClickListener(
        view -> {
          PopupMenu menu = new PopupMenu(getContext(), optionsBtn);
          menu.getMenu()
              .add(R.string.folder_rename_action)
              .setOnMenuItemClickListener(
                  item -> {
                    if (onRenameFolderClick != null) {
                      onRenameFolderClick.accept(folderName);
                    }
                    return true;
                  });
          boolean canDelete =
              !FolderStorage.DEFAULT_GENERAL_FOLDER.equals(folderName) || lastFolders.size() > 1;
          if (canDelete) {
            menu.getMenu()
                .add(R.string.folder_delete_action)
                .setOnMenuItemClickListener(
                    item -> {
                      if (onDeleteFolderClick != null) {
                        onDeleteFolderClick.accept(folderName);
                      }
                      return true;
                    });
          }
          menu.show();
        });
    return card;
  }

  private void renderNotes() {
    if (selectedFolder != null) {
      notesHeading.setText(
          getContext()
              .getString(
                  R.string.library_in_folder,
                  UiLabels.folderLabel(getContext(), selectedFolder)));
    } else {
      notesHeading.setText(R.string.library_recent_notes);
    }

    List<Note> filtered =
        NoteFilters.filterNotes(lastNotes, "", selectedFolder, null, null, activeSource);
    emptyView.setVisibility(filtered.isEmpty() ? VISIBLE : GONE);
    int count = filtered.size();
    while (notesContainer.getChildCount() > count) {
      notesContainer.removeViewAt(notesContainer.getChildCount() - 1);
    }
    while (notesContainer.getChildCount() < count) {
      SearchResultView row = new SearchResultView(getContext());
      row.setOnSelect(
          note -> {
            if (onSelectNote != null) {
              onSelectNote.accept(note);
            }
          });
      row.setOnDelete(
          note -> {
            if (onDeleteNote != null) {
              onDeleteNote.accept(note);
            }
          });
      row.setOnMoveFolder(
          note -> {
            if (onMoveNoteFolder != null) {
              onMoveNoteFolder.accept(note);
            }
          });
      LayoutParams params =
          new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      params.topMargin = getResources().getDimensionPixelSize(R.dimen.home_note_spacing);
      notesContainer.addView(row, params);
    }
    for (int i = 0; i < count; i++) {
      Note note = filtered.get(i);
      ((SearchResultView) notesContainer.getChildAt(i))
          .bind(note, lastDateFormat.format(new Date(note.getTime())));
    }
  }
}
