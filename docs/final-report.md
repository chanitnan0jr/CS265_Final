# Voice2Note — CS265 Final Project Report

**Course:** CS265 — Basic Mobile Development (Academic Year 2569)  
**Repository:** `chanitnan0jr/CS265_Final`  
**Build Under Test:** Pure Java + Android XML Views (`com.example.cs265final`, 0 `.kt` files)  
**Date:** 2026-11-17

---

## 1. Project Overview, Target Scope & Essential Functions (`REQ-1.1`)

Voice2Note is a Thai-first mobile voice journaling and note-taking Android application. It helps users record spoken thoughts (via device `SpeechRecognizer` with `th-TH`), edit transcripts, tag moods, organize notes into folders, upload existing audio files for background transcription, search across folders or the whole library, browse daily entries by calendar, and replay attached audio.

- **Technology & Architecture Choice:** Migrated cleanly from Kotlin/Jetpack Compose into **Java + Android XML Views** (`UI -> Repository -> Data source`) to maximize code readability and team explainability without adding external frameworks or changing local JSON persistence compatibility.
- **Scope Boundary:** In-scope features match CS265 requirements and MVP phases (`F-01` through `F-10`). Future phases from `Voice2Note_Pack/Roadmap-Plan.md` (RAG, semantic retrieval, karaoke word sync, and AI mood analytics) are deliberately excluded.

**Complete Essential Function Inventory:** See [feature-inventory.md](feature-inventory.md) (`F-01` Home, `F-02` Recording & Thai STT, `F-03` Note Detail/Rename/Copy/Share, `F-04` Safe Deletion, `F-05` Library & Folder CRUD, `F-06` Scoped Search, `F-07` Calendar, `F-08` Audio Playback, `F-09` Background Upload & Transcription, `F-10` Compatibility/Navigation/Lifecycle).

---

## 2. Annotated End-to-End User Flow (`REQ-5.1`)

1. **Home (`evidence/screens/01-home-thai.png`):** Shows Thai Light UI (`วันนี้อยากบันทึกอะไร?`), Record CTA, Upload Audio, New Note, Recent Notes, and navigation bar.
2. **Compose & Tag (`evidence/screens/02-recording-composer.png`):** Live transcript editor, folder selector chips, 5 mood chips (`ปกติ` selected), waveform, and Save button.
3. **Saved Result (`evidence/screens/03-home-with-note.png`):** `CS265 Demo` persists in `NoteStorage` (`SharedPreferences`) and appears immediately in Recent Notes.
4. **Folder Organization (`evidence/screens/04-library-folders.png`):** `LibraryView` renders source filters, a 2-column folder grid (`ทั่วไป`, `การเรียน`, etc.), and filtered note cards. Folders categorize notes and are distinct from single-file audio playback.
5. **Calendar View (`evidence/screens/05-calendar-journal.png`):** `CalendarView` highlights `17/11/2026` and filters notes matching that date.
6. **Folder-Scoped Search (`evidence/screens/06-search-scoped.png`):** `SearchView` filters case-insensitively across all folders or a chosen folder `Spinner` scope and displays exact match count.
7. **Note Detail (`evidence/screens/07-note-detail.png`):** Shows metadata, audio bar, transcript card, Copy/Edit/Share actions, and failed-transcription Retry controls.
8. **Safe Delete (`evidence/screens/08-delete-confirm-dialog.png`):** Prompts `ลบโน้ตนี้หรือไม่?`; Cancel keeps the note, Confirm removes the note, cancels WorkManager jobs, and cleans managed recording files.

*(Detailed step table in [user-flow.md](user-flow.md).)*

---

## 3. Architecture, Main Mechanism & Academic Mapping (`REQ-2.1`, `REQ-1.3`, `REQ-1.4`)

- **Layered Structure:**
  - **UI Views (`feature/*`, `res/layout/`):** `MainActivity` orchestrates navigation, permissions, `OnBackPressedCallback`, `onSaveInstanceState`, and worker polling; custom XML Views render each screen without touching JSON/HTTP directly.
  - **Controllers (`speech/`, `transcription/`):** `SpeechCaptureController` + `SpeechDraft` + `RecordingDraftViewModel` manage `SpeechRecognizer` and API 33+ `AudioRecord` PCM teeing; `TranscriptionScheduler` + `TranscriptionWorker` + `TranscriptionClient` handle background HTTP upload/polling via `WorkManager`.
  - **Repository & Storage (`data/`):** `NoteRepository` coordinates `NoteStorage` (`items`), `FolderStorage` (`folders`), `NoteFilters`, and `AudioUtils` (`WAV` headers, `resampleMonoPcm16`, `jump3r` MP3 encoding).
- **Main Input → Processing → Output Flow:** Documented with sequence and architecture diagrams in [architecture.md](architecture.md).
- **CS265 & Other-Course Traceability:** Mapped slide-by-slide (`BM02`–`BM09`) plus Operating Systems concurrency, Database integrity, and DSP audio resampling in [course-mapping.md](course-mapping.md).

---

## 4. Verification, Device Compatibility & Lifecycle Results (`REQ-1.2`, `REQ-3.x`, `REQ-4.x`)

All automated checks were executed on **2026-11-17** against the final Java + XML Views codebase (`--rerun-tasks`):
- **JVM Unit Tests:** `22 / 22` passed (`0` failed, `0` skipped) across `5` suites (`evidence/tests/jvm-unit-tests-combined.xml`).
- **Connected Instrumented Tests (`Medium_Phone AVD`, Android 17 / API 37):** `29 / 29` passed (`0` failed, `0` skipped) across `12` suites (`evidence/tests/connected-android-tests.xml`).
- **Device Compatibility (`REQ-3.1–3.4`):** Verified Phone (`1080×2400`) bottom nav + Tablet (`1600×2560`, `layout-sw600dp/activity_main.xml`) `176dp` rail, Landscape rotation, Thai/English string switching (`values/` vs `values-en/`), and Day/Night themes (`values-night/`), with screenshots in `evidence/device-compatibility/`.
- **Navigation, Deletion & Lifecycle (`REQ-4.1–4.4`):** Verified Back routing across all tabs/sub-screens, exit & unsaved-draft dialogs, delete confirmation (`Cancel` vs `Confirm`) on notes and folders, and `ON_STOP` playback pause even after view detach/reattach (`NoteDetailLifecycleTest`).

*(Full test matrix in [test-cases.md](test-cases.md).)*

---

## 5. Technical Challenges & Verified Fixes (`REQ-2.2–2.4`, `REQ-5.3`)

1. **Odd Folder Grid Layout Collapse (`LibraryView`):** Using a plain `View` spacer inside a `ScrollView` with `fillViewport="true"` expanded to `2038px` (`MeasureSpec.AT_MOST`), hiding all notes underneath (`measH=0`). Replacing it with `android.widget.Space(0, 0, 1f)` fixed measurement immediately. Before/after proof: `evidence/challenge/before-spacer-bug.txt` → `evidence/challenge/after-connected-tests.xml` + `evidence/screens/04-library-folders.png`.
2. **Custom View XML Inflation Crash (`SearchControls`):** Converting Compose `AndroidView` factories to XML tags triggered `InflateException: NoSuchMethodException: SearchControls.<init>(Context, AttributeSet)`. Adding `(Context, AttributeSet, int)` constructors fixed inflation (`evidence/challenge/before-xml-inflation.txt` → `SearchControlsTest`).
3. **Lifecycle Re-attach & Worker Failure Safety (Release Audit Fixes):** Updated `NoteDetailView` and `PlaybackView` to retain their `Lifecycle` reference across view detach/reattach transitions so `ON_STOP` pauses playback on subsequent visits, fixed `TranscriptionWorker` to retry on `JSONException` storage read/write failures instead of reporting premature success, and added best-effort rollback in `NoteRepository` when folder persistence fails.

*(Details in [technical-challenge.md](technical-challenge.md).)*

---

## 6. Attribution & AI Disclosure (`REQ-5.2`)

All original team code, AndroidX/LGPL (`jump3r`) dependencies, test media fixtures, and transparent AI coding/documentation assistance disclosures are documented in [attribution.md](attribution.md).

---

## 7. Team Sign-Off & Pre-Demo Physical Checklist (`REQ-5.4` Gate)

Automated emulator and unit checks do **not** exercise physical microphone acoustics or an external team backend server (`local.properties` defines only `sdk.dir`). Before live grading, complete and record on a physical Android phone:
1. Live Thai speech recognition (`Record → speak Thai → Save → Reopen → Playback`) and OS `RECORD_AUDIO` denial/recovery.
2. Live `transcriptionBaseUrl` upload (`Upload → QUEUED → PROCESSING → COMPLETED`) plus real server `FAILED → Retry`.
3. Team peer-review of this report and [presentation-outline.md](../evidence/presentation/presentation-outline.md), then check off `REQ-5.4` in [Checklist.md](../reference/Checklist.md).
