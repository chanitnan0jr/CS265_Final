# AGENTS.md — Voice2Note / CS265 Final Project

> Repository: `chanitnan0jr/CS265_Final` | Updated: 2026-11-17
>
> **Goal:** Build an Android voice-note app that every teammate can explain,
> test, demonstrate, and relate to the **20 CS265 grading criteria**.
> Prefer readable code and verified behavior over clever abstractions.

## 1. Ground truth and read-first order

- **Verified baseline (`main`, 2026-11-17):** Kotlin + Jetpack Compose;
  `MainActivity.kt` handles navigation/speech/note actions. Notes use
  `SharedPreferences` + JSON (`data/NoteStorage.kt`). Basic title/body search
  already exists in `data/NoteFilters.kt`. A WorkManager transcription path,
  audio playback, calendar, and partial folder filtering also exist.
  **Existing code is not proof that a feature passes the rubric.**
- **Planned rewrite:** Java + XML Views is being considered to help teammates
  understand the implementation and align with classroom examples. This is
  **a team choice, not a CS265 requirement**. Do not start a wholesale rewrite
  unless the task explicitly asks for it or the team decision is recorded.
- Read: (1) task + affected source; (2) `requirement.md` / `Checklist.md` if
  present; (3) `CS265_references/INDEX.md` and **only relevant BMxx files** if
  present; (4) `README.md`, relevant tests, and `Voice2Note_Pack/` as needed.
- If a referenced document is absent, say so. Never fabricate slide content,
  test results, report evidence, or completed checklist entries. Designs and
  roadmaps describe intent; **current source + executed tests describe reality**.

## 2. Non-negotiable agent behavior

1. Before implementation, identify **current behavior, desired behavior,
   affected files, data impact, and rubric criterion**.
2. Make **small vertical changes**: UI action -> validation -> logic ->
   repository/service -> visible result -> test. Do not rewrite all features in
   one AI request or create unrelated refactors.
3. Make code **teachable**. Every feature owner must explain its inputs,
   output, error path, classes and data flow without AI. Do not hide logic in
   unexplained utilities, magic constants, or oversized callbacks.
4. Preserve saved notes, existing recordings, audio URIs, worker status and
   permission behavior. Test migrations; never treat storage read failure as
   an empty library and overwrite user data.
5. Do not add libraries, Gradle modules, a new backend, DI framework or database
   merely for appearance. Provide a reason and team approval for dependencies.
6. Do not commit or expose `.env`, `local.properties`, API tokens, user audio,
   transcripts or credentials. Do not log private note contents. Explain when
   uploaded audio leaves the device.
7. Separate **COURSE** (slide), **PROJECT** (observed source), **DECISION**
   (team convention), and **EVIDENCE** (executed test). Never claim unrun tests
   passed or third-party/AI-authored code was written entirely by the team.

## 3. Architecture rules

Use **UI -> Repository -> Data source**; add a domain/use-case layer only when
logic is genuinely complex or reused. An Activity/Fragment handles view events,
rendering and navigation—not database serialization, SQL, HTTP or audio loops.

**Suggested target structure for Java/XML migration** (not current layout):

```text
app/src/main/java/com/example/cs265final/
  MainActivity.java                    # entry/navigation only
  feature/{home,recording,notes,folders,search,calendar,playback}/
  data/model/                         # Note, Folder, status models
  data/repository/                    # shared read/write entry points
  data/local/                         # SQLite or compatible JSON storage
  speech/                             # SpeechRecognizer integration
  transcription/                      # API client and WorkManager
app/src/main/res/
  layout/  values/  values-en/  values-night/
  layout-sw600dp/                     # only if large-screen variant needed
```

- The above are **packages**, not separate Gradle modules. Choose one
  consistent navigation approach (Activities or Fragments) and document it.
- Use a stable `noteId` / `folderId`; do not use title, timestamp or object
  identity as a primary key. Preserve old JSON notes through a tested migration.
- **Folder vs Playlist:** folder groups notes; a playlist orders audio for
  playback. Clarify which one the feature implements before changing data.
- Search must reuse/extend the existing title/body query, then support the
  agreed scope (whole library / folder) without introducing embeddings/RAG.
- Repository owns persistence; UI receives models and sends actions. Use
  `ViewModel` or a lightweight state holder for UI state when helpful. A
  `ViewModel` survives configuration changes but **not process death**; save
  critical drafts using saved state and/or persistent storage.
- SQLite/Room are **optional choices**, not rubric obligations. If moving from
  JSON to SQLite, add migration and regression tests for duplicate titles,
  missing fields, failed writes and old saved notes.

## 4. Java, XML and resource conventions

**Java baseline: Google Java Style Guide.**

- UTF-8; **2-space indentation**, no tabs; K&R braces; target 100 columns;
  one top-level class per `.java`; no wildcard imports; use `@Override`.
- Classes: `UpperCamelCase` (`NoteRepository`); methods/fields: `lowerCamelCase`
  (`saveNote`, `noteId`); constants: `UPPER_SNAKE_CASE`; packages: lowercase.
- Minimize visibility, prefer cohesive small functions, check input/error cases,
  never silently swallow exceptions. Comments/Javadoc explain **why** or
  constraints, not obvious code. Only use language/API features supported by
  the configured Android toolchain.
- Resources: `lower_snake_case` file names (`activity_main.xml`,
  `item_note.xml`, `dialog_delete_note.xml`); descriptive IDs
  (`button_save_note`) and string keys (`notes_delete_confirm`).
- **No hardcoded UI strings** in screens, dialogs, Toasts or errors. Provide a
  complete default `res/values/strings.xml` (Thai-first) and `values-en/`;
  use `R.string.*`, formatted strings and appropriate plurals. Store stable
  enum/status IDs, **not localized display labels**, in persistent data.
- Centralize colors/dimensions/themes. Support legible Day/Night, device
  font-scale changes, and accessible labels for icon-only controls. Use `dp`
  for layout, `sp` for text, scroll when necessary; never pin entire layouts
  to physical pixel dimensions.
- Existing Kotlin/Compose code follows its established style **until it is
  explicitly migrated**. Gradle `.kts` files do not need conversion to Groovy
  when rewriting application code into Java.

## 5. Android-specific safety and lifecycle

- Handle microphone permission denial, unavailable recognition provider,
  callbacks/errors, interruptions, and audio file failures. Release
  `SpeechRecognizer`, `AudioRecord`, and `MediaPlayer`/`MediaRecorder` properly.
- Current live STT uses the **device-configured Android speech service**, not a
  verified fixed Google model; do not label it otherwise.
- Pause/stop live recording and audio playback when the app backgrounds;
  restore text, position or progress on return as appropriate. If a listening
  session must restart, preserve the draft and clearly inform the user.
  Do not enable secret/continuous background microphone capture.
- **WorkManager upload/transcription** may continue in background by design;
  document/test this independently from the rubric's pause/resume example.
- **Every delete action** (card, detail, folder) requires confirmation.
  Cancel keeps data; Confirm persists deletion and updates UI; report failures.
- System Back must follow the actual navigation flow; close modal/editor first,
  preserve unsaved changes, and protect against accidental app exit with
  double-back or a confirmation dialog according to the rubric.
- Never block the main thread with network calls, heavy encoding or file I/O.
  Timeouts, offline conditions and failed background tasks need recoverable UI.

## 6. Rewrite and scope control

When the team explicitly requests the Java/XML rewrite:

1. **Freeze a baseline** in version control and record current build/tests,
   working user journeys, storage schema and known limitations.
2. Agree on navigation, models, storage strategy and UI screens. Draw the
   real event/data flow; do not generate an architecture that nobody can teach.
3. Migrate **one feature at a time**, with acceptance criteria, tests and an
   explanation of each involved Java class/XML screen.
4. Verify existing data, audio handling and transcription state still work.
   Remove the old implementation only after equivalence checks pass.
5. Finish rubric gaps before optional improvements: folder management,
   search UX, localization, Day/Night, responsive layout, Back/Delete and
   lifecycle behavior.

**Release core flow:** Thai speech -> edit transcript -> save Note -> reopen ->
search -> organize -> delete safely. Keep calendar/playback/upload if already
working; postpone semantic search, karaoke highlighting, AI mood intelligence,
cloud sync and new backend services unless separately approved.

## 7. Testing and report evidence

Current repository README documents:

```bash
# Run from repository root with its Gradle wrapper
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest    # needs emulator/device
```

- README currently describes Android Studio JDK 25 for its development setup;
  verify the actual machine/toolchain instead of assuming every environment
  matches. Record failed/skipped checks accurately.
- Each essential function: positive, negative and boundary cases where
  applicable, with **steps, input, expected, actual, pass/fail, device and
  evidence file**. Add unit tests for pure logic and device tests for UI,
  permissions, lifecycle, storage and audio.
- Rubric `REQ-3.1–3.4`: test **two screen configurations**, portrait/landscape
  or a documented lock, **Thai/English**, and **Day/Night**; controls must not
  overlap or be clipped.
- Rubric `REQ-4.1–4.4`: check Back on every relevant screen, exit guard,
  confirm/cancel at **every deletion entry**, and background -> foreground
  pause/resume of active work.
- Rubric `REQ-1.1–1.4`: list features and screens, test all essential features,
  link CS265 lessons and a second course where genuinely applicable.
- Rubric `REQ-2.1–2.4`: explain input -> processing -> output; retain real
  **problem -> cause -> fix -> before/after evidence** for a technical challenge.
- Rubric `REQ-5.1–5.4`: provide at least one annotated end-to-end screen flow,
  disclose code/assets/AI sources, present challenge evidence, keep report clear.
- **Only mark `Checklist.md` as complete after implementation + test + report
  evidence are verified.** Cite actual BM slide/page numbers; never invent them.
  Relevant lessons: BM02 lifecycle; BM03 layout; BM04 listeners/Intents;
  BM05 resources; BM06 runtime changes; BM07 input; BM08 storage/RecyclerView;
  BM09 use cases/tests/report.

## 8. Collaboration and definition of done

- Use small commits/PRs: `feat(notes): add folder picker`,
  `fix(recording): preserve draft`, `test(search): cover empty query`.
- In every handoff explain: **what/why; touched classes; user action -> data
  flow -> result; error paths; tests actually run; rubric link; data impact**.
- A feature is done only if teammates can explain it, its relevant tests ran
  (or blockers are recorded), failure/cancel paths are safe, existing saved
  data remains compatible, and grading evidence is accounted for.
- In the report, distinguish original team code from adapted examples,
  dependencies, generated assets and AI-assisted implementation.

## Sources (external guidance, not additional course requirements)

- AGENTS.md format: https://agents.md/
- Google Java Style Guide: https://google.github.io/styleguide/javaguide.html
- Android architecture: https://developer.android.com/topic/architecture
- Android Views architecture: https://developer.android.com/topic/architecture/views/recommendations-views
- Android saved UI state: https://developer.android.com/topic/libraries/architecture/views/saving-states-views
- Android localization: https://developer.android.com/guide/topics/resources/localization
- WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Android testing: https://developer.android.com/training/testing/fundamentals/strategies
- Codebase: https://github.com/chanitnan0jr/CS265_Final
