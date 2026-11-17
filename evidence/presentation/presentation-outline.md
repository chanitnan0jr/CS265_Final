# Voice2Note — Presentation Deck Outline & Code Walkthrough Guide (`REQ-5.3`, `REQ-5.4`)

## 1. Slide-by-Slide Presentation Structure (10 Slides)

| Slide | Title | Key Points & Visuals | Rubric Link |
|---|---|---|---|
| 1 | **Voice2Note — Thai Voice Journal** | Problem statement, team scope, Java + XML Views architecture | `REQ-1.1` |
| 2 | **Essential Features & MVP Scope** | `Home`, `Recording + STT`, `Library + Folders`, `Scoped Search`, `Calendar`, `Note Detail + Playback`, `WorkManager Upload` | `REQ-1.1` |
| 3 | **End-to-End User Journey** | Visual strip from `evidence/screens/01-home-thai.png` → `08-delete-confirm-dialog.png` showing real build output | `REQ-5.1` |
| 4 | **Architecture (`UI → Repository → Data Source`)** | Package diagram (`feature/*`, `speech/`, `transcription/`, `data/`) + why Java/XML + `SharedPreferences` | `REQ-2.1`, `REQ-1.3` |
| 5 | **Main Mechanism: Thai Speech → Persistent Note** | Sequence flow: `RecordingView` → `MainActivity` → `SpeechCaptureController` (`SpeechDraft` + `AudioRecord` tee) → `NoteRepository` → `NoteStorage` | `REQ-2.1`, `REQ-1.4` |
| 6 | **Technical Challenge: Odd Folder Grid Bug** | **Problem:** Notes disappeared under odd folder counts (`measH=0`). **Cause:** `View` + `AT_MOST` (`2038px`). **Fix:** `Space(0, 0, 1f)`. **After:** `29/29` instrumented tests pass + `04-library-folders.png` | `REQ-2.2–2.4`, `REQ-5.3` |
| 7 | **Multi-Device, Locale & Theme Compatibility** | Side-by-side screenshots: Phone vs `sw600dp` Tablet rail, Landscape, Thai/English, Light/Dark (`evidence/device-compatibility/`) | `REQ-3.1–3.4` |
| 8 | **Navigation, Deletion & Lifecycle Safety** | Back routes across all tabs, exit/unsaved-draft dialogs, delete Cancel vs Confirm, `ON_STOP` pause/resume + detach-reattach fix | `REQ-4.1–4.4` |
| 9 | **Automated Testing & Course Mapping** | `22/22` JVM unit + `29/29` Android instrumented tests; `BM02–BM09` + OS concurrency/Database integrity/DSP resampling | `REQ-1.2–1.4` |
| 10 | **Attribution, AI Disclosure & Live Demo** | Team code vs AndroidX/`jump3r`, honest AI disclosure (`docs/attribution.md`), live physical phone demo (`STT → Save → Playback → Delete`) | `REQ-5.2`, `REQ-5.4` |

---

## 2. Team Code Walkthrough Assignment (`Event Flow → Data Flow → Error Handling`)

| Owner Role | Modules & Files | Must Explain Clearly Without AI |
|---|---|---|
| **Member A — Shell, Nav & Lifecycle** | `MainActivity.java`, `activity_main.xml`, `layout-sw600dp/activity_main.xml` | 1. **Event:** Tab clicks & `OnBackPressedCallback` priorities (rename -> recording unsaved check -> detail/playback -> tab to Home -> exit dialog).<br>2. **Data:** `onSaveInstanceState` keys (`STATE_*`) restore draft/tab/note selection across rotation.<br>3. **Errors:** Storage `JSONException` banner fails closed without overwriting; missing permission shows rationale/settings guidance. |
| **Member B — Recording, Speech & Audio** | `RecordingView.java`, `SpeechCaptureController.java`, `SpeechDraft.java`, `RecordingDraftViewModel.java`, `AudioUtils.java` | 1. **Event:** Mic click checks `RECORD_AUDIO`, starts `SpeechRecognizer` (`th-TH`), edits disable live mic.<br>2. **Data:** `SpeechDraft` generation counter drops stale recognizer callbacks; API 33+ `Voice2NoteMicTee` captures 16 kHz PCM into `RecordingDraftViewModel`; Save encodes MP3 via `jump3r`.<br>3. **Errors:** 15 s timeout, recognizer error, or `onStop` cancellation always keep captured draft text; path traversal blocked in `AudioUtils.isManagedRecording`. |
| **Member C — Notes, Folders & Search** | `NoteRepository.java`, `NoteStorage.java`, `FolderStorage.java`, `NoteFilters.java`, `LibraryView.java`, `SearchView.java`, `CalendarView.java` | 1. **Event:** Folder CRUD dialogs, source pills, `SearchView` query `TextWatcher` + folder `Spinner`.<br>2. **Data:** UUID `noteId` primary keys; separate `items` and `folders` JSON keys; `NoteRepository` updates notes + folders together (rolling back notes if folder save fails).<br>3. **Errors:** Blank/duplicate folder names rejected; Delete requires confirmation dialog (`DeleteNoteConfirmation` / `FolderDialogs`); Cancel leaves data intact. |
| **Member D — Transcription & Playback** | `TranscriptionScheduler.java`, `TranscriptionWorker.java`, `TranscriptionClient.java`, `NoteDetailView.java`, `PlaybackView.java` | 1. **Event:** Upload URI picker -> `WorkManager` -> 3 s Activity poll; Play/Pause & `SeekBar` scrub.<br>2. **Data:** `TranscriptionClient` `POST /api/v1/transcriptions` + `GET .../jobId`; `savePreservingTranscriptions` prevents UI edits from clobbering finished transcripts.<br>3. **Errors:** Empty `TRANSCRIPTION_BASE_URL` or HTTP 4xx sets `FAILED` + enables Retry; HTTP 5xx/network/`JSONException` triggers `Result.retry()` up to 3 attempts; `ON_STOP` pauses `MediaPlayer` even after detach/reattach. |
