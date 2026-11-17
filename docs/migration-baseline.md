# Voice2Note migration baseline

This is a source audit for the requested Java/XML migration, not a claim that
the application or any rubric item has passed acceptance. The tracked source
was inspected at commit `0ac59b78967f374252686307c72e5c2b7b24576e` on `main`.
`AGENTS.md` and `reference/` were untracked at the time of the audit and are
not included in that commit. Preserve these files when freezing the baseline.

## Observed application

The current app uses Kotlin and Compose. `MainActivity.kt` coordinates screen
selection, permission and speech recognition, audio capture, note operations,
and WorkManager jobs. Compose screens live under `feature/`; note storage and
filters live under `data/`, and upload/polling code lives under `transcription/`.
The manifest declares recording, media and network permissions and exposes the
launcher activity. Speech uses the device-configured recognition service.

The intended user flow visible in source is: start a recording, review or edit
the transcript, save a note, then browse, search, select, play or delete it.
There are calendar and library views, and an imported audio file can be sent to
a configured server through WorkManager. Attaching audio in note detail is for
playback, whereas the home upload starts transcription. The README describes
prior emulator checks, but these are not evidence of a current migration build.

## Persistent data and compatibility

`NoteStorage` stores a JSON array in app-private SharedPreferences named `notes`,
key `items`. Each record requires `title`, `body`, `mood`, `folder`, and `time`;
optional fields are `audioUri`, `audioSource`, `jobId`, `transcription`, and
`transcriptionError`. The URI and source distinguish recorded and imported
audio; for older records missing `audioSource`, `file:` maps to `RECORDED`,
`content:` to `UPLOADED`, and other values to `UNKNOWN`. Status and source are
stored as enum names. There is no stable note ID in the current schema.

UI saves reconcile pending transcription with stored worker results by job ID.
An in-process lock serializes storage operations, and the write returns the
SharedPreferences commit result. A failed read must block a save rather than
being interpreted as an empty library. Migration must preserve these behaviors,
existing URIs and permissions, and worker identity/status. Audio files must not
be removed until equivalent behavior has been verified.

## Known gaps and risks

- Folder names are labels on notes, with defaults and names derived from
  existing notes; there is no independent persistent empty-folder collection.
  Search by title/body and folder filtering exist, but their combined UX needs
  verification and completion.
- Card and detail delete menus invoke deletion directly; confirmation is
  required at each entry point. Back and unsaved-draft handling need device
  verification; playback has its own Back handler.
- The UI contains hardcoded Thai strings; English resource coverage and
  light/dark, configuration and screen-size behavior require implementation
  and device evidence.
- Speech is stopped in `onStop`; recorded audio may be unavailable on older
  Android releases or with recognition services that reject the capture source.
  Unsaved audio is held in memory and does not survive process termination.
- Upload requires a configured server and persistable access to the selected
  content URI. Audio sent for transcription leaves the device. The README says
  the worker has not been tested against a real server.

## Test and rubric evidence

Existing source includes JVM tests for note filtering, speech drafts, WAV files,
and transcription, plus instrumented storage, recording, playback and draft
tests. On 2026-11-17, `:app:assembleDebug :app:testDebugUnitTest` succeeded
with Gradle 9.5.0 and Android Studio JDK 25.0.2 using an offline copy of the
Gradle cache. The unit-test task was up-to-date, so this run did not execute
the tests anew. The wrapper initially failed on a read-only Gradle home; a
temporary writable cache and local socket access allowed the build to run.
No device was attached when checked; connected tests and device journeys have
not been run in this session.
An isolated Android 37 emulator was attempted three times, including software
CPU and graphics; each attempt terminated with SIGSEGV before becoming usable.
The existing AVD data was not used. Connected tests therefore remain unrun.

After the first migration unit (`SpeechDraft`), a forced rerun of
`:app:testDebugUnitTest` passed 20 tests across five suites, with no failures,
errors or skips. `:app:assembleDebug` also succeeded. These results verify the
local JVM/build checks only; they do not establish speech-provider or device
behavior.

The assessment and tracker are `reference/requirement.md` and
`reference/Checklist.md`; the course index is
`reference/CS265_references/INDEX.md`. The 20 criteria remain unverified for
this migration until implementation, executed tests and report evidence are
all checked. Do not mark checklist entries complete based on this audit.

## Migration guardrails

Migrate one feature at a time with an explicit acceptance test and preserve the
existing implementation until equivalent behavior and data access are checked.
The Java/XML navigation, stable identifiers, storage approach and screens need
to be recorded as decisions before the relevant migrations. Preserve the
existing SharedPreferences schema or provide a tested migration; do not infer
that duplicate titles or timestamps identify a note. Verify permission denial,
read/write failures, deletion cancellation, lifecycle and worker races as well
as successful paths. No dependency or backend change is implied by this audit.
