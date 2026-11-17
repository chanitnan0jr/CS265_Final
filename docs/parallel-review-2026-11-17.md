# Parallel migration review — 2026-11-17

This is a read-only source review of the in-progress Java/XML migration. It is
not evidence that the listed paths have been exercised, and findings should be
rechecked against the latest source before changes because another agent may
also be working in this repository. No rubric item is marked complete here.

## Data and background work

1. `NoteRepository.renameFolder` and `deleteFolder` save changed notes before
   saving folder names. If the second commit fails, the method reports failure
   after the note reassignment has already been persisted. The UI can then
   retain an older snapshot. Both values use the same preferences file; a
   single serialized read/merge/commit for both keys, with failure and worker
   race tests, is a candidate fix.
2. `TranscriptionWorker.doWork` treats a storage JSON read failure and an
   unsuccessful transition to PROCESSING like a completed job. A genuine
   storage failure can leave a note pending without a worker retry. Verify
   missing/deleted jobs separately from storage errors before changing this.
3. Folder persistence currently uses display names as identity. Stable folder
   IDs and a tested migration remain necessary under `AGENTS.md`.

## Playback, recording and lifecycle

1. `NoteDetailView` and `PlaybackView` unregister their lifecycle observer on
   detachment. `MainActivity` binds each observer when constructing the view,
   while navigation later reuses those view objects. Review and test reopening
   each view and then backgrounding while playback is active.
2. The background recording path cancels recognition and discards the current
   session's buffered audio. Check draft text and audio together, inform the
   user when listening must restart, and test actual microphone interruption.
3. `MainActivity.saveDraftNote` copies and encodes recorded PCM and commits
   preferences on the main thread. Long recordings need a bounded, recoverable
   background save path. Playback position after recreation also needs testing.

## Evidence and test safety

- The saved connected-test report records 28 tests with no failures on the
  `Medium_Phone` Android 17 emulator. Source/resources changed after the
  recorded run, so this is not verification of the current tree.
- `MainActivityFlowsTest`, `CompatibilityAndFolderTest`, and
  `CalendarAndDetailTest` clear the app's `notes` preferences. Until their
  storage is isolated, run them only on a disposable emulator or test profile
  containing no important Voice2Note data. Restoring preferences after a test
  would not protect against interruption or other side effects.
- `evidence/tests/jvm-unit-tests-combined.xml` contains multiple concatenated
  XML documents and is not a single parseable XML document. Use individual
  Gradle test reports as evidence until the aggregation is corrected.
- Permission denial, provider unavailability, real speech recognition,
  WorkManager/server integration, duplicate legacy-note IDs, failed writes,
  and screen-by-screen Back/delete/lifecycle coverage still need evidence.

These findings are derived from source inspection and existing reports, not
newly executed device tests. Confirm each item and record actual results before
updating `reference/Checklist.md`.
