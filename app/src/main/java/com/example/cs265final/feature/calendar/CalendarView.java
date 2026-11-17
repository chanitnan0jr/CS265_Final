package com.example.cs265final.feature.calendar;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.example.cs265final.R;
import com.example.cs265final.data.Note;
import com.example.cs265final.feature.search.SearchResultView;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** Daily journal calendar view displaying a month grid and notes for the selected date. */
public final class CalendarView extends LinearLayout {
  private final TextView monthTitle;
  private final LinearLayout weeksContainer;
  private final TextView selectedDateHeading;
  private final TextView dayCountView;
  private final TextView emptyView;
  private final LinearLayout notesContainer;

  private long monthStartMillis;
  private boolean monthInitialized;
  private String currentDate = "";
  private List<Note> currentNotes = new ArrayList<>();
  private SimpleDateFormat currentDateFormat;

  private Consumer<String> onDateChanged;
  private Consumer<Note> onSelectNote;
  private Consumer<Note> onDeleteNote;
  private Consumer<Note> onMoveNoteFolder;
  private Runnable onNewNote;

  public CalendarView(@NonNull Context context) {
    super(context);
    setOrientation(VERTICAL);
    LayoutInflater.from(context).inflate(R.layout.view_calendar, this, true);
    monthTitle = findViewById(R.id.text_calendar_month);
    weeksContainer = findViewById(R.id.calendar_weeks_container);
    selectedDateHeading = findViewById(R.id.text_calendar_selected_date);
    dayCountView = findViewById(R.id.text_calendar_day_count);
    emptyView = findViewById(R.id.text_calendar_empty);
    notesContainer = findViewById(R.id.calendar_notes_container);

    findViewById(R.id.button_calendar_today)
        .setOnClickListener(
            view -> {
              if (currentDateFormat != null && onDateChanged != null) {
                String today = currentDateFormat.format(new Date());
                monthStartMillis = computeMonthStart(currentDateFormat.getCalendar(), new Date());
                onDateChanged.accept(today);
              }
            });
    findViewById(R.id.button_calendar_prev_month).setOnClickListener(view -> shiftMonth(-1));
    findViewById(R.id.button_calendar_next_month).setOnClickListener(view -> shiftMonth(1));
    findViewById(R.id.button_calendar_new_note)
        .setOnClickListener(
            view -> {
              if (onNewNote != null) {
                onNewNote.run();
              }
            });
  }

  public void setOnDateChanged(Consumer<String> listener) {
    onDateChanged = listener;
  }

  public void setOnSelectNote(Consumer<Note> listener) {
    onSelectNote = listener;
  }

  public void setOnDeleteNote(Consumer<Note> listener) {
    onDeleteNote = listener;
  }

  public void setOnMoveNoteFolder(Consumer<Note> listener) {
    onMoveNoteFolder = listener;
  }

  public void setOnNewNote(Runnable action) {
    onNewNote = action;
  }

  public void bind(
      @NonNull List<Note> notes,
      @NonNull SimpleDateFormat dateFormat,
      @NonNull String selectedDate) {
    this.currentNotes = notes;
    this.currentDateFormat = dateFormat;
    boolean dateChanged = !selectedDate.equals(this.currentDate);
    this.currentDate = selectedDate;

    Date parsed = parseDate(dateFormat, selectedDate);
    if (!monthInitialized || dateChanged) {
      monthStartMillis =
          computeMonthStart(dateFormat.getCalendar(), parsed != null ? parsed : new Date());
      monthInitialized = true;
    }
    render();
  }

  private void shiftMonth(int delta) {
    if (currentDateFormat == null) {
      return;
    }
    Calendar cal = (Calendar) currentDateFormat.getCalendar().clone();
    cal.setTimeInMillis(monthStartMillis);
    cal.add(Calendar.MONTH, delta);
    monthStartMillis = cal.getTimeInMillis();
    render();
  }

  private void render() {
    if (currentDateFormat == null) {
      return;
    }
    SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    monthFormat.setCalendar((Calendar) currentDateFormat.getCalendar().clone());
    monthTitle.setText(monthFormat.format(new Date(monthStartMillis)));

    Set<String> noteDates = new HashSet<>();
    List<Note> dayNotes = new ArrayList<>();
    for (Note note : currentNotes) {
      String formatted = currentDateFormat.format(new Date(note.getTime()));
      noteDates.add(formatted);
      if (currentDate.equals(formatted)) {
        dayNotes.add(note);
      }
    }

    renderCalendarGrid(noteDates);
    selectedDateHeading.setText(
        getContext().getString(R.string.calendar_notes_on_date, currentDate));
    dayCountView.setText(getContext().getString(R.string.folder_item_count, dayNotes.size()));
    emptyView.setVisibility(dayNotes.isEmpty() ? VISIBLE : GONE);
    renderNotesList(dayNotes, currentDateFormat);
  }

  private void renderCalendarGrid(Set<String> noteDates) {
    weeksContainer.removeAllViews();
    Calendar cal = (Calendar) currentDateFormat.getCalendar().clone();
    cal.setTimeInMillis(monthStartMillis);
    int leading = Math.max(0, Math.min(6, cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));
    int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    float density = getResources().getDisplayMetrics().density;
    int cellHeight = Math.round(44 * density);
    int circleSize = Math.round(30 * density);
    int dotSize = Math.round(4 * density);

    int totalCells = leading + daysInMonth;
    int rowCount = (totalCells + 6) / 7;
    for (int week = 0; week < rowCount; week++) {
      LinearLayout row = new LinearLayout(getContext());
      row.setOrientation(HORIZONTAL);
      for (int col = 0; col < 7; col++) {
        int index = week * 7 + col;
        int day = index - leading + 1;
        if (day < 1 || day > daysInMonth) {
          View spacer = new View(getContext());
          row.addView(spacer, new LayoutParams(0, cellHeight, 1f));
          continue;
        }
        Calendar dayCal = (Calendar) currentDateFormat.getCalendar().clone();
        dayCal.setTimeInMillis(monthStartMillis);
        dayCal.set(Calendar.DAY_OF_MONTH, day);
        String dayString = currentDateFormat.format(dayCal.getTime());
        boolean isSelected = dayString.equals(currentDate);
        boolean hasNotes = noteDates.contains(dayString);

        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setOnClickListener(
            view -> {
              if (onDateChanged != null) {
                onDateChanged.accept(dayString);
              }
            });

        TextView dayLabel = new TextView(getContext());
        dayLabel.setGravity(Gravity.CENTER);
        dayLabel.setText(String.valueOf(day));
        dayLabel.setTextSize(14f);
        if (isSelected) {
          dayLabel.setBackgroundResource(R.drawable.bg_circle_coral);
          dayLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.on_primary_coral));
        } else {
          dayLabel.setBackgroundColor(Color.TRANSPARENT);
          dayLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        }
        cell.addView(dayLabel, new LayoutParams(circleSize, circleSize));

        View dot = new View(getContext());
        if (hasNotes) {
          dot.setBackgroundResource(R.drawable.bg_circle_coral);
        } else {
          dot.setBackgroundColor(Color.TRANSPARENT);
        }
        LayoutParams dotParams = new LayoutParams(dotSize, dotSize);
        dotParams.topMargin = Math.round(2 * density);
        cell.addView(dot, dotParams);

        row.addView(cell, new LayoutParams(0, cellHeight, 1f));
      }
      weeksContainer.addView(
          row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }
  }

  private void renderNotesList(List<Note> dayNotes, DateFormat dateFormat) {
    int count = dayNotes.size();
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
      Note note = dayNotes.get(i);
      ((SearchResultView) notesContainer.getChildAt(i))
          .bind(note, dateFormat.format(new Date(note.getTime())));
    }
  }

  private static long computeMonthStart(Calendar template, Date source) {
    Calendar cal = (Calendar) template.clone();
    cal.setTime(source);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTimeInMillis();
  }

  private static Date parseDate(SimpleDateFormat format, String date) {
    try {
      return format.parse(date);
    } catch (ParseException e) {
      return null;
    }
  }
}
