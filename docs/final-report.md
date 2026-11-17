# Voice2Note — CS265 Final Project Report

**Course:** CS265 — Basic Mobile Development (Academic Year 2569)  
**Repository:** `chanitnan0jr/CS265_Final`  
**Build Under Test:** Pure Java + Android XML Views (`com.example.cs265final`, `0` `.kt` files)  
**Date:** 2026-11-17

---

## 1. Project Overview, Target Scope & Essential Functions (`REQ-1.1`)

Voice2Note is a Thai-first mobile voice journaling and note-taking Android application. Users record spoken thoughts via the device `SpeechRecognizer` (`th-TH`), edit transcripts, tag moods, organize notes into folders, upload existing audio files for background transcription, search across folders or the full library, browse daily entries in a calendar, and replay attached audio.

- **Technology & Architecture Choice:** Migrated completely from Kotlin + Jetpack Compose to **Java + Android XML Views** (`UI -> Repository -> Data source`) for readability and team explainability without adding unnecessary libraries or breaking legacy JSON persistence.
- **Brand Identity:** Uses the Voice2Note logo across adaptive/mipmap launcher icons (`ic_launcher`, `ic_launcher_round`) and the Home header badge ([view_home.xml](../app/src/main/res/layout/view_home.xml)).
- **Scope Boundary:** In-scope functions (`F-01`–`F-10`) cover CS265 rubric criteria and core MVP features; future roadmap phases (RAG, semantic retrieval, karaoke word sync, AI mood analytics) are excluded.

*(Complete table of all 10 functions `F-01`–`F-10`: [feature-inventory.md](feature-inventory.md).)*

---

## 2. Annotated End-to-End User Flow (`REQ-5.1`)

| Step | Screen | User Action | Verified Result | Screenshot |
|---|---|---|---|---|
| 1 | `HomeView` (`F-01`) | Open Voice2Note | Displays Home CTA, upload/create buttons, recent notes, and bottom nav | `evidence/screens/01-home-thai.png` |
| 2 | `RecordingView` (`F-02`) | Tap **สร้างโน้ตใหม่**, type/edit transcript, choose folder & mood | Shows transcript editor, 5 mood chips, folder chips, waveform, Save button | `evidence/screens/02-recording-composer.png` |
| 3 | `HomeView` (`F-01`) | Tap **บันทึกโน้ต** | Validates non-empty draft, saves to `NoteStorage`, displays card on Home | `evidence/screens/03-home-with-note.png` |
| 4 | `LibraryView` (`F-05`) | Tap **คลังโน้ต** | Displays source filter pills, 2-column folder grid, and note cards | `evidence/screens/04-library-folders.png` |
| 5 | `CalendarView` (`F-07`) | Tap **บันทึกรายวัน** | Highlights date (`17/11/2026`), renders dot indicators & day notes | `evidence/screens/05-calendar-journal.png` |
| 6 | `SearchView` (`F-06`) | Tap **ค้นหา**, type `"CS265"`, pick folder scope | Filters Thai/English case-insensitively and reports match count | `evidence/screens/06-search-scoped.png` |
| 7 | `NoteDetailView` (`F-03`) | Tap saved note card | Displays metadata, audio player bar, transcript, Rename/Copy/Share/Retry | `evidence/screens/07-note-detail.png` |
| 8 | `DeleteNoteConfirmation` (`F-04`) | Open `⋮` menu → **ลบโน้ต** | Prompts confirmation dialog; Cancel preserves note, Confirm removes & cleans files | `evidence/screens/08-delete-confirm-dialog.png` |

*(Transition rules and Back graph: [user-flow.md](user-flow.md).)*

---

## 3. Architecture, Main Mechanism & Course Mapping (`REQ-2.1`, `REQ-1.3`, `REQ-1.4`)

- **UI Views (`feature/*`, `res/layout/`):** `MainActivity` owns tab routing, `OnBackPressedCallback`, `RECORD_AUDIO` permission prompts, `onSaveInstanceState`, and `WorkManager` polling. Feature views (`HomeView`, `LibraryView`, `CalendarView`, `SearchView`, `RecordingView`, `NoteDetailView`, `PlaybackView`) handle rendering and callbacks only.
- **Controllers (`speech/`, `transcription/`):** `SpeechCaptureController` + `SpeechDraft` + `RecordingDraftViewModel` coordinate `SpeechRecognizer` and API 33+ `AudioRecord` PCM capture (legacy intent fallback on `< API 33`); `TranscriptionScheduler` + `TranscriptionWorker` + `TranscriptionClient` handle background upload/polling.
- **Repository & Persistence (`data/`):** `NoteRepository` coordinates `NoteStorage` (`items`), `FolderStorage` (`folders`), `NoteFilters`, and `AudioUtils` (`WAV`, `resampleMonoPcm16`, `jump3r` MP3 encoding).

*(Sequence diagram & class inventory: [architecture.md](architecture.md); BM02–BM09 & OS/DB/DSP mapping: [course-mapping.md](course-mapping.md).)*

---

## 4. Verification, Compatibility & Lifecycle Results (`REQ-1.2`, `REQ-3.x`, `REQ-4.x`)

- **JVM Unit Tests (`./gradlew :app:testDebugUnitTest --rerun-tasks`):** **`22 / 22` passed** (`0` failed, `0` skipped) across `5` suites (`evidence/tests/jvm-unit-tests-combined.xml`).
- **Android Instrumented Tests (`AndroidJUnitRunner` on `Medium_Phone AVD`, API 37):** **`29 / 29` passed** (`0` failed, `0` skipped) across `12` suites (`evidence/tests/connected-android-tests.xml` + final run after `61c18f4`).
- **Physical Device Verification (`Samsung Galaxy A12s`, `SM-A127F`, Android 11 / API 30, `720×1600 @ 300 dpi`):** Verified legacy `< API 33` runtime compatibility, detected & fixed Samsung OneUI Thai trailing `\u00a0` padding in `CompatibilityAndFolderTest` and `CalendarAndDetailTest` (`6a65b49`), and verified latest APK installation (`61c18f4`).
- **Device Compatibility (`REQ-3.1–3.4`):** Verified Phone (`1080×2400`) bottom nav vs Tablet (`1600×2560`, `layout-sw600dp/activity_main.xml`) `176dp` rail, Landscape orientation, Thai/English localization (`values/` vs `values-en/`), and Light/Dark themes (`values-night/`), captured in `evidence/device-compatibility/`.
- **Navigation, Deletion & Lifecycle (`REQ-4.1–4.4`):** Verified Back routing across all tabs/sub-screens, exit & unsaved-draft dialogs, delete confirmation (`Cancel` vs `Confirm`) for both notes and folders, and `ON_STOP` playback pause even after view detach/reattach.

*(Case-by-case Positive / Negative / Boundary matrix: [test-cases.md](test-cases.md).)*

---

## 5. Technical Challenges & Verified Fixes (`REQ-2.2–2.4`, `REQ-5.3`)

1. **Odd Folder Grid Collapse (`LibraryView`):** Plain `View` spacer inside `ScrollView` (`fillViewport="true"`) expanded to `2038px` under `MeasureSpec.AT_MOST`, hiding notes (`measH=0`). Replaced with `android.widget.Space(0, 0, 1f)`. Evidence: `evidence/challenge/before-spacer-bug.txt` → `evidence/challenge/after-connected-tests.xml` & `04-library-folders.png`.
2. **`SearchControls` XML Inflation Crash (`NoSuchMethodException`):** Added `(Context, AttributeSet)` and `(Context, AttributeSet, int)` constructors (`evidence/challenge/before-xml-inflation.txt` → `SearchControlsTest`).
3. **Lifecycle Re-attach & Worker Error Hardening:** Preserved `Lifecycle` observer binding across `NoteDetailView`/`PlaybackView` detach-reattach transitions, updated `TranscriptionWorker` to retry on storage `JSONException`, and added best-effort note rollback in `NoteRepository` when folder save fails.

*(Full technical root-cause analysis: [technical-challenge.md](technical-challenge.md).)*

---

## 6. Attribution & AI Disclosure (`REQ-5.2`)

Original team code, Jetpack/AndroidX & `jump3r` libraries, team-provided logo/audio assets, and AI-assisted development/verification scope are disclosed transparently in [attribution.md](attribution.md).

---

## 7. Known Boundaries & Live-Demo Checklist (`REQ-5.4`)

1. **External Transcription Server:** No external backend server is deployed (`local.properties` sets only `sdk.dir`); HTTP upload/polling behavior is verified via local `ServerSocket` tests (`TranscriptionClientTest`), and empty `TRANSCRIPTION_BASE_URL` cleanly transitions uploads to `FAILED` with localized guidance and Retry.
2. **Physical Microphone STT:** Hardware mic acoustic recognition (`th-TH`) and OS permission prompt denial/grant are verified interactively during live team demo on the Samsung `SM-A127F` device (`GoogleRecognitionService` confirmed enabled).

---

## 8. Coherence & Cross-Document Audit (`REQ-5.4`)

- **Structure & Non-Repetition:** Sections follow rubric order (`Overview → User Flow → Architecture → Verification → Challenges → Attribution → Scope/Audit`), referencing specialized detail files instead of duplicating full tables.
- **Terminology & Metric Alignment:** Function IDs (`F-01`–`F-10`), class names, and test counts (`22/22` unit tests + `29/29` instrumented tests) agree across `README.md`, `docs/*.md`, `evidence/presentation/presentation-outline.md`, `evidence/presentation/Voice2Note-Slides.html`, and `reference/Checklist.md`.
- **Link & Captions Check:** All referenced paths under `docs/`, `evidence/screens/`, `evidence/device-compatibility/`, `evidence/challenge/`, `evidence/tests/`, and `evidence/presentation/` exist and match actual captures and valid single-root XML reports.
