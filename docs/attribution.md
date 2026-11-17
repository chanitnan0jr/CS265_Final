# Voice2Note — Attribution & AI Disclosure (`REQ-5.2`, `G19`)

## 1. Team-Authored vs. Reused Components

| Component / Artifact | Category | Source / License | Role in Voice2Note | Team Contribution & Modifications |
|---|---|---|---|---|
| `MainActivity`, `NoteRepository`, `NoteStorage`, `FolderStorage`, `NoteFilters`, `SpeechCaptureController`, `SpeechDraft`, `HomeView`, `LibraryView`, `FolderDialogs`, `CalendarView`, `SearchControls`, `SearchView`, `SearchResultView`, `RecordingView`, `NoteDetailView`, `PlaybackView`, `DeleteNoteConfirmation`, `WaveformView`, `UiLabels`, `TranscriptionClient`, `TranscriptionWorker`, `AudioUtils`, XML layouts & resources | Application code & UI | Original project (`chanitnan0jr/CS265_Final`) | Core Voice2Note app logic, XML Views, Thai/English localization, Day/Night themes, persistence, and tests | Team product requirements, UX design scheme (`Voice2Note_Pack/`), baseline Kotlin prototype, and Java + XML migration verified via unit and instrumented tests |
| `androidx.activity:activity` (`1.13.0`), `androidx.lifecycle:lifecycle-runtime` (`2.11.0`), `androidx.work:work-runtime` (`2.10.5`) | Jetpack / AndroidX Libraries | Google / Android Open Source Project (Apache-2.0) | `ComponentActivity`, `OnBackPressedDispatcher`, `ViewModelProvider`, `Lifecycle`, and `WorkManager` background jobs | Integrated into `MainActivity`, `RecordingDraftViewModel`, `NoteDetailView`, `PlaybackView`, and `TranscriptionWorker` |
| `de.sciss:jump3r` (`1.0.5`) | Audio encoding library | Sciss / LAME port (LGPL) | Pure-Java MP3 frame encoding from captured 16 kHz mono PCM | Wrapped inside `AudioUtils.writeMp3File()` and verified with `WavFileTest` |
| `junit:junit` (`4.13.2`), `org.json:json` (`20250517`), `androidx.test.ext:junit` (`1.3.0`), `androidx.test.espresso:espresso-core` (`3.7.0`) | Testing libraries | JUnit (EPL-1.0), JSON.org, AndroidX Test (Apache-2.0) | JVM unit tests and Android instrumented UI/storage tests | Authored all 22 JVM unit tests and 29 instrumented tests under `app/src/test` and `app/src/androidTest` |
| Android Platform APIs (`SpeechRecognizer`, `AudioRecord`, `MediaPlayer`, `MediaCodec`, `MediaExtractor`, `SharedPreferences`, `HttpURLConnection`) | Android SDK | AOSP (Apache-2.0) | Live STT (`th-TH`), PCM mic tee pipe, local playback, HTTP upload/polling | Configured and wrapped with lifecycle pause/resume, error handling, and resource cleanup |
| `app/src/androidTest/assets/thai-speech-sample.{mp3,wav}` | Test media fixture | Project repository (`app/src/androidTest/assets/README.md`) | Deterministic Thai speech audio fixture for playback tests | Used in `PlaybackViewTest` |
| Built-in Android drawable icons (`@android:drawable/ic_btn_speak_now`, `ic_media_pause`) & system fonts | Platform assets | Android SDK | Microphone & pause icons on Recording/Home buttons | Styled via `drawableTint` / `tint` with semantic theme colors |

## 2. AI Assistance Disclosure (`REQ-5.2`)

- **AI coding assistants** were used to accelerate parts of the Java + XML Views migration, test migration, layout debugging (`LibraryView` `Space` fix and `SearchControls` inflation constructor), and CS265 documentation drafting.
- **Verification ownership:** All architectural decisions (`UI -> Repository -> Data source`), scope control (`AGENTS.md`), and verification evidence (`./gradlew :app:testDebugUnitTest`, `./gradlew :app:connectedDebugAndroidTest`, emulator compatibility checks, and `evidence/` screenshots) were executed and verified directly against the built APK in this workspace.
- **No fabricated claims:** No AI-generated report metrics or untested behaviors are claimed; all test numbers (`22/22` unit tests, `29/29` instrumented tests) come from actual command executions on 2026-11-17.
