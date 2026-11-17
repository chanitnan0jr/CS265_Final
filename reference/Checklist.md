# CS265 — Project Submission Checklist

**Project:** Voice2Note (working name)  
**Course:** CS265 — Basic Mobile Development, Academic Year 2569  
**Assessment source:** `CS265_TermProject_Rubric1-69.xlsx` (5 categories × 4 criteria = 20 criteria)  
**Canonical requirements:** `requirement.md`  
**Course references:** `CS265_references/INDEX.md` → `BM01.md`–`BM09.md`

> **Purpose:** This is an operational *verification and evidence tracker*, not a replacement for `requirement.md`. Leave a criterion unchecked until both the implementation and the required evidence have been verified against the actual app/build. Anything not yet inspected is **unverified**, not necessarily unimplemented.
>
> **Scope caveat:** Playlist/Folder and Search are known pending items from team discussion, but the rubric itself does **not** mandate those exact features. They become required for REQ-1.1/1.2 only if included in the agreed project scope.

## Status dashboard

- **Verified rubric criteria:** `20 / 20` (codebase, unit/instrumented tests, compatibility captures, challenge evidence, consolidated report `docs/final-report.md`, and 10-slide presentation deck verified on 2026-11-18)
- **Category 1 — Functional completeness/report evidence:** `4 / 4` verified
- **Category 2 — Technical capability/challenge:** `4 / 4` verified
- **Category 3 — Device compatibility:** `4 / 4` verified
- **Category 4 — Navigation/delete/lifecycle:** `4 / 4` verified
- **Category 5 — Report/presentation:** `4 / 4` verified (`REQ-5.1`–`REQ-5.4` verified across report, slide deck, and cross-document audit)

**Rule for updating counts:** Count only checked `REQ-*` items backed by a traceable evidence path or report page. A blank checkbox means *not verified*. Do not infer the instructor's score for 0 passed subcriteria: the provided rubric specifies scores for 1–4 items per category only.

## 1 — Functional completeness and report evidence

- [x] **REQ-1.1 — Function inventory:** List *every in-scope essential feature*, its screen and observable result. Compare list with running build. **Evidence:** `docs/feature-inventory.md` + `evidence/screens/`. **Related:** BM09 pp. 2–6.
- [x] **REQ-1.2 — Tests for all essential functions:** For every in-scope feature, record test steps, expected result, actual result, pass/fail and proof that it works. **Evidence:** `docs/test-cases.md` and `evidence/tests/` (`22` JVM unit tests + `29` instrumented tests passing). **Related:** BM09 pp. 7–8.
- [x] **REQ-1.3 — CS265 connection:** Map techniques actually implemented to BM topic, slide page, source module and reason used. Do not claim a technique merely because it appears in slides. **Evidence:** `docs/course-mapping.md`. **Related:** BM01–BM09 via `INDEX.md`.
- [x] **REQ-1.4 — Other course connection:** Explain at least one actual application of knowledge from another course, with concrete code/design evidence. **Evidence:** `docs/course-mapping.md` (DSP / Audio Signal Processing & HTTP/Multipart networking examples).

## 2 — Technical capability and challenge

- [x] **REQ-2.1 — Main function mechanism:** Explain at least one complete **Input → Processing → Output** flow, supported by source code or diagram. **Evidence:** `docs/architecture.md` + diagram/code locations. **Related:** BM04 pp. 2–16; BM08 pp. 3–17.
- [x] **REQ-2.2 — Real technical challenge:** Identify at least one *actually observed* bug/challenge, affected feature, symptoms and root cause. **Evidence:** `evidence/challenge/before-spacer-bug.txt`, `evidence/challenge/before-xml-inflation.txt` + reproduction notes in `docs/technical-challenge.md`. **Related:** relevant BM module.
- [x] **REQ-2.3 — Actual fix:** Document what changed, why the change addresses the cause and exact class/file/function/commit. **Evidence:** `docs/technical-challenge.md` (`LibraryView`, `SearchControls`, `themes.xml`).
- [x] **REQ-2.4 — After-fix result:** Show actual post-fix test/screenshot/log, contrasted with original problem or expected result. **Evidence:** `evidence/challenge/after-connected-tests.xml` + `docs/technical-challenge.md`.

## 3 — Compatibility across mobile devices

- [x] **REQ-3.1 — Two device configurations:** Test a minimum of **two screen configurations with different size and resolution**; main UI text/buttons must not clip or overlap and must remain usable. **Evidence:** `evidence/device-compatibility/compat-screen-phone-1080x2400.png`, `compat-screen-tablet-1600x2560.png` + `CompatibilityAndFolderTest`. **Related:** BM03 p. 17, BM05 pp. 18–36.
- [x] **REQ-3.2 — Orientation:** Demonstrate working portrait + landscape **or explicitly demonstrate a single locked orientation** (both are permitted by the rubric). **Evidence:** `evidence/device-compatibility/compat-orientation-landscape.png` + `CompatibilityAndFolderTest`. **Related:** BM05 pp. 20–22, BM06 pp. 2–6.
- [x] **REQ-3.3 — Thai + English:** Switch language and verify app-created menus, buttons and notifications/messages all follow the selected language. **Evidence:** `evidence/device-compatibility/compat-lang-th.png`, `compat-lang-en.png` + `CompatibilityAndFolderTest`. **Related:** BM05 pp. 10, 18–26, BM06 pp. 3, 7–8.
- [x] **REQ-3.4 — Day + Night:** Switch theme and verify main screen changes; both text and controls remain distinguishable and readable. **Evidence:** `evidence/device-compatibility/compat-theme-light.png`, `compat-theme-dark.png` + `CompatibilityAndFolderTest`. **Related:** BM02 p. 31, BM05 pp. 11, 22; BM06 pp. 4–5.

## 4 — Back navigation, delete confirmation and activity lifecycle

- [x] **REQ-4.1 — Consistent Back paths:** From *each main content page reachable from a menu*, Back returns to its expected previous/defined destination (no unrelated jumps). **Evidence:** `docs/architecture.md` + `MainActivityFlowsTest`. **Related:** BM02 pp. 23–28.
- [x] **REQ-4.2 — Prevent accidental exit:** When Back would exit the app from content, require a confirmation dialog or double-back prompt; verify that **Cancel** leaves work intact. **Evidence:** `MainActivityFlowsTest` + `MainActivity.java`. **Related:** BM02 pp. 27–28.
- [x] **REQ-4.3 — Confirm every delete:** All deletion actions must ask first; test both **Cancel** (data preserved) and **Confirm** (data deleted). Include notes and folders/playlists *if those delete actions exist*. **Evidence:** `evidence/screens/08-delete-confirm-dialog.png`, `DeleteNoteConfirmationTest`, `CompatibilityAndFolderTest`. **Related:** BM08 pp. 9–10, 18–19.
- [x] **REQ-4.4 — Pause/background and continue/return:** While an important task is running, switch to another app and verify work pauses; return and demonstrate that the prior state can continue. For recording, measure/document actual microphone/audio behavior. **Evidence:** `NoteDetailLifecycleTest`, `RecordingDraftTest`, `PlaybackViewTest`, `docs/feature-inventory.md`. **Related:** BM02 pp. 18–22, 27–28. **Not the same as:** configuration recreation in BM06.

## 5 — Report and presentation

- [x] **REQ-5.1 — One complete user journey:** Include screenshots ordered **start → actions/taps → finished result**; label each transition and ensure the reported result matches the build. **Evidence:** `docs/user-flow.md`, `evidence/screens/01-home-thai.png`–`08-delete-confirm-dialog.png`. **Related:** BM09 pp. 4–6, 9.
- [x] **REQ-5.2 — Attribution and AI disclosure:** List external code, libraries, media and AI assistance; specify what was reused vs developed by the team. If none used, state that accurately. **Evidence:** `docs/attribution.md`. **Related:** rubric text (not a claim that BM slides require a particular tool).
- [x] **REQ-5.3 — Present one real challenge:** The report/slides/demo show **Problem → Cause → Fix → Measured/observed result**, with evidence from REQ-2.2–2.4. **Evidence:** `docs/technical-challenge.md`, `evidence/challenge/`.
- [x] **REQ-5.4 — Coherent report/presentation:** Read through for logical order, accurate terminology and no circular repetition; verify diagrams, captions and results agree. **Evidence:** `docs/final-report.md` (Section 8 coherence audit), `evidence/presentation/Voice2Note-Slides.html`, and `evidence/presentation/presentation-outline.md`.

## Feature readiness — team execution tracker (project-specific; not extra CS265 scoring items)

> Use this as a planning aid. Confirm final MVP scope and technology choice with the team; neither Kotlin nor Java is explicitly required by the provided grading rubric.

| Feature / task | Code status | Verification needed | Responsible | Notes |
|---|---|---|---|---|
| Record audio / Speech-to-Text | Implemented (Java/XML) | Live hardware mic & device speech service verification on physical device | Team | Draft editing, `RecordingViewTest`, `SpeechDraftTest`, `RecordingDraftTest` verified; hardware mic STT awaits team physical-device run |
| Create / view / edit / delete note | Implemented (Java/XML) | Verified | Team | `NoteStorageTest`, `DeleteNoteConfirmationTest`, `CalendarAndDetailTest`, `MainActivityFlowsTest` |
| Playlist / folder organization | Implemented (Java/XML) | Verified | Team | Folder create/rename/delete & note move via `LibraryView` + `FolderStorage` + `NoteRepository`; distinct from single-file `PlaybackView` (`CompatibilityAndFolderTest`) |
| Search notes | Implemented (Java/XML) | Verified | Team | Title/body search + folder-scoped filter + empty states (`NoteFiltersTest`, `SearchControlsTest`, `SearchResultViewTest`) |
| Thai/English + Day/Night | Implemented (Java/XML) | Verified | Team | `res/values/strings.xml`, `res/values-en/strings.xml`, `res/values-night/` (`CompatibilityAndFolderTest` + `evidence/device-compatibility/`) |
| Navigation & state restoration | Implemented (Java/XML) | Verified | Team | Back stack, exit confirmation, unsaved-draft prompt, `onSaveInstanceState`, `ON_STOP` cleanup (`MainActivityFlowsTest`, `NoteDetailLifecycleTest`) |
| Architecture modularization | Implemented (Java/XML) | Verified | Team | Separated into `data/`, `speech/`, `transcription/`, `ui/`, `feature/{home,notes,search,calendar,recording,playback}/` |
| Kotlin → Java migration | **Completed (0 `.kt` files)** | Verified (`22` JVM + `29` Android tests pass) | Team | All Kotlin/Compose sources, tests, and Gradle plugins/dependencies removed and replaced with Java + Android XML Views |

## Evidence index to fill in before submission

Use a short path or exact page reference instead of pasting every screenshot into this checklist.

| ID | Test/report location | Evidence path or report page | Verified by / date |
|---|---|---|---|
| REQ-1.1–1.4 | `docs/feature-inventory.md`, `docs/test-cases.md`, `docs/course-mapping.md` | `evidence/tests/`, `evidence/screens/` | Verified 2026-11-17 (`--rerun-tasks`) |
| REQ-2.1–2.4 | `docs/architecture.md`, `docs/technical-challenge.md` | `evidence/challenge/before-spacer-bug.txt`, `before-xml-inflation.txt`, `after-connected-tests.xml` | Verified 2026-11-17 |
| REQ-3.1–3.4 | `CompatibilityAndFolderTest`, `docs/test-cases.md` | `evidence/device-compatibility/compat-*.png` (7 captures) | Verified 2026-11-17 |
| REQ-4.1–4.4 | `MainActivityFlowsTest`, `DeleteNoteConfirmationTest`, `NoteDetailLifecycleTest` | `evidence/screens/08-delete-confirm-dialog.png`, `evidence/tests/connected-android-tests.xml` | Verified 2026-11-17 |
| REQ-5.1–5.4 | `docs/final-report.md`, `docs/user-flow.md`, `docs/attribution.md`, `docs/technical-challenge.md` | `evidence/screens/01`–`08`, `evidence/presentation/Voice2Note-Slides.html`, `presentation-outline.md` | Verified 2026-11-18 |

Suggested evidence directories: `evidence/screens/`, `evidence/tests/`, `evidence/device-compatibility/`, `evidence/challenge/`, `evidence/presentation/`.

### Suggested test-record format

`TC ID | REQ ID | Feature | Device/API | Preconditions / Test data | Steps | Expected | Actual | Pass/Fail | Evidence path`

Include positive, negative and boundary/validation cases where applicable (BM09 pp. 7–8). Never fill **Actual** or **Pass** without running the test.

## Release gate

- [x] Confirm in-scope essential features and team language/architecture decision (Java + Android XML Views).
- [x] Build/install a release candidate and run all essential feature tests (`22` unit + `29` instrumented tests on `Medium_Phone` API 37).
- [x] Have device/language/theme/navigation/lifecycle/delete evidence for REQ-3.x and REQ-4.x.
- [x] Capture **before and after** evidence for at least one genuine technical challenge.
- [x] Write source/AI attribution and confirm external asset licenses as applicable (`docs/attribution.md`).
- [x] Complete 20/20 criterion verification and check all evidence links/report pages (`20/20` verified across codebase, tests, `docs/final-report.md`, and `evidence/presentation/Voice2Note-Slides.html`).
- [ ] Review final user journey, report and live physical-mic demo rehearsal with the team on the Samsung build (no external transcription server deployed).

**Important source note:** The supplied rubric workbook has a few example cells already containing `1` and page numbers (e.g., 2.1, 2.2, 2.3, 3.3, 5.1). Those are not verified evidence of this project; this checklist deliberately initializes all criteria as unchecked.
