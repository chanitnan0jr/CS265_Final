# Voice2Note — Real Technical Challenges (`REQ-2.2–2.4`, `REQ-5.3`, `G16`)

This document records two genuine bugs encountered and fixed during the Kotlin/Compose → Java/XML migration, with observed before/after evidence.

---

## Challenge 1 (Primary): Plain `View` Spacer Expanded to Viewport Height in Odd-Sized Folder Grids

### 1. Feature & Symptom (`REQ-2.2`)
- **Affected Feature:** `LibraryView` (`app/src/main/java/com/example/cs265final/feature/notes/LibraryView.java`)
- **Observed Symptom:** When the folder list had an odd number of folders (e.g., 1 custom folder `"Podcasts"` or 5 folders after creating `"Exams"`), all notes in `library_notes_container` below the folder grid became invisible (`height = 0px`). `CompatibilityAndFolderTest.folderRenameAndDeleteConfirmationPreserveOrMoveNotes` failed with Espresso reporting `containerH=0 measH=0 rowH=0 tvH=0 tvW=784 libH=2038`.
- **Before Evidence:** `evidence/challenge/before-spacer-bug.txt`

### 2. Root Cause (`REQ-2.2`)
In `LibraryView.renderFolders()`, rows with an odd trailing folder appended a `new View(getContext())` placeholder with `LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)` to balance the 2-column horizontal `LinearLayout`.
- `LibraryView` sits inside a `ScrollView` with `android:fillViewport="true"`.
- Under Android's `View.onMeasure()` -> `View.getDefaultSize(0, heightMeasureSpec)`, a plain `View` measured with `MeasureSpec.AT_MOST` returns **`specSize` (the full remaining viewport height, `2038px`)** instead of `0`!
- Consequently, the odd folder row expanded to `2038px`, pushing `library_notes_container` completely out of the remaining layout space (`measH=0`). (This bug did not trigger with the 4 default folders because `4` is even.)

### 3. Implementation Fix (`REQ-2.3`)
- **File & Method:** `app/src/main/java/com/example/cs265final/feature/notes/LibraryView.java` → `renderFolders()`
- **Change:** Replaced `new View(getContext())` with `new android.widget.Space(getContext())` and `LayoutParams(0, 0, 1f)`:
  ```java
  // Space reports 0 under AT_MOST, whereas a plain View expands to fill the parent height.
  Space spacer = new Space(getContext());
  LayoutParams spacerParams = new LayoutParams(0, 0, 1f);
  spacerParams.setMarginStart(spacing);
  row.addView(spacer, spacerParams);
  ```
- **Why It Fixes the Cause:** `android.widget.Space` overrides `onMeasure` using `getDefaultSize2()`, which returns `Math.min(size, specSize)` (`0`) under `MeasureSpec.AT_MOST`, keeping the row height equal to the sibling folder card (`84dp`).

### 4. After-Fix Verification (`REQ-2.4`)
- **Executed Test:** `./gradlew :app:connectedDebugAndroidTest` (`CompatibilityAndFolderTest` and all 28 connected tests).
- **Result:** Passed (`28/28` passed, `0` failed). Notes below odd folder counts (e.g., 1 folder `"ทั่วไป"` and 5 folders in `evidence/screens/04-library-folders.png`) now measure and display at full height immediately.
- **After Evidence:** `evidence/challenge/after-connected-tests.xml` and `evidence/screens/04-library-folders.png`.

---

## Challenge 2 (Supporting): `SearchControls` XML Inflation Crash (`NoSuchMethodException`)

### 1. Feature & Symptom (`REQ-2.2`)
- **Affected Feature:** `SearchView` (`res/layout/view_search.xml` hosting `<com.example.cs265final.feature.search.SearchControls>`)
- **Observed Symptom:** Launching `MainActivity` crashed immediately in `onCreate` with `android.view.InflateException: Binary XML file line #12 in layout/view_search: Caused by: java.lang.NoSuchMethodException: SearchControls.<init> [Context, AttributeSet]`.
- **Before Evidence:** `evidence/challenge/before-xml-inflation.txt`

### 2. Root Cause & Fix (`REQ-2.3`)
- `SearchControls.java` had originally been constructed programmatically inside Compose `AndroidView(factory = { SearchControls(context) })` with only a single-argument constructor `SearchControls(Context)`.
- Android's `LayoutInflater` requires a two-argument constructor `(Context, AttributeSet)` whenever a custom `View` tag is declared in an XML layout.
- Added standard constructor chaining `SearchControls(Context)`, `SearchControls(Context, AttributeSet)`, and `SearchControls(Context, AttributeSet, int)` in `SearchControls.java`.

### 3. After-Fix Verification (`REQ-2.4`)
- **Result:** `MainActivity` inflates `view_search.xml` cleanly; `SearchControlsTest`, `MainActivityFlowsTest`, and `evidence/screens/06-search-scoped.png` all pass and render properly.
