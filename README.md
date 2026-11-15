# Voice2Note

Voice2Note is a Thai-first Android voice journal built with Kotlin and Jetpack Compose. The current prototype transcribes speech using the device's Android speech service, stores notes locally, and lets users rename saved notes from the note detail screen.

## Project structure

```text
app/src/main/java/com/example/cs265final/
├── MainActivity.kt       # App navigation, permissions, and speech integration
├── data/                 # Note model, local storage, and filtering
├── feature/
│   ├── home/             # Home screen
│   ├── recording/        # Recording and note composer screen
│   ├── playback/         # Local audio playback
│   ├── notes/            # Library and note cards
│   └── calendar/         # Notes by date
└── ui/theme/             # Compose theme and visual tokens
```

The [Voice2Note Pack](Voice2Note_Pack/README.md) contains the roadmap and design direction.

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
- The home audio action opens a local file for playback and creates a note referencing the selected file. The app saves a reference only when the document provider grants persistent access; otherwise playback is temporary and a warning is shown. Automatic transcription and accurate word highlighting are not implemented yet.
- Background transcription and full folder management are pending. Audio source filters use stored metadata; for older notes without metadata, `file:` audio is treated as recorded and `content:` audio as uploaded. Notes without an identifiable source remain unclassified.
- Eight instrumented tests passed on an Android emulator, including note storage migration, audio playback, and draft recreation. With an injected [Thai speech sample](app/src/androidTest/assets/README.md), the current app transcribed the sample correctly apart from spacing, saved recorded audio, played it, filtered the note by date and folder, renamed and searched it, and reopened it after restart. Permission denial and settings guidance were also checked. These checks cover one emulator and one short sample; other recognition services, audio inputs, and longer recordings still need testing.
