package com.example.cs265final.feature.recording;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.example.cs265final.R;
import com.example.cs265final.ui.UiLabels;
import com.example.cs265final.ui.WaveformView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Live Thai voice recording and transcript editing screen. */
public final class RecordingView extends LinearLayout {
  private final View statusDot;
  private final TextView statusText;
  private final WaveformView waveformView;
  private final EditText transcriptInput;
  private final TextView messageView;
  private final View metaContainer;
  private final EditText titleInput;
  private final LinearLayout foldersContainer;
  private final EditText folderInput;
  private final LinearLayout moodsContainer;
  private final ImageButton micButton;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean updatingFields;
  private boolean showMetaEditor = true;
  private boolean listening;
  private int elapsedSeconds;
  private String selectedMood = UiLabels.MOOD_NORMAL;
  private String selectedFolder = "";
  private List<String> availableFolders = new ArrayList<>();

  private Consumer<String> onTitleChanged;
  private Consumer<String> onTranscriptChanged;
  private Consumer<String> onMoodChanged;
  private Consumer<String> onFolderChanged;
  private Runnable onToggleMic;
  private Runnable onSave;
  private Runnable onCancel;

  private final Runnable timerTick =
      new Runnable() {
        @Override
        public void run() {
          if (listening) {
            elapsedSeconds++;
            updateStatusBanner();
            handler.postDelayed(this, 1_000L);
          }
        }
      };

  public RecordingView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_recording, this, true);
    statusDot = findViewById(R.id.view_recording_status_dot);
    statusText = findViewById(R.id.text_recording_status);
    waveformView = findViewById(R.id.waveform_recording);
    transcriptInput = findViewById(R.id.input_recording_transcript);
    messageView = findViewById(R.id.text_recording_message);
    metaContainer = findViewById(R.id.recording_meta_container);
    titleInput = findViewById(R.id.input_recording_title);
    foldersContainer = findViewById(R.id.recording_folders_container);
    folderInput = findViewById(R.id.input_recording_folder);
    moodsContainer = findViewById(R.id.recording_moods_container);
    micButton = findViewById(R.id.button_recording_mic);

    findViewById(R.id.button_recording_back)
        .setOnClickListener(
            view -> {
              if (onCancel != null) {
                onCancel.run();
              }
            });
    View.OnClickListener toggleMeta =
        view -> {
          showMetaEditor = !showMetaEditor;
          metaContainer.setVisibility(showMetaEditor ? VISIBLE : GONE);
        };
    findViewById(R.id.button_recording_toggle_meta).setOnClickListener(toggleMeta);
    findViewById(R.id.button_recording_bookmark).setOnClickListener(toggleMeta);
    micButton.setOnClickListener(
        view -> {
          if (onToggleMic != null) {
            onToggleMic.run();
          }
        });
    findViewById(R.id.button_recording_save)
        .setOnClickListener(
            view -> {
              if (onSave != null) {
                onSave.run();
              }
            });

    transcriptInput.addTextChangedListener(
        simpleWatcher(
            value -> {
              if (onTranscriptChanged != null) {
                onTranscriptChanged.accept(value);
              }
            }));
    titleInput.addTextChangedListener(
        simpleWatcher(
            value -> {
              if (onTitleChanged != null) {
                onTitleChanged.accept(value);
              }
            }));
    folderInput.addTextChangedListener(
        simpleWatcher(
            value -> {
              selectedFolder = value;
              renderFolderChips();
              if (onFolderChanged != null) {
                onFolderChanged.accept(value);
              }
            }));
  }

  public void setOnTitleChanged(Consumer<String> listener) {
    onTitleChanged = listener;
  }

  public void setOnTranscriptChanged(Consumer<String> listener) {
    onTranscriptChanged = listener;
  }

  public void setOnMoodChanged(Consumer<String> listener) {
    onMoodChanged = listener;
  }

  public void setOnFolderChanged(Consumer<String> listener) {
    onFolderChanged = listener;
  }

  public void setOnToggleMic(Runnable action) {
    onToggleMic = action;
  }

  public void setOnSave(Runnable action) {
    onSave = action;
  }

  public void setOnCancel(Runnable action) {
    onCancel = action;
  }

  public int getElapsedSeconds() {
    return elapsedSeconds;
  }

  public void setElapsedSeconds(int seconds) {
    this.elapsedSeconds = Math.max(0, seconds);
    updateStatusBanner();
  }

  public void bind(
      @NonNull String title,
      @NonNull String transcript,
      boolean isListening,
      @NonNull String mood,
      @NonNull String folder,
      @NonNull List<String> folders,
      @NonNull String message) {
    updatingFields = true;
    try {
      syncEditText(titleInput, title);
      syncEditText(transcriptInput, transcript);
      syncEditText(folderInput, folder);
    } finally {
      updatingFields = false;
    }

    if (this.listening != isListening) {
      this.listening = isListening;
      handler.removeCallbacks(timerTick);
      if (isListening) {
        handler.postDelayed(timerTick, 1_000L);
      }
    }

    this.selectedMood = mood;
    this.selectedFolder = folder;
    this.availableFolders = folders;

    if (message.trim().isEmpty()) {
      messageView.setVisibility(GONE);
    } else {
      messageView.setText(message);
      messageView.setVisibility(VISIBLE);
    }

    waveformView.setActive(isListening);
    micButton.setContentDescription(
        getContext()
            .getString(
                isListening
                    ? R.string.recording_mic_stop_desc
                    : R.string.recording_mic_start_desc));
    micButton.setImageResource(
        isListening
            ? android.R.drawable.ic_media_pause
            : android.R.drawable.ic_btn_speak_now);
    updateStatusBanner();
    renderFolderChips();
    renderMoodChips();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    handler.removeCallbacks(timerTick);
  }

  private void updateStatusBanner() {
    String clock =
        String.format(
            Locale.US,
            "%02d:%02d:%02d",
            elapsedSeconds / 3600,
            (elapsedSeconds % 3600) / 60,
            elapsedSeconds % 60);
    statusText.setText(
        getContext()
            .getString(
                listening
                    ? R.string.recording_status_listening
                    : R.string.recording_status_ready,
                clock));
    statusDot.setBackgroundResource(
        listening ? R.drawable.bg_circle_coral : R.drawable.bg_circle_tonal);
  }

  private void renderFolderChips() {
    foldersContainer.removeAllViews();
    int gap = Math.round(8 * getResources().getDisplayMetrics().density);
    for (int i = 0; i < availableFolders.size(); i++) {
      String option = availableFolders.get(i);
      boolean selected = option.equals(selectedFolder);
      Button chip = new Button(getContext());
      chip.setAllCaps(false);
      chip.setText(UiLabels.folderLabel(getContext(), option));
      chip.setTextSize(12f);
      chip.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.min_touch_target));
      chip.setPadding(gap * 2, 0, gap * 2, 0);
      chip.setBackgroundResource(
          selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
      chip.setTextColor(
          ContextCompat.getColor(
              getContext(), selected ? R.color.primary_coral : R.color.text_secondary));
      chip.setOnClickListener(
          view -> {
            String next = selected ? "" : option;
            selectedFolder = next;
            updatingFields = true;
            try {
              syncEditText(folderInput, next);
            } finally {
              updatingFields = false;
            }
            renderFolderChips();
            if (onFolderChanged != null) {
              onFolderChanged.accept(next);
            }
          });
      LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      if (i > 0) {
        params.setMarginStart(gap);
      }
      foldersContainer.addView(chip, params);
    }
  }

  private void renderMoodChips() {
    moodsContainer.removeAllViews();
    int gap = Math.round(6 * getResources().getDisplayMetrics().density);
    for (int i = 0; i < UiLabels.DEFAULT_MOODS.size(); i++) {
      String option = UiLabels.DEFAULT_MOODS.get(i);
      boolean selected = option.equals(selectedMood);
      Button chip = new Button(getContext());
      chip.setAllCaps(false);
      chip.setGravity(Gravity.CENTER);
      chip.setMaxLines(2);
      chip.setText(UiLabels.moodLabel(getContext(), option));
      chip.setTextSize(12f);
      chip.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.min_touch_target));
      chip.setPadding(gap, gap, gap, gap);
      chip.setBackgroundResource(
          selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
      chip.setTextColor(
          ContextCompat.getColor(
              getContext(), selected ? R.color.primary_coral : R.color.text_secondary));
      chip.setOnClickListener(
          view -> {
            selectedMood = option;
            renderMoodChips();
            if (onMoodChanged != null) {
              onMoodChanged.accept(option);
            }
          });
      LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
      if (i > 0) {
        params.setMarginStart(gap);
      }
      moodsContainer.addView(chip, params);
    }
  }

  private TextWatcher simpleWatcher(Consumer<String> callback) {
    return new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!updatingFields) {
          callback.accept(s.toString());
        }
      }

      @Override
      public void afterTextChanged(Editable s) {}
    };
  }

  private static void syncEditText(EditText field, String value) {
    if (!value.contentEquals(field.getText())) {
      field.setText(value);
      field.setSelection(field.length());
    }
  }
}
