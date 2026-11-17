# Voice2Note

Voice2Note is a Thai-first Android voice journal built with **Java and Android XML Views**. The app transcribes live Thai speech (`th-TH`) using the device's configured Android speech service, supports manual editing, mood tagging, persistent folder organization, folder-scoped search, daily journal calendar browsing, local audio playback, and background audio upload transcription via `WorkManager`.

## Project structure

```text
app/src/main/java/com/example/cs265final/
├── MainActivity.java            # Navigation host, permissions, Back & exit guards, lifecycle state
├── data/                        # Note/Folder models, NoteRepository, NoteStorage, FolderStorage, NoteFilters, AudioUtils
├── feature/
│   ├── home/                    # HomeView (view_home.xml)
│   ├── recording/               # RecordingView (view_recording.xml)
│   ├── notes/                   # LibraryView, NoteDetailView, FolderDialogs, DeleteNoteConfirmation
│   ├── calendar/                # CalendarView (view_calendar.xml)
│   ├── search/                  # SearchView, SearchControls, SearchResultView
│   └── playback/                # PlaybackView, PlaybackHelper (view_playback.xml)
├── speech/                      # SpeechCaptureController, SpeechDraft, RecordingDraftViewModel
├── transcription/               # TranscriptionClient, TranscriptionWorker, TranscriptionScheduler
└── ui/                          # WaveformView, UiLabels
```

Resources include Thai-first defaults (`res/values/strings.xml`), English translations (`res/values-en/strings.xml`), Light/Dark themes (`res/values/` & `res/values-night/`), and tablet navigation rail support (`res/layout-sw600dp/activity_main.xml`).

## Background transcription

Voice2Note supports uploading MP3, M4A, and WAV files to the transcription API described in [Roadmap-Plan.md](Voice2Note_Pack/Roadmap-Plan.md). Configure the server root in the untracked `local.properties`:

```properties
transcriptionBaseUrl=https://your-server.example
```

Alternatively pass `-PtranscriptionBaseUrl=https://your-server.example` to Gradle and rebuild. Without a configured URL, uploads fail cleanly with a localized configuration message and can be retried from `NoteDetailView`.

## Build and test

```sh
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

- **Verified on 2026-11-17:** `22` JVM unit tests (`testDebugUnitTest`) and `28` connected instrumented tests (`connectedDebugAndroidTest` on `Medium_Phone` Android 17 / API 37 emulator) pass with 0 failures and 0 skips.

## CS265 Documentation & Evidence

- [Feature Inventory (REQ-1.1)](docs/feature-inventory.md)
- [Test Cases & Results (REQ-1.2)](docs/test-cases.md)
- [Course & Cross-Course Mapping (REQ-1.3–1.4)](docs/course-mapping.md)
- [Architecture, Use Cases & Data Flows (REQ-2.1)](docs/architecture.md)
- [Technical Challenges & Fixes (REQ-2.2–2.4)](docs/technical-challenge.md)
- [End-to-End User Flow & Device Compatibility (REQ-3.*, REQ-5.1)](docs/user-flow.md)
- [Attribution & AI Disclosure (REQ-5.2)](docs/attribution.md)
- [Rubric Verification Checklist](reference/Checklist.md)
