package com.example.cs265final.feature.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.example.cs265final.R;

/** Displays the confirmation for a note deletion requested by a screen. */
public final class DeleteNoteConfirmation {
  private DeleteNoteConfirmation() {}

  public static AlertDialog show(
      Activity activity, String title, Runnable onConfirm, Runnable onCancel) {
    View content = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_note, null);
    TextView message = content.findViewById(R.id.text_delete_note_message);
    message.setText(activity.getString(R.string.delete_note_message, title));

    AlertDialog dialog = new AlertDialog.Builder(activity)
        .setTitle(R.string.delete_note_title)
        .setView(content)
        .setPositiveButton(R.string.delete_note_confirm, (ignored, which) -> onConfirm.run())
        .setNegativeButton(R.string.delete_note_cancel, (ignored, which) -> onCancel.run())
        .setOnCancelListener(ignored -> onCancel.run())
        .create();
    dialog.show();
    return dialog;
  }
}
