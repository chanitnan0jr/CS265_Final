# Voice2Note — Presentation Deck Outline & Code Walkthrough Guide (`REQ-5.3`, `REQ-5.4`)

- **Interactive Slide Deck:** Open `evidence/presentation/Voice2Note-Slides.html` in a browser (use `←` / `→` keys or click navigation; `Ctrl+P` exports 1 slide per landscape page cleanly).
- **Target Presentation Time:** ~8–9 minutes total (10 slides + 2-minute live device demo).

## 1. Slide-by-Slide Presentation Structure (10 Slides)

| Slide | Time | Title | Key Points & Visuals | Rubric Link |
|---|---|---|---|---|
| 1 | 0:30 | **Voice2Note — Thai Voice Journal** | Voice2Note logo, problem statement, Java + XML Views architecture | `REQ-1.1` |
| 2 | 0:45 | **Essential Features & MVP Scope (`F-01`–`F-10`)** | Home, Recording + Thai STT, Library + Folders, Scoped Search, Calendar, Detail + Playback, WorkManager Upload (out-of-scope deferred) | `REQ-1.1` |
| 3 | 1:00 | **End-to-End User Journey (`8` Steps)** | Visual strip: `evidence/screens/01-home-thai.png` → `08-delete-confirm-dialog.png` showing real build results | `REQ-5.1` |
| 4 | 0:45 | **Architecture (`UI → Repository → Data Source`)** | `feature/*`, `speech/`, `transcription/`, `data/` packages; Folders vs Playback distinction | `REQ-2.1`, `REQ-1.3` |
| 5 | 0:50 | **Main Mechanism: Thai Speech → Note** | Sequence flow: `RecordingView` → `MainActivity` → `SpeechCaptureController` (`SpeechDraft` + `AudioRecord` tee) → `NoteRepository` → `NoteStorage` | `REQ-2.1`, `REQ-1.4` |
| 6 | 1:00 | **Technical Challenge: Odd Folder Grid Bug** | **Problem:** Notes disappeared (`measH=0`). **Cause:** `View` + `AT_MOST` (`2038px`). **Fix:** `Space(0, 0, 1f)`. **After:** `29/29` connected tests + `04-library-folders.png` | `REQ-2.2–2.4`, `REQ-5.3` |
| 7 | 0:50 | **Multi-Device, Locale & Theme Compatibility** | Phone (`1080×2400`) vs Tablet `sw600dp` (`1600×2560`), Landscape, Thai/English, Light/Dark (`evidence/device-compatibility/`) + Samsung `SM-A127F` OneUI NBSP compatibility | `REQ-3.1–3.4` |
| 8 | 0:45 | **Navigation, Deletion & Lifecycle Safety** | Back routes across all tabs, exit/unsaved-draft dialogs, delete Cancel/Confirm, `ON_STOP` pause/resume + detach-reattach fix | `REQ-4.1–4.4` |
| 9 | 0:45 | **Automated Testing & Course Mapping** | `22/22` JVM unit + `29/29` instrumented tests (`51` total); `BM02–BM09` + OS concurrency, DB integrity, DSP resampling | `REQ-1.2–1.4` |
| 10 | 1:30 | **Attribution, Scope Disclosure & Live Demo** | Team code vs AndroidX/`jump3r`/AI disclosure (`docs/attribution.md`), no-external-server boundary, live demo on Samsung `SM-A127F` | `REQ-5.2`, `REQ-5.4` |

---

## 2. Team Code Walkthrough Assignment (`Event Flow → Data Flow → Error Handling`)

| Owner Role | Modules & Files | Must Explain Clearly Without AI |
|---|---|---|
| **Member A — Shell, Nav & Lifecycle** | `MainActivity.java`, `activity_main.xml`, `layout-sw600dp/activity_main.xml` | 1. **Event:** Tab clicks & `OnBackPressedCallback` priorities (rename -> recording unsaved check -> detail/playback -> tab to Home -> exit dialog).<br>2. **Data:** `onSaveInstanceState` keys (`STATE_*`) restore draft/tab/note selection across rotation.<br>3. **Errors:** Storage `JSONException` banner fails closed without overwriting; missing permission shows rationale/settings guidance. |
| **Member B — Recording, Speech & Audio** | `RecordingView.java`, `SpeechCaptureController.java`, `SpeechDraft.java`, `RecordingDraftViewModel.java`, `AudioUtils.java` | 1. **Event:** Mic click checks `RECORD_AUDIO`, starts `SpeechRecognizer` (`th-TH`), edits disable live mic.<br>2. **Data:** `SpeechDraft` generation counter drops stale recognizer callbacks; API 33+ `Voice2NoteMicTee` captures 16 kHz PCM into `RecordingDraftViewModel` (API < 33 uses `RecognizerIntent`); Save encodes MP3 via `jump3r`.<br>3. **Errors:** 15 s timeout, recognizer error, or `onStop` cancellation always keep captured draft text; path traversal blocked in `AudioUtils.isManagedRecording`. |
| **Member C — Notes, Folders & Search** | `NoteRepository.java`, `NoteStorage.java`, `FolderStorage.java`, `NoteFilters.java`, `LibraryView.java`, `SearchView.java`, `CalendarView.java` | 1. **Event:** Folder CRUD dialogs, source pills, `SearchView` query `TextWatcher` + folder `Spinner`.<br>2. **Data:** UUID `noteId` primary keys; separate `items` and `folders` JSON keys; `NoteRepository` updates notes + folders together (rolling back notes if folder save fails).<br>3. **Errors:** Blank/duplicate folder names rejected; Delete requires confirmation dialog (`DeleteNoteConfirmation` / `FolderDialogs`); Cancel leaves data intact. |
| **Member D — Transcription & Playback** | `TranscriptionScheduler.java`, `TranscriptionWorker.java`, `TranscriptionClient.java`, `NoteDetailView.java`, `PlaybackView.java` | 1. **Event:** Upload URI picker -> `WorkManager` -> 3 s Activity poll; Play/Pause & `SeekBar` scrub.<br>2. **Data:** `TranscriptionClient` `POST /api/v1/transcriptions` + `GET .../jobId`; `savePreservingTranscriptions` prevents UI edits from clobbering finished transcripts.<br>3. **Errors:** Empty `TRANSCRIPTION_BASE_URL` or HTTP 4xx sets `FAILED` + enables Retry; HTTP 5xx/network/`JSONException` triggers `Result.retry()` up to 3 attempts; `ON_STOP` pauses `MediaPlayer` even after detach/reattach. |
