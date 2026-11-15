# Voice2Note

Voice2Note is a Thai-first Android voice journal built with Kotlin and Jetpack Compose. The current prototype transcribes speech using the device's Android speech service, stores notes locally, lets users rename saved notes, and uploads audio files for background transcription by a configured server.

## Project structure

```text
app/src/main/java/com/example/cs265final/
├── MainActivity.kt       # App navigation, permissions, speech, uploads, and note actions
├── data/                 # Note model, local storage, filtering, and file helpers
├── feature/
│   ├── home/             # Home screen
│   ├── recording/        # Recording and note composer screen
│   ├── playback/         # Local audio playback
│   ├── notes/            # Library and note cards
│   └── calendar/         # Notes by date
├── speech/               # Live transcription draft handling
├── transcription/        # Upload API client and background worker
└── ui/theme/             # Compose theme and visual tokens
```

The [Voice2Note Pack](Voice2Note_Pack/README.md) contains the roadmap and design direction.

## Background transcription

Phase 2 uploads MP3, M4A, and WAV files, sends them to the transcription API described in the [roadmap](Voice2Note_Pack/Roadmap-Plan.md), and polls until the job finishes. The server root is read from the untracked `local.properties` so it stays machine-specific:

```properties
transcriptionBaseUrl=https://your-server.example
```

Alternatively pass `-PtranscriptionBaseUrl=https://your-server.example` to Gradle. Rebuild the app after changing the URL. Without a URL, uploads fail with a configuration message and can be retried after the app is rebuilt. Use HTTPS: Android blocks cleartext traffic by default.

Uploaded audio leaves the device and is sent to that server. Attaching audio from a note detail screen only adds the playback file and does not upload it. WorkManager continues the job when the user leaves the screen; note cards and the note detail screen show queued, processing, failed, and completed states, and failed jobs can be retried from the detail screen. For a note that already contains text, the transcript is appended rather than overwriting the text.

## Build and test

Open the project in Android Studio and sync Gradle. The project uses JDK 25; on this development machine, Android Studio's bundled JDK can be used:

```sh
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

The connected tests need an Android device or emulator. Testing speech also requires a microphone and an installed Thai-capable recognition service.

## Current limitations

- Speech recognition uses the device's configured provider and may require network access. The current recording path attempts to save captured audio on Android 13 and newer, but recognition services may not support the supplied audio source. Recorded audio is not available when capture cannot start, including on older Android versions. This path still needs device testing.
- Notes are stored as JSON in app-private SharedPreferences without application-level encryption. Android backup settings may copy app data according to the device's backup configuration. Evaluate these protections before storing sensitive notes.
- A storage read error blocks saving to avoid overwriting existing data. There is no in-app recovery or export yet. Unsaved audio is held in memory and does not survive process termination; save the note before closing the app.
- Uploaded audio is sent to the configured transcription server; there is no offline or on-device file transcription. An upload is saved only when the document provider grants persistent read access. The app polls a job for at most five minutes, ignores the API's word-level `segments` (reserved for Phase 4), and a retry re-uploads the file. WorkManager's background limits apply. If the server is unreachable after repeated attempts, the note is marked failed and can be retried.
- Automatic transcription and accurate word highlighting are not implemented for locally recorded notes. Background folder management is pending. Audio source filters use stored metadata; for older notes without metadata, `file:` audio is treated as recorded and `content:` audio as uploaded. Notes without an identifiable source remain unclassified.
- Eleven instrumented tests pass on an Android emulator, including note storage migration, worker/UI storage reconciliation, audio playback, and draft recreation. With an injected [Thai speech sample](app/src/androidTest/assets/README.md), the app transcribed the sample correctly apart from spacing, saved recorded audio, played it, filtered the note by date and folder, renamed and searched it, and reopened it after restart. Permission denial and settings guidance were also checked. The background upload worker itself has not been run against a real transcription server; these checks cover one emulator and one short sample, and other recognition services, audio inputs, longer recordings, and server behavior still need testing.
