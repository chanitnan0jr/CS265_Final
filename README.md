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

- Speech recognition uses the device's configured provider and may require network access. The app does not save audio recordings.
- Notes are stored as JSON in app-private SharedPreferences without application-level encryption. Android backup settings may copy app data according to the device's backup configuration. Evaluate these protections before storing sensitive notes.
- A storage read error blocks saving to avoid overwriting existing data. There is no in-app recovery or export yet.
- The home audio action opens a local file for playback, pause, and seeking. The selected file is not attached to a note or retained after the playback screen closes. Automatic transcription and word highlighting are not implemented yet.
- Background transcription and full folder management are pending. The current library source filters infer a source from the folder name and can misclassify notes; source metadata is not yet stored.
- Five instrumented storage and recording-screen callback tests passed on an Android emulator. Permission denial and granting were checked interactively. An injected [Thai speech test sample](app/src/androidTest/assets/README.md) was transcribed correctly apart from spacing, saved, found by title, and remained visible after restarting the app. One earlier attempt failed; the provider reported a missing offline Thai language pack. Folder and calendar navigation and broader speech conditions still need end-to-end testing.
