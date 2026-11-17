# Voice2Note — Architecture, Use Cases & Data Flows (`REQ-2.1`, `G18`)

## 1. Layered Java + XML Views Architecture (`UI -> Repository -> Data Source`)

```mermaid
flowchart TB
    subgraph UI["UI Layer (Android XML Views + Java)"]
        MA["MainActivity.java\n(Navigation, Permissions, Back, Lifecycle)"]
        HV["HomeView.java\n(view_home.xml)"]
        RV["RecordingView.java + WaveformView.java\n(view_recording.xml)"]
        LV["LibraryView.java + FolderDialogs.java\n(view_library.xml)"]
        CV["CalendarView.java\n(view_calendar.xml)"]
        SV["SearchView.java + SearchControls.java\n(view_search.xml)"]
        DV["NoteDetailView.java\n(view_note_detail.xml)"]
        PV["PlaybackView.java\n(view_playback.xml)"]
    end

    subgraph Controllers["Platform & Background Controllers"]
        SC["SpeechCaptureController.java\n+ SpeechDraft.java"]
        VM["RecordingDraftViewModel.java"]
        TS["TranscriptionScheduler.java\n-> TranscriptionWorker.java\n-> TranscriptionClient.java"]
    end

    subgraph Repo["Repository & Data Layer"]
        NR["NoteRepository.java\n+ NoteFilters.java"]
        NS["NoteStorage.java\n(SharedPreferences 'items')"]
        FS["FolderStorage.java\n(SharedPreferences 'folders')"]
        AU["AudioUtils.java & AudioFiles.java\n(WAV/MP3, Resample, URI names)"]
    end

    MA --> HV & RV & LV & CV & SV & DV & PV
    MA --> SC & VM & TS & NR
    NR --> NS & FS
    TS --> NS & AU
    SC --> VM & AU
```

### Folder vs. Audio Playback Distinction (`G07`)
- **Folders (`FolderStorage`):** Persistent category labels (`การเรียน`, `ระบายความรู้สึก`, `งาน / โปรเจกต์`, `ไอเดีย`, `ทั่วไป`, or user-created folders) that group notes for browsing (`LibraryView`) and search scoping (`SearchView`).
- **Audio Playback (`NoteDetailView` / `PlaybackView`):** Plays the audio file attached to a single selected `Note` via `MediaPlayer`. Folders do **not** act as ordered audio playlists.

---

## 2. Main Function Mechanism: Input → Processing → Output (`REQ-2.1`)

### Mechanism A: Voice Recording → Thai Live STT → Persistent Note

```mermaid
sequenceDiagram
    actor User
    participant RV as RecordingView
    participant MA as MainActivity
    participant SC as SpeechCaptureController
    participant SD as SpeechDraft
    participant NR as NoteRepository
    participant NS as NoteStorage

    User->>RV: Tap Mic CTA
    RV->>MA: onToggleMic()
    MA->>MA: Check RECORD_AUDIO permission
    MA->>SC: startSpeech(draftTranscript)
    SC->>SD: start(existingText) -> sessionId
    SC->>SC: Start AudioRecord pipe (API 33+) & SpeechRecognizer (th-TH)
    SC-->>RV: onPartialResults / onResults -> SpeechDraft.result(sessionId, text)
    User->>RV: Edit text, choose mood/folder, tap Save
    RV->>MA: onSave()
    MA->>MA: Validate (!listening && !transcript.isBlank())
    MA->>NR: Encode PCM->MP3 (AudioUtils) + saveNotesPreservingTranscriptions()
    NR->>NS: synchronized write() via SharedPreferences.commit()
    NS-->>MA: Updated Note list
    MA-->>RV: Return to HomeView with new Note visible
```

1. **Input:** User voice audio (`th-TH`) + keyboard edits + mood/folder/title selection.
2. **Processing:** `SpeechCaptureController` captures live STT callbacks into `SpeechDraft` (guarding against stale callbacks from prior sessions via monotonic `generation` IDs) while teeing 16 kHz mono PCM on API 33+. On Save, `MainActivity` validates that listening is stopped and transcript is non-blank, encodes PCM to MP3 (`AudioUtils.writeMp3File`) in `filesDir`, and constructs an immutable `Note` with a UUID `id`.
3. **Output:** `NoteRepository` persists the note via `NoteStorage` (`SharedPreferences.commit()`), ensures the folder is registered in `FolderStorage`, clears the draft, and updates `HomeView`.
4. **Error Paths:** Permission denied displays rationale/settings guidance; SpeechRecognizer timeout (15 s) or error preserves already captured text; storage read/write failures abort save and delete any newly written audio file without overwriting existing notes.

---

## 3. Use Case Diagram & Key Use Case Specification (`BM09 pp. 2–3`)

```mermaid
flowchart LR
    User(["User"])
    UC1(["UC-01: Record & Transcribe Voice Note"])
    UC2(["UC-02: Upload Audio for Background Transcription"])
    UC3(["UC-03: Manage Folders & Move Notes"])
    UC4(["UC-04: Scoped Search & Calendar Browse"])
    UC5(["UC-05: Review, Rename, Play & Delete Note"])

    User --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    User --> UC5
```

### Use Case Specification: `UC-01 — Record & Save Thai Voice Note`
- **Actor:** App User
- **Preconditions:** App is open on `HomeView`; device has microphone hardware (optional for text-only entry) and a speech recognition provider for live STT.
- **Basic Flow:**
  1. User taps **Start recording** (`button_home_record`) or **Create note** (`button_home_new_note`).
  2. `MainActivity` opens `RecordingView`.
  3. User taps Mic (`button_recording_mic`); `MainActivity` verifies `RECORD_AUDIO` permission and calls `SpeechCaptureController.startSpeech()`.
  4. User speaks Thai; partial and final transcripts update `input_recording_transcript`.
  5. User stops listening, optionally edits text/title/folder/mood, and taps **Save note** (`button_recording_save`).
  6. `MainActivity` encodes captured PCM to MP3 (if non-zero PCM exists) and saves via `NoteRepository`.
- **Alternative / Error Flows:**
  - *A1 (Permission denied):* Show `R.string.permission_mic_rationale` or `R.string.permission_mic_settings`; user can still type notes manually.
  - *A2 (Back pressed with unsaved draft):* `MainActivity` displays `R.string.discard_draft_title` confirmation dialog; Cancel retains draft, Confirm discards and exits composer.
  - *A3 (Save clicked while listening or blank):* Displays inline warning (`R.string.recording_error_still_listening` or `R.string.recording_error_empty_transcript`) without losing data.

---

## 4. Class Guide (For Team Walkthrough)

| Package | Class | Responsibility |
|---|---|---|
| `com.example.cs265final` | `MainActivity` | Host navigation (`Home`, `Library`, `Calendar`, `Search`, `Recording`, `Detail`, `Playback`), `OnBackPressedCallback`, runtime permissions, lifecycle state save/restore |
| `data` | `Note`, `AudioSource`, `TranscriptionStatus` | Immutable domain models with stable UUID `id` |
| `data` | `NoteStorage`, `FolderStorage`, `NoteRepository` | Thread-safe JSON persistence (`SharedPreferences`), legacy migration, folder CRUD & note reassignment |
| `data` | `NoteFilters`, `AudioUtils`, `AudioFiles` | Pure filtering/search, WAV/MP3 encoding, resampling, content URI filename lookup |
| `speech` | `SpeechDraft`, `SpeechCaptureController`, `RecordingDraftViewModel` | Session-safe transcript merging, `SpeechRecognizer` + `AudioRecord` mic-tee management, rotation-safe PCM buffer |
| `transcription` | `TranscriptionClient`, `TranscriptionWorker`, `TranscriptionScheduler` | HTTP multipart upload & polling, `WorkManager` background worker, retry/cancel scheduling |
| `feature/*` & `ui` | `HomeView`, `LibraryView`, `FolderDialogs`, `CalendarView`, `SearchControls`, `SearchView`, `SearchResultView`, `RecordingView`, `NoteDetailView`, `DeleteNoteConfirmation`, `PlaybackView`, `PlaybackHelper`, `WaveformView`, `UiLabels` | XML-inflated custom Views, dialogs, media playback lifecycle, localized label mapping |
