package com.example.cs265final.feature.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.cs265final.R;
import com.example.cs265final.data.FolderStorage;
import com.example.cs265final.ui.UiLabels;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Dialogs for creating, renaming, deleting, and assigning note folders. */
public final class FolderDialogs {
  private FolderDialogs() {}

  public static AlertDialog showCreateDialog(
      @NonNull Activity activity,
      @NonNull List<String> existingFolders,
      @NonNull Consumer<String> onCreate) {
    return showNameDialog(
        activity,
        R.string.folder_create_title,
        "",
        null,
        existingFolders,
        onCreate);
  }

  public static AlertDialog showRenameDialog(
      @NonNull Activity activity,
      @NonNull String currentName,
      @NonNull List<String> existingFolders,
      @NonNull Consumer<String> onRename) {
    return showNameDialog(
        activity,
        R.string.folder_rename_title,
        currentName,
        currentName,
        existingFolders,
        onRename);
  }

  public static AlertDialog showDeleteConfirmation(
      @NonNull Activity activity,
      @NonNull String folderName,
      @NonNull String fallbackFolder,
      @NonNull Runnable onConfirm,
      @NonNull Runnable onCancel) {
    String displayTarget = UiLabels.folderLabel(activity, folderName);
    String displayFallback = UiLabels.folderLabel(activity, fallbackFolder);
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle(R.string.folder_delete_title)
            .setMessage(
                activity.getString(R.string.folder_delete_message, displayTarget, displayFallback))
            .setPositiveButton(R.string.folder_delete_confirm, (ignored, which) -> onConfirm.run())
            .setNegativeButton(R.string.folder_cancel_action, (ignored, which) -> onCancel.run())
            .setOnCancelListener(ignored -> onCancel.run())
            .create();
    dialog.show();
    return dialog;
  }

  public static AlertDialog showMoveNoteDialog(
      @NonNull Activity activity,
      @NonNull List<String> folders,
      @NonNull String currentFolder,
      @NonNull Consumer<String> onSelect) {
    List<String> keys = new ArrayList<>(folders);
    if (!keys.contains(FolderStorage.DEFAULT_GENERAL_FOLDER)) {
      keys.add(FolderStorage.DEFAULT_GENERAL_FOLDER);
    }
    String[] labels = new String[keys.size()];
    int checked = -1;
    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      labels[i] = UiLabels.folderLabel(activity, key);
      if (key.equals(currentFolder)) {
        checked = i;
      }
    }
    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle(R.string.folder_move_title)
            .setSingleChoiceItems(
                labels,
                checked,
                (dlg, which) -> {
                  dlg.dismiss();
                  onSelect.accept(keys.get(which));
                })
            .setNegativeButton(R.string.folder_cancel_action, null)
            .create();
    dialog.show();
    return dialog;
  }

  private static AlertDialog showNameDialog(
      Activity activity,
      int titleRes,
      String initialValue,
      @Nullable String currentName,
      List<String> existingFolders,
      Consumer<String> onValidName) {
    View content = LayoutInflater.from(activity).inflate(R.layout.dialog_folder_name, null);
    EditText input = content.findViewById(R.id.input_folder_name);
    TextView errorView = content.findViewById(R.id.text_folder_name_error);
    input.setText(initialValue);
    input.setSelection(input.length());

    AlertDialog dialog =
        new AlertDialog.Builder(activity)
            .setTitle(titleRes)
            .setView(content)
            .setPositiveButton(R.string.folder_save_action, null)
            .setNegativeButton(R.string.folder_cancel_action, null)
            .create();
    dialog.show();

    Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    positive.setOnClickListener(
        view -> {
          String candidate = input.getText().toString().trim();
          if (candidate.isEmpty()) {
            errorView.setText(R.string.folder_name_empty_error);
            errorView.setVisibility(View.VISIBLE);
            return;
          }
          if (currentName != null && candidate.equals(currentName)) {
            dialog.dismiss();
            return;
          }
          for (String existing : existingFolders) {
            if (existing.equalsIgnoreCase(candidate)
                && (currentName == null || !existing.equals(currentName))) {
              errorView.setText(R.string.folder_name_duplicate_error);
              errorView.setVisibility(View.VISIBLE);
              return;
            }
          }
          dialog.dismiss();
          onValidName.accept(candidate);
        });
    return dialog;
  }
}
