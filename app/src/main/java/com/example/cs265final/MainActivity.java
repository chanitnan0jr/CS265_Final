package com.example.cs265final;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.cs265final.data.AudioFiles;
import com.example.cs265final.data.AudioSource;
import com.example.cs265final.data.AudioUtils;
import com.example.cs265final.data.FolderStorage;
import com.example.cs265final.data.Note;
import com.example.cs265final.data.NoteRepository;
import com.example.cs265final.data.TranscriptionStatus;
import com.example.cs265final.feature.calendar.CalendarView;
import com.example.cs265final.feature.home.HomeView;
import com.example.cs265final.feature.notes.DeleteNoteConfirmation;
import com.example.cs265final.feature.notes.FolderDialogs;
import com.example.cs265final.feature.notes.LibraryView;
import com.example.cs265final.feature.notes.NoteDetailView;
import com.example.cs265final.feature.playback.PlaybackView;
import com.example.cs265final.feature.recording.RecordingView;
import com.example.cs265final.feature.search.SearchView;
import com.example.cs265final.speech.RecordingDraftViewModel;
import com.example.cs265final.speech.SpeechCaptureController;
import com.example.cs265final.transcription.TranscriptionScheduler;
import com.example.cs265final.ui.UiLabels;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONException;

/**
 * Entry point and coordinator for navigation, lifecycle restoration, permissions, and repository
 * actions across Voice2Note screens.
 */
public final class MainActivity extends ComponentActivity {
  private static final String TAB_HOME = "home";
  private static final String TAB_LIBRARY = "library";
  private static final String TAB_CALENDAR = "calendar";
  private static final String TAB_SEARCH = "search";

  private static final String STATE_TAB = "active_tab";
  private static final String STATE_EDITING = "editing";
  private static final String STATE_SELECTED_NOTE_ID = "selected_note_id";
  private static final String STATE_PLAYBACK_URI = "playback_uri";
  private static final String STATE_DRAFT_TITLE = "draft_title";
  private static final String STATE_DRAFT_TRANSCRIPT = "draft_transcript";
  private static final String STATE_DRAFT_MOOD = "draft_mood";
  private static final String STATE_DRAFT_FOLDER = "draft_folder";
  private static final String STATE_RECORDING_ELAPSED = "recording_elapsed";
  private static final String STATE_SEARCH_QUERY = "search_query";
  private static final String STATE_SEARCH_FOLDER = "search_folder";
  private static final String STATE_SELECTED_DATE = "selected_date";

  private static final String[] UPLOAD_MIME_TYPES =
      new String[] {"audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/wav", "audio/x-wav"};
  private static final long WORKER_REFRESH_INTERVAL_MS = 2_000L;

  private NoteRepository repository;
  private RecordingDraftViewModel draftViewModel;
  private SpeechCaptureController speechController;
  private SimpleDateFormat dateFormat;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean storageError;
  private List<Note> notes = new ArrayList<>();
  private List<String> folders = new ArrayList<>();

  private String activeTab = TAB_HOME;
  private boolean editing;
  @Nullable private String selectedNoteId;
  @Nullable private Uri playbackUri;

  private String draftTitle = "";
  private String draftTranscript = "";
  private String draftMood = UiLabels.MOOD_NORMAL;
  private String draftFolder = "";
  private String recordingMessage = "";
  private int recordingElapsedSeconds;

  private String searchQuery = "";
  @Nullable private String searchFolderFilter;
  private String selectedDate = "";

  private TextView storageErrorBanner;
  private FrameLayout contentContainer;
  private View navBar;
  private Button navHome;
  private Button navLibrary;
  private Button navCalendar;
  private Button navSearch;

  private HomeView homeView;
  private LibraryView libraryView;
  private CalendarView calendarView;
  private SearchView searchView;
  private RecordingView recordingView;
  private NoteDetailView noteDetailView;
  private PlaybackView playbackView;
  @Nullable private View currentContentView;
  @Nullable private AlertDialog activeDialog;

  private final Runnable workerPollTask =
      new Runnable() {
        @Override
        public void run() {
          if (!hasPendingTranscriptions()) {
            return;
          }
          refreshFromStorage();
          if (hasPendingTranscriptions()) {
            handler.postDelayed(this, WORKER_REFRESH_INTERVAL_MS);
          }
        }
      };

  private final ActivityResultLauncher<Intent> legacySpeechLauncher =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(),
          result -> {
            speechController.handleLegacyResult(
                result.getResultCode(), result.getData(), draftTranscript);
            renderCurrentScreen();
          });

  private final ActivityResultLauncher<String> micPermissionLauncher =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            if (granted) {
              speechController.startSpeech(draftTranscript, legacySpeechLauncher::launch);
            } else {
              recordingMessage =
                  getString(
                      shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                          ? R.string.permission_mic_rationale
                          : R.string.permission_mic_settings);
            }
            renderCurrentScreen();
          });

  private final ActivityResultLauncher<String[]> audioPickerLauncher =
      registerForActivityResult(
          new ActivityResultContracts.OpenDocument(), this::handlePickedAudioUri);

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    repository = new NoteRepository(this);
    draftViewModel = new ViewModelProvider(this).get(RecordingDraftViewModel.class);
    dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    selectedDate = dateFormat.format(new Date());

    speechController =
        new SpeechCaptureController(
            this,
            draftViewModel,
            new SpeechCaptureController.Listener() {
              @Override
              public void onTranscriptChanged(@NonNull String transcript) {
                draftTranscript = transcript;
                if (editing && currentContentView == recordingView) {
                  bindRecordingView();
                }
              }

              @Override
              public void onListeningChanged(boolean listening) {
                if (editing && currentContentView == recordingView) {
                  bindRecordingView();
                }
              }

              @Override
              public void onMessage(@NonNull String message) {
                recordingMessage = message;
                if (editing && currentContentView == recordingView) {
                  bindRecordingView();
                }
              }
            });

    restoreState(savedInstanceState);
    loadLibraryInitial();
    bindChromeViews();
    initFeatureViews();
    registerBackHandler();
    renderCurrentScreen();
  }

  @Override
  protected void onStart() {
    super.onStart();
    refreshFromStorage();
    scheduleWorkerPollingIfNeeded();
  }

  @Override
  protected void onStop() {
    handler.removeCallbacks(workerPollTask);
    if (speechController != null) {
      speechController.cancelSpeech();
    }
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    handler.removeCallbacks(workerPollTask);
    dismissActiveDialog();
    if (speechController != null) {
      speechController.release();
    }
    if (noteDetailView != null) {
      noteDetailView.releaseMedia();
    }
    if (playbackView != null) {
      playbackView.releasePlayer();
    }
    super.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString(STATE_TAB, activeTab);
    outState.putBoolean(STATE_EDITING, editing);
    outState.putString(STATE_SELECTED_NOTE_ID, selectedNoteId);
    outState.putString(STATE_PLAYBACK_URI, playbackUri != null ? playbackUri.toString() : null);
    outState.putString(STATE_DRAFT_TITLE, draftTitle);
    outState.putString(STATE_DRAFT_TRANSCRIPT, draftTranscript);
    outState.putString(STATE_DRAFT_MOOD, draftMood);
    outState.putString(STATE_DRAFT_FOLDER, draftFolder);
    outState.putInt(
        STATE_RECORDING_ELAPSED,
        recordingView != null ? recordingView.getElapsedSeconds() : recordingElapsedSeconds);
    outState.putString(STATE_SEARCH_QUERY, searchQuery);
    outState.putString(STATE_SEARCH_FOLDER, searchFolderFilter);
    outState.putString(STATE_SELECTED_DATE, selectedDate);
    super.onSaveInstanceState(outState);
  }

  private void restoreState(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      return;
    }
    activeTab = savedInstanceState.getString(STATE_TAB, TAB_HOME);
    editing = savedInstanceState.getBoolean(STATE_EDITING, false);
    selectedNoteId = savedInstanceState.getString(STATE_SELECTED_NOTE_ID);
    String uriRaw = savedInstanceState.getString(STATE_PLAYBACK_URI);
    playbackUri = uriRaw != null ? Uri.parse(uriRaw) : null;
    draftTitle = savedInstanceState.getString(STATE_DRAFT_TITLE, "");
    draftTranscript = savedInstanceState.getString(STATE_DRAFT_TRANSCRIPT, "");
    draftMood = savedInstanceState.getString(STATE_DRAFT_MOOD, UiLabels.MOOD_NORMAL);
    draftFolder = savedInstanceState.getString(STATE_DRAFT_FOLDER, "");
    recordingElapsedSeconds = savedInstanceState.getInt(STATE_RECORDING_ELAPSED, 0);
    searchQuery = savedInstanceState.getString(STATE_SEARCH_QUERY, "");
    searchFolderFilter = savedInstanceState.getString(STATE_SEARCH_FOLDER);
    selectedDate = savedInstanceState.getString(STATE_SELECTED_DATE, selectedDate);
  }

  private void loadLibraryInitial() {
    try {
      NoteRepository.LibrarySnapshot snapshot = repository.loadLibrary();
      notes = snapshot.notes;
      folders = snapshot.folders;
      storageError = false;
    } catch (JSONException e) {
      storageError = true;
      notes = Collections.emptyList();
      folders = new ArrayList<>(FolderStorage.DEFAULT_FOLDERS);
    }
  }

  private void bindChromeViews() {
    storageErrorBanner = findViewById(R.id.text_storage_error);
    contentContainer = findViewById(R.id.main_content_container);
    navBar = findViewById(R.id.bottom_nav_bar);
    navHome = findViewById(R.id.nav_home);
    navLibrary = findViewById(R.id.nav_library);
    navCalendar = findViewById(R.id.nav_calendar);
    navSearch = findViewById(R.id.nav_search);

    navHome.setOnClickListener(view -> selectTab(TAB_HOME));
    navLibrary.setOnClickListener(view -> selectTab(TAB_LIBRARY));
    navCalendar.setOnClickListener(view -> selectTab(TAB_CALENDAR));
    navSearch.setOnClickListener(view -> selectTab(TAB_SEARCH));
  }

  private void initFeatureViews() {
    homeView = new HomeView(this);
    homeView.setOnNew(this::startComposer);
    homeView.setOnUpload(() -> audioPickerLauncher.launch(UPLOAD_MIME_TYPES));
    homeView.setOnViewAll(() -> selectTab(TAB_LIBRARY));
    homeView.setOnSelect(this::openNoteDetail);
    homeView.setOnDelete(this::confirmDeleteNote);
    homeView.setOnMoveFolder(this::promptMoveNoteFolder);

    libraryView = new LibraryView(this);
    libraryView.setOnSearchClick(() -> selectTab(TAB_SEARCH));
    libraryView.setOnCreateFolderClick(this::promptCreateFolder);
    libraryView.setOnRenameFolderClick(this::promptRenameFolder);
    libraryView.setOnDeleteFolderClick(this::promptDeleteFolder);
    libraryView.setOnSelectNote(this::openNoteDetail);
    libraryView.setOnDeleteNote(this::confirmDeleteNote);
    libraryView.setOnMoveNoteFolder(this::promptMoveNoteFolder);

    calendarView = new CalendarView(this);
    calendarView.setOnDateChanged(
        newDate -> {
          selectedDate = newDate;
          renderCurrentScreen();
        });
    calendarView.setOnSelectNote(this::openNoteDetail);
    calendarView.setOnDeleteNote(this::confirmDeleteNote);
    calendarView.setOnMoveNoteFolder(this::promptMoveNoteFolder);
    calendarView.setOnNewNote(this::startComposer);

    searchView = new SearchView(this);
    searchView.setOnQueryChanged(
        value -> {
          searchQuery = value;
          if (currentContentView == searchView) {
            searchView.bind(notes, folders, searchQuery, searchFolderFilter, dateFormat);
          }
        });
    searchView.setOnFolderChanged(
        folder -> {
          searchFolderFilter = folder;
          if (currentContentView == searchView) {
            searchView.bind(notes, folders, searchQuery, searchFolderFilter, dateFormat);
          }
        });
    searchView.setOnSelect(this::openNoteDetail);
    searchView.setOnDelete(this::confirmDeleteNote);
    searchView.setOnMoveFolder(this::promptMoveNoteFolder);

    recordingView = new RecordingView(this);
    recordingView.setElapsedSeconds(recordingElapsedSeconds);
    recordingView.setOnTitleChanged(value -> draftTitle = value);
    recordingView.setOnTranscriptChanged(
        value -> {
          if (speechController.isListening()) {
            speechController.cancelSpeech();
          }
          draftTranscript = value;
        });
    recordingView.setOnMoodChanged(value -> draftMood = value);
    recordingView.setOnFolderChanged(value -> draftFolder = value);
    recordingView.setOnToggleMic(this::toggleRecordingMic);
    recordingView.setOnSave(this::saveDraftNote);
    recordingView.setOnCancel(this::handleCancelRecording);

    noteDetailView = new NoteDetailView(this);
    noteDetailView.bindLifecycle(getLifecycle());
    noteDetailView.setOnBack(
        () -> {
          if (noteDetailView.isRenaming()) {
            noteDetailView.dismissRename();
          } else {
            selectedNoteId = null;
            noteDetailView.releaseMedia();
            renderCurrentScreen();
          }
        });
    noteDetailView.setOnRename(
        newTitle -> {
          Note current = findSelectedNote();
          return current != null && updateNote(current, current.withTitle(newTitle));
        });
    noteDetailView.setOnAttachAudio(() -> audioPickerLauncher.launch(UPLOAD_MIME_TYPES));
    noteDetailView.setOnMoveFolder(
        () -> {
          Note current = findSelectedNote();
          if (current != null) {
            promptMoveNoteFolder(current);
          }
        });
    noteDetailView.setOnDelete(
        () -> {
          Note current = findSelectedNote();
          if (current != null) {
            confirmDeleteNote(current);
          }
        });
    noteDetailView.setOnRetryTranscription(
        () -> {
          Note current = findSelectedNote();
          if (current != null) {
            retryTranscription(current);
          }
        });

    playbackView = new PlaybackView(this);
    playbackView.bindLifecycle(getLifecycle());
    playbackView.setOnBack(
        () -> {
          playbackUri = null;
          playbackView.releasePlayer();
          renderCurrentScreen();
        });
  }

  private void registerBackHandler() {
    getOnBackPressedDispatcher()
        .addCallback(
            this,
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                if (playbackUri != null) {
                  playbackUri = null;
                  playbackView.releasePlayer();
                  renderCurrentScreen();
                  return;
                }
                if (selectedNoteId != null) {
                  if (noteDetailView.isRenaming()) {
                    noteDetailView.dismissRename();
                  } else {
                    selectedNoteId = null;
                    noteDetailView.releaseMedia();
                    renderCurrentScreen();
                  }
                  return;
                }
                if (editing) {
                  handleCancelRecording();
                  return;
                }
                if (!TAB_HOME.equals(activeTab)) {
                  selectTab(TAB_HOME);
                  return;
                }
                showExitConfirmation();
              }
            });
  }

  private void selectTab(String tab) {
    activeTab = tab;
    renderCurrentScreen();
  }

  private void startComposer() {
    draftTranscript = "";
    draftTitle = "";
    draftFolder = "";
    draftMood = UiLabels.MOOD_NORMAL;
    recordingMessage = "";
    recordingElapsedSeconds = 0;
    recordingView.setElapsedSeconds(0);
    draftViewModel.setAudioBuffer(null);
    editing = true;
    renderCurrentScreen();
  }

  private void openNoteDetail(@NonNull Note note) {
    selectedNoteId = note.getId();
    renderCurrentScreen();
  }

  private void renderCurrentScreen() {
    storageErrorBanner.setVisibility(storageError ? View.VISIBLE : View.GONE);

    Note selectedNote = findSelectedNote();
    if (selectedNoteId != null && selectedNote == null) {
      selectedNoteId = null;
    }

    boolean showNav = !editing && selectedNote == null && playbackUri == null;
    navBar.setVisibility(showNav ? View.VISIBLE : View.GONE);
    updateNavHighlight();

    View target;
    if (playbackUri != null) {
      target = playbackView;
      playbackView.bind(playbackUri);
    } else if (selectedNote != null) {
      target = noteDetailView;
      noteDetailView.bind(selectedNote, dateFormat);
    } else if (editing) {
      target = recordingView;
      bindRecordingView();
    } else if (TAB_LIBRARY.equals(activeTab)) {
      target = libraryView;
      libraryView.bind(notes, folders, dateFormat);
    } else if (TAB_CALENDAR.equals(activeTab)) {
      target = calendarView;
      calendarView.bind(notes, dateFormat, selectedDate);
    } else if (TAB_SEARCH.equals(activeTab)) {
      target = searchView;
      searchView.bind(notes, folders, searchQuery, searchFolderFilter, dateFormat);
    } else {
      target = homeView;
      homeView.bind(notes, dateFormat);
    }

    if (currentContentView != target) {
      if (currentContentView == noteDetailView) {
        noteDetailView.releaseMedia();
      } else if (currentContentView == playbackView) {
        playbackView.releasePlayer();
      }
      contentContainer.removeAllViews();
      contentContainer.addView(
          target,
          new FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
      currentContentView = target;
    }
  }

  private void bindRecordingView() {
    recordingView.bind(
        draftTitle,
        draftTranscript,
        speechController.isListening(),
        draftMood,
        draftFolder,
        folders,
        recordingMessage);
  }

  private void updateNavHighlight() {
    styleNavButton(navHome, TAB_HOME.equals(activeTab));
    styleNavButton(navLibrary, TAB_LIBRARY.equals(activeTab));
    styleNavButton(navCalendar, TAB_CALENDAR.equals(activeTab));
    styleNavButton(navSearch, TAB_SEARCH.equals(activeTab));
  }

  private void styleNavButton(Button button, boolean selected) {
    button.setTextColor(
        ContextCompat.getColor(
            this, selected ? R.color.primary_coral : R.color.text_secondary));
    button.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
  }

  private void toggleRecordingMic() {
    if (speechController.isListening()) {
      speechController.stopSpeech();
    } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED) {
      speechController.startSpeech(draftTranscript, legacySpeechLauncher::launch);
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }
  }

  private void saveDraftNote() {
    if (speechController.isListening()) {
      recordingMessage = getString(R.string.recording_error_still_listening);
      bindRecordingView();
      return;
    }
    if (draftTranscript.trim().isEmpty()) {
      recordingMessage = getString(R.string.recording_error_empty_transcript);
      bindRecordingView();
      return;
    }

    speechController.stopAudioCapture();
    speechController.flushSessionAudio();

    long now = System.currentTimeMillis();
    ByteArrayOutputStream buffer = draftViewModel.getAudioBuffer();
    byte[] pcm = null;
    if (buffer != null) {
      byte[] raw;
      synchronized (buffer) {
        raw = buffer.toByteArray();
      }
      if (AudioUtils.hasNonZeroPcm(raw)) {
        pcm = raw;
      }
    }

    File audioFile = null;
    boolean audioSaved = true;
    if (pcm != null) {
      try {
        audioFile = File.createTempFile("note_", ".mp3", getFilesDir());
        AudioUtils.writeMp3File(audioFile, pcm);
      } catch (Exception e) {
        audioSaved = false;
      }
    }
    if (!audioSaved) {
      if (audioFile != null) {
        audioFile.delete();
      }
      recordingMessage = getString(R.string.recording_error_save_audio);
      bindRecordingView();
      return;
    }

    String audioUri = audioFile != null ? audioFile.toURI().toString() : "";
    AudioSource source = audioUri.isEmpty() ? AudioSource.UNKNOWN : AudioSource.RECORDED;
    String finalTitle =
        draftTitle.trim().isEmpty() ? getString(R.string.recording_default_title) : draftTitle;
    String finalFolder =
        draftFolder.trim().isEmpty() ? FolderStorage.DEFAULT_GENERAL_FOLDER : draftFolder.trim();

    Note created =
        new Note(finalTitle, draftTranscript, draftMood, finalFolder, now, audioUri, source);
    List<Note> updated = new ArrayList<>(notes.size() + 1);
    updated.add(created);
    updated.addAll(notes);

    List<Note> saved = storageError ? null : repository.saveNotesPreservingTranscriptions(updated);
    if (saved == null) {
      if (audioFile != null) {
        audioFile.delete();
      }
      recordingMessage = getString(R.string.recording_error_save_note);
      bindRecordingView();
      return;
    }

    notes = saved;
    if (repository.ensureFolderExists(finalFolder, folders) && !folders.contains(finalFolder)) {
      List<String> nextFolders = new ArrayList<>(folders);
      nextFolders.add(finalFolder);
      folders = nextFolders;
    }
    speechController.cancelSpeech();
    draftViewModel.setAudioBuffer(null);
    draftTranscript = "";
    draftTitle = "";
    draftFolder = "";
    recordingMessage = "";
    recordingElapsedSeconds = 0;
    recordingView.setElapsedSeconds(0);
    editing = false;
    renderCurrentScreen();
  }

  private void handleCancelRecording() {
    boolean hasAudio =
        draftViewModel.getAudioBuffer() != null && draftViewModel.getAudioBuffer().size() > 0;
    boolean hasUnsavedWork =
        speechController.isListening()
            || !draftTranscript.trim().isEmpty()
            || !draftTitle.trim().isEmpty()
            || hasAudio;
    if (!hasUnsavedWork) {
      discardDraftAndExitComposer();
      return;
    }
    dismissActiveDialog();
    activeDialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.discard_draft_title)
            .setMessage(R.string.discard_draft_message)
            .setPositiveButton(
                R.string.discard_draft_confirm, (ignored, which) -> discardDraftAndExitComposer())
            .setNegativeButton(R.string.discard_draft_cancel, null)
            .create();
    activeDialog.show();
  }

  private void discardDraftAndExitComposer() {
    speechController.cancelSpeech();
    draftViewModel.setAudioBuffer(null);
    draftTranscript = "";
    draftTitle = "";
    draftFolder = "";
    recordingMessage = "";
    recordingElapsedSeconds = 0;
    recordingView.setElapsedSeconds(0);
    editing = false;
    renderCurrentScreen();
  }

  private void showExitConfirmation() {
    dismissActiveDialog();
    activeDialog =
        new AlertDialog.Builder(this)
            .setTitle(R.string.exit_confirm_title)
            .setMessage(R.string.exit_confirm_message)
            .setPositiveButton(R.string.exit_confirm_yes, (ignored, which) -> finish())
            .setNegativeButton(R.string.exit_confirm_no, null)
            .create();
    activeDialog.show();
  }

  private boolean updateNote(@NonNull Note current, @NonNull Note replacement) {
    if (storageError) {
      return false;
    }
    int index = NoteRepository.indexOfNote(notes, current);
    if (index < 0) {
      return false;
    }
    List<Note> updated = new ArrayList<>(notes);
    updated.set(index, replacement);
    List<Note> saved = repository.saveNotesPreservingTranscriptions(updated);
    if (saved == null) {
      return false;
    }
    notes = saved;
    renderCurrentScreen();
    return true;
  }

  private void confirmDeleteNote(@NonNull Note target) {
    dismissActiveDialog();
    activeDialog =
        DeleteNoteConfirmation.show(
            this,
            target.getTitle(),
            () -> performDeleteNote(target),
            () -> activeDialog = null);
  }

  private void performDeleteNote(@NonNull Note target) {
    int index = NoteRepository.indexOfNote(notes, target);
    if (index < 0) {
      Toast.makeText(this, R.string.delete_note_not_found, Toast.LENGTH_LONG).show();
      return;
    }
    List<Note> updated = new ArrayList<>(notes);
    updated.remove(index);
    List<Note> saved = storageError ? null : repository.saveNotesPreservingTranscriptions(updated);
    if (saved == null) {
      Toast.makeText(this, R.string.delete_note_failed, Toast.LENGTH_LONG).show();
      return;
    }
    notes = saved;
    TranscriptionScheduler.cancel(this, target.getJobId());
    cleanupOrphanedRecording(target, saved);
    if (target.getId().equals(selectedNoteId)) {
      selectedNoteId = null;
      noteDetailView.releaseMedia();
    }
    renderCurrentScreen();
  }

  private void cleanupOrphanedRecording(Note deleted, List<Note> remaining) {
    String uriRaw = deleted.getAudioUri();
    if (uriRaw.trim().isEmpty()) {
      return;
    }
    for (Note note : remaining) {
      if (uriRaw.equals(note.getAudioUri())) {
        return;
      }
    }
    Uri parsed = Uri.parse(uriRaw);
    if ("file".equalsIgnoreCase(parsed.getScheme()) && parsed.getPath() != null) {
      File file = new File(parsed.getPath());
      if (AudioUtils.isManagedRecording(file, getFilesDir())) {
        file.delete();
      }
    }
  }

  private void handlePickedAudioUri(@Nullable Uri uri) {
    if (uri == null) {
      return;
    }
    boolean persistent = takePersistableReadPermission(uri);
    if (!persistent) {
      Toast.makeText(this, R.string.upload_permission_temp, Toast.LENGTH_LONG).show();
      return;
    }

    Note current = findSelectedNote();
    if (current != null) {
      // Detail audio attachment is for playback only; only Home upload sends audio for transcription.
      if (updateNote(current, current.withAudio(uri.toString(), AudioSource.UPLOADED))) {
        Toast.makeText(this, R.string.upload_attach_success, Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, R.string.upload_save_failed, Toast.LENGTH_LONG).show();
      }
      return;
    }

    String jobId = UUID.randomUUID().toString();
    String displayName = AudioFiles.contentDisplayName(getContentResolver(), uri);
    String title = displayName != null ? displayName : getString(R.string.upload_default_title);
    Note uploaded =
        new Note(
            UUID.randomUUID().toString(),
            title,
            "",
            UiLabels.MOOD_NORMAL,
            FolderStorage.DEFAULT_UPLOAD_FOLDER,
            System.currentTimeMillis(),
            uri.toString(),
            AudioSource.UPLOADED,
            jobId,
            TranscriptionStatus.QUEUED,
            "");
    List<Note> updated = new ArrayList<>(notes.size() + 1);
    updated.add(uploaded);
    updated.addAll(notes);
    List<Note> saved = storageError ? null : repository.saveNotesPreservingTranscriptions(updated);
    Note noteToTranscribe = null;
    if (saved != null) {
      notes = saved;
      for (Note item : saved) {
        if (jobId.equals(item.getJobId())) {
          noteToTranscribe = item;
          break;
        }
      }
    }
    if (noteToTranscribe == null) {
      Toast.makeText(this, R.string.upload_save_failed, Toast.LENGTH_LONG).show();
      return;
    }
    if (repository.ensureFolderExists(FolderStorage.DEFAULT_UPLOAD_FOLDER, folders)
        && !folders.contains(FolderStorage.DEFAULT_UPLOAD_FOLDER)) {
      List<String> nextFolders = new ArrayList<>(folders);
      nextFolders.add(FolderStorage.DEFAULT_UPLOAD_FOLDER);
      folders = nextFolders;
    }
    TranscriptionScheduler.enqueue(this, noteToTranscribe);
    scheduleWorkerPollingIfNeeded();
    Toast.makeText(this, R.string.upload_started, Toast.LENGTH_SHORT).show();
    playbackUri = uri;
    renderCurrentScreen();
  }

  private boolean takePersistableReadPermission(@NonNull Uri uri) {
    try {
      getContentResolver()
          .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      return true;
    } catch (SecurityException e) {
      for (UriPermission grant : getContentResolver().getPersistedUriPermissions()) {
        if (uri.equals(grant.getUri()) && grant.isReadPermission()) {
          return true;
        }
      }
      return false;
    }
  }

  private void retryTranscription(@NonNull Note target) {
    if (target.getTranscription() != TranscriptionStatus.FAILED) {
      return;
    }
    String oldJob = target.getJobId();
    String newJob = UUID.randomUUID().toString();
    Note queued =
        target.withTranscription(target.getBody(), newJob, TranscriptionStatus.QUEUED, "");
    if (updateNote(target, queued)) {
      for (Note item : notes) {
        if (newJob.equals(item.getJobId()) && item.getTranscription().isPending()) {
          TranscriptionScheduler.enqueue(this, item);
          scheduleWorkerPollingIfNeeded();
          break;
        }
      }
      if (!oldJob.trim().isEmpty() && !oldJob.equals(newJob)) {
        TranscriptionScheduler.cancel(this, oldJob);
      }
    }
  }

  private void promptCreateFolder() {
    if (storageError) {
      return;
    }
    dismissActiveDialog();
    activeDialog =
        FolderDialogs.showCreateDialog(
            this,
            folders,
            name -> {
              List<String> updated = repository.createFolder(name, folders);
              if (updated != null) {
                folders = updated;
                renderCurrentScreen();
              } else {
                Toast.makeText(this, R.string.folder_create_failed, Toast.LENGTH_LONG).show();
              }
            });
  }

  private void promptRenameFolder(@NonNull String oldName) {
    if (storageError) {
      return;
    }
    dismissActiveDialog();
    activeDialog =
        FolderDialogs.showRenameDialog(
            this,
            oldName,
            folders,
            newName -> {
              NoteRepository.LibrarySnapshot snapshot =
                  repository.renameFolder(oldName, newName, notes, folders);
              if (snapshot != null) {
                notes = snapshot.notes;
                folders = snapshot.folders;
                if (oldName.equals(searchFolderFilter)) {
                  searchFolderFilter = newName;
                }
                renderCurrentScreen();
              } else {
                Toast.makeText(this, R.string.folder_rename_failed, Toast.LENGTH_LONG).show();
              }
            });
  }

  private void promptDeleteFolder(@NonNull String targetFolder) {
    if (storageError) {
      return;
    }
    String fallback = FolderStorage.DEFAULT_GENERAL_FOLDER;
    if (fallback.equals(targetFolder)) {
      for (String candidate : folders) {
        if (!targetFolder.equals(candidate)) {
          fallback = candidate;
          break;
        }
      }
    }
    final String finalFallback = fallback;
    dismissActiveDialog();
    activeDialog =
        FolderDialogs.showDeleteConfirmation(
            this,
            targetFolder,
            finalFallback,
            () -> {
              NoteRepository.LibrarySnapshot snapshot =
                  repository.deleteFolder(targetFolder, finalFallback, notes, folders);
              if (snapshot != null) {
                notes = snapshot.notes;
                folders = snapshot.folders;
                if (targetFolder.equals(searchFolderFilter)) {
                  searchFolderFilter = null;
                }
                renderCurrentScreen();
              } else {
                Toast.makeText(this, R.string.folder_delete_failed, Toast.LENGTH_LONG).show();
              }
            },
            () -> activeDialog = null);
  }

  private void promptMoveNoteFolder(@NonNull Note target) {
    if (storageError) {
      return;
    }
    dismissActiveDialog();
    activeDialog =
        FolderDialogs.showMoveNoteDialog(
            this,
            folders,
            target.getFolder(),
            destination -> {
              if (updateNote(target, target.withFolder(destination))) {
                if (repository.ensureFolderExists(destination, folders)
                    && !folders.contains(destination)) {
                  List<String> nextFolders = new ArrayList<>(folders);
                  nextFolders.add(destination);
                  folders = nextFolders;
                  renderCurrentScreen();
                }
                String label = UiLabels.folderLabel(this, destination);
                Toast.makeText(
                        this, getString(R.string.folder_moved_toast, label), Toast.LENGTH_SHORT)
                    .show();
              } else {
                Toast.makeText(this, R.string.folder_move_failed, Toast.LENGTH_LONG).show();
              }
            });
  }

  @Nullable
  private Note findSelectedNote() {
    if (selectedNoteId == null) {
      return null;
    }
    for (Note note : notes) {
      if (selectedNoteId.equals(note.getId())) {
        return note;
      }
    }
    return null;
  }

  private boolean hasPendingTranscriptions() {
    for (Note note : notes) {
      if (note.getTranscription().isPending()) {
        return true;
      }
    }
    return false;
  }

  private void scheduleWorkerPollingIfNeeded() {
    handler.removeCallbacks(workerPollTask);
    if (hasPendingTranscriptions()) {
      handler.postDelayed(workerPollTask, WORKER_REFRESH_INTERVAL_MS);
    }
  }

  private void refreshFromStorage() {
    try {
      NoteRepository.LibrarySnapshot snapshot = repository.loadLibrary();
      storageError = false;
      notes = snapshot.notes;
      folders = snapshot.folders;
      renderCurrentScreen();
    } catch (JSONException e) {
      storageError = true;
      renderCurrentScreen();
    }
  }

  private void dismissActiveDialog() {
    if (activeDialog != null) {
      activeDialog.dismiss();
      activeDialog = null;
    }
  }
}
