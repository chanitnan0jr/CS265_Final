# Voice2Note — End-to-End User Flow & Device Compatibility (`REQ-5.1`, `REQ-3.1–3.4`)

## 1. Annotated End-to-End User Journey (`REQ-5.1`, `BM09 pp. 4–6, 9`)

| Step | Screen | User Action | Observed Result | Screenshot |
|---|---|---|---|---|
| 1 | Home (`HomeView`) | Open Voice2Note in Thai Light mode | Displays prompt `วันนี้อยากบันทึกอะไร?`, Mic CTA, Upload & Create buttons, recent notes list, and bottom navigation | `evidence/screens/01-home-thai.png` |
| 2 | Recording (`RecordingView`) | Tap **สร้างโน้ตใหม่** (`button_home_new_note`), enter transcript `"Demo CS265 Java XML"` and title `"CS265 Demo"` | Live transcript editor, folder chips, 5 mood chips (`ปกติ` selected), waveform, and Save button displayed cleanly | `evidence/screens/02-recording-composer.png` |
| 3 | Home (`HomeView`) | Tap **บันทึกโน้ต** (`button_recording_save`) | Validates non-empty transcript, persists `CS265 Demo` to `NoteStorage`, and returns to Home showing `CS265 Demo` at the top of Recent Notes | `evidence/screens/03-home-with-note.png` |
| 4 | Library (`LibraryView`) | Tap **คลังโน้ต** (`nav_library`) | Shows source filter pills (`ทั้งหมด`, `เสียงที่บันทึก`, `ไฟล์ที่อัปโหลด`), 2-column folder grid including `ทั่วไป`, and filtered notes below | `evidence/screens/04-library-folders.png` |
| 5 | Calendar (`CalendarView`) | Tap **บันทึกรายวัน** (`nav_calendar`) | Shows November 2026 calendar, dot indicator on day 17, coral selection highlight, and 4 notes for `17/11/2026` | `evidence/screens/05-calendar-journal.png` |
| 6 | Search (`SearchView`) | Tap **ค้นหา** (`nav_search`) and type `"CS265"` | Filters library case-insensitively with folder scope spinner (`ทุกโฟลเดอร์`), reporting `พบ 2 รายการ` (`CS265 Demo`, `สรุปเลกเชอร์ CS265`) | `evidence/screens/06-search-scoped.png` |
| 7 | Note Detail (`NoteDetailView`) | Tap `CS265 Demo` card | Displays full note title, date, mood/folder pills, audio waveform bar, body card, Attach/Play button, and Copy/Edit/Share actions | `evidence/screens/07-note-detail.png` |
| 8 | Delete Confirmation (`DeleteNoteConfirmation`) | Open `⋮` menu (`button_detail_options`) and tap **ลบโน้ต** | Displays confirmation dialog `ลบโน้ตนี้หรือไม่?` (`ยกเลิก` keeps note; `ลบ` removes note from storage and updates UI) | `evidence/screens/08-delete-confirm-dialog.png` |

---

## 2. Device, Orientation, Language & Theme Compatibility (`REQ-3.1–3.4`)

| Criterion | Configuration Tested | Observed Result | Screenshot |
|---|---|---|---|
| **REQ-3.1 (Screen 1)** | Phone `1080×2400 @ 420 dpi` (`411dp × 914dp`, `layout/activity_main.xml`) | Text, buttons, and cards render without clipping or overlap; bottom nav bar active | `evidence/device-compatibility/compat-screen-phone-1080x2400.png` |
| **REQ-3.1 (Screen 2)** | Tablet `1600×2560 @ 320 dpi` (`800dp × 1280dp`, `layout-sw600dp/activity_main.xml`) | Switches to `176dp` left navigation rail with spacious centered content pane; zero clipping | `evidence/device-compatibility/compat-screen-tablet-1600x2560.png` |
| **REQ-3.2 (Orientation)** | Choice A: Portrait & Landscape supported (`user_rotation=0` and `user_rotation=1`) | Landscape displays scrollable content and bottom bar cleanly; rotation preserves drafts via `onSaveInstanceState` & `RecordingDraftViewModel` | `evidence/device-compatibility/compat-orientation-landscape.png` |
| **REQ-3.3 (Thai & English)** | `cmd locale set-app-locales ... th` vs `en` | Menus, buttons, tabs, folder labels (`การเรียน` ↔ `Study`), and messages switch cleanly with zero hardcoded UI strings | `compat-lang-th.png`, `compat-lang-en.png` |
| **REQ-3.4 (Day & Night)** | `cmd uimode night no` vs `yes` (`values/` vs `values-night/`) | Light (`#FAF5F2` / `#2F2726`) and Dark (`#181414` / `#F5ECE8`) palettes maintain readable contrast across cards, buttons, and status bar | `compat-theme-light.png`, `compat-theme-dark.png` |

---

## 3. Navigation & Back Rules (`REQ-4.1`, `REQ-4.2`)

```text
Home (nav_home)
 ├── Back ──▶ Exit Confirmation Dialog ("Exit Voice2Note?")
 │              ├── Stay   ──▶ Remains on Home
 │              └── Exit   ──▶ finish()
 ├── Library / Calendar / Search tabs ──(Back)──▶ Home
 ├── RecordingView
 │     ├── Empty draft & idle ──(Back)──▶ Previous tab
 │     └── Unsaved draft / listening ──(Back)──▶ Discard Draft Dialog
 │                                                  ├── Keep editing ──▶ Remains in RecordingView
 │                                                  └── Discard      ──▶ Clears draft, returns to tab
 ├── NoteDetailView
 │     ├── Inline rename open ──(Back)──▶ Closes rename editor first
 │     └── Normal detail      ──(Back)──▶ Previous tab
 └── PlaybackView ──(Back)──▶ NoteDetailView (if selected) or Home
```
