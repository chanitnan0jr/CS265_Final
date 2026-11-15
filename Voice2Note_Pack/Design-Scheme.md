# Voice2Note — Design Scheme

Version: 1.0  
Style direction: soft, aesthetic, calm, mobile-first  
Primary market: Thai users  
Platform: Android mobile app

---

## 1. Design Direction

Voice2Note should feel like a mix of:
- a calm journaling app
- a modern voice tool
- a personal emotional notebook

### Visual keywords
- soft
- warm
- reflective
- clean
- safe
- elegant
- feminine-neutral
- premium but approachable

The app should avoid looking too corporate, too technical, or too harsh.

---

## 2. UX Principles

1. **Speak first** — voice capture is the core action.
2. **Low friction** — user should be able to start speaking quickly.
3. **Emotion-safe** — the UI should feel emotionally gentle.
4. **Organized later** — structure should help after capture, not block capture.
5. **Readable Thai UI** — typography and spacing should prioritize Thai legibility.
6. **Aesthetic but functional** — beautiful, but still practical.

---

## 3. Core UI Sections

### 3.1 Home / Start Recording (`หน้าหลัก`)
Purpose: immediate entry point

Elements:
- Top bar: App logo + `Voice2Note` title + notification bell icon
- Hero copy: `วันนี้อยากบันทึกอะไร?` / `พูดได้เลย ไม่ว่าจะเป็นความคิด ความรู้สึก หรือโน้ตการเรียน`
- Soft layered hill accent + side waveform bars flanking a large circular (`999 px`) primary coral microphone CTA
- Secondary quick action row:
  - `อัปโหลดไฟล์เสียง (MP3)`
  - `สร้างโน้ตใหม่`
- Recently saved note preview (`บันทึกล่าสุด` with `ดูทั้งหมด >`):
  - Play button icon, note title, duration & timestamp (`2 นาที • วันนี้ 09:40`), and `⋮` overflow menu
- Bottom navigation

### 3.2 Live Transcription Screen (`บันทึกเสียง (ถอดข้อความสด)`)
Purpose: capture live speech and mood

Elements:
- Top bar: back button (`←`), title `บันทึกเสียง (ถอดข้อความสด)`, and `⋮` overflow menu
- Status pill & waveform card: `🔴 กำลังบันทึก • 00:02:36` above a centered live waveform visualization
- Live editable Thai transcript card with smooth line height and active cursor
- Mood selector (`อารมณ์ตอนนี้`) with 5 rounded pill chips: `ดีมาก`, `ดี`, `ปกติ`, `เครียด`, `เศร้า`
- Bottom transport row: bookmark (`🔖`), large circular pause/resume (`⏸`), stop (`⏹`)

### 3.3 Daily Journal Screen (`บันทึกรายวัน`)
Purpose: browse notes by date

Elements:
- Header: calendar icon + `บันทึกรายวัน` + `⋮` menu
- Month calendar card: `< พฤศจิกายน 2026 >`, Thai weekday headers (`อา จ อ พ พฤ ศ ส`), circular coral highlight on selected date, small dot indicators under dates with notes
- Selected date list (`บันทึกวันที่ ...` + `N รายการ`): note cards with play icon, title, duration/time, and mood or folder badge
- Floating `+` primary CTA in the bottom-right corner

### 3.4 Note Library / Folder Screen (`คลังโน้ต`)
Purpose: browse and organize content

Elements:
- Header: folder icon + `คลังโน้ต` + search (`🔍`) and `⋮` actions
- Category filter chips: `ทั้งหมด`, `เสียงที่บันทึก`, `ไฟล์ที่อัปโหลด`
- `โฟลเดอร์` (`จัดการ >`) 2x2 tinted grid with 4 default folders:
  - `การเรียน` (graduation cap icon, item count)
  - `ระบายความรู้สึก` (heart icon, item count)
  - `งาน / โปรเจกต์` (briefcase icon, item count)
  - `ไอเดีย` (bookmark icon, item count)
- `โน้ตล่าสุด` (`ดูทั้งหมด >`) list with play icon, title, duration/relative time, and `⋮` menu

---

## 4. Visual Style

## 4.1 Overall tone
- bright or warm-light theme preferred
- soft shadow instead of harsh contrast
- slightly rounded cards
- elegant iconography
- minimal clutter
- calm spacing and breathable layouts

## 4.2 Shapes
- rounded buttons
- rounded cards
- circular record CTA
- compact but soft chips for emotion tags

Recommended radius:
- small: 12 px
- medium: 18 px
- large cards: 24 px
- circular primary CTA: 999 px

---

## 5. Color System

Use soft muted tones, not aggressive neon colors.

### Core Material 3 / Compose Tokens
| Token             | Hex       | Compose `Color`     | Role                                                                |
|-------------------|-----------|---------------------|---------------------------------------------------------------------|
| `BackgroundCream` | `#FAF5F2` | `Color(0xFFFAF5F2)` | Main screen background (`cream` in `MainActivity.kt`: `0xFFFFF8F5`) |
| `SurfaceWhite`    | `#FFFFFF` | `Color(0xFFFFFFFF)` | Cards, bottom bar, input fields                                     |
| `SurfaceTonal`    | `#F5ECE8` | `Color(0xFFF5ECE8)` | Status pill, subtle secondary containers                            |
| `PrimaryCoral`    | `#C87468` | `Color(0xFFC87468)` | Primary record CTA, active calendar day, primary action highlights  |
| `SecondaryRose`   | `#AA6D78` | `Color(0xFFAA6D78)` | App accent (`rose` in codebase), metadata text, secondary icons     |
| `TextPrimary`     | `#2F2726` | `Color(0xFF2F2726)` | Deep charcoal headings and transcript body                          |
| `TextSecondary`   | `#7D716E` | `Color(0xFF7D716E)` | Warm gray durations, timestamps, subtitles                          |
| `BorderSoft`      | `#E8DDD9` | `Color(0xFFE8DDD9)` | Soft hairline card/chip strokes                                     |

### Emotion Colors (`อารมณ์ตอนนี้`)
| Thai Label | Meaning   | Container Hex | Content / Icon Hex |
|------------|-----------|---------------|--------------------|
| `ดีมาก`    | Very Good | `#E8EFE9`     | `#5C8A67`          |
| `ดี`       | Good      | `#FBF0E4`     | `#C98A4B`          |
| `ปกติ`     | Normal    | `#F5E6E4`     | `#C87468`          |
| `เครียด`   | Stressed  | `#F6E5DF`     | `#B85D4B`          |
| `เศร้า`    | Sad       | `#EAE8F0`     | `#6E6A85`          |

### Default Folder Card Tints
| Folder (`โฟลเดอร์`) | Background Hex |
|---------------------|----------------|
| `การเรียน`          | `#ECE6F0`      |
| `ระบายความรู้สึก`   | `#F5E7E5`      |
| `งาน / โปรเจกต์`    | `#F7ECE2`      |
| `ไอเดีย`            | `#E9EEE8`      |

Avoid overly saturated red except for the small active recording dot (`🔴 กำลังบันทึก`).

---

## 6. Typography

The UI should support Thai comfortably without clipped tone marks or tight line spacing.

### Font Family & Compose `Typography` Scale
Preferred fonts: **Noto Sans Thai** or **Anuphan** (fallback to Android system Thai sans-serif).

| Compose Role     | Size    | Line Height | Weight                | Usage                                                |
|------------------|---------|-------------|-----------------------|------------------------------------------------------|
| `headlineMedium` | `24.sp` | `34.sp`     | `FontWeight.Bold`     | Hero prompt (`วันนี้อยากบันทึกอะไร?`), screen titles |
| `headlineSmall`  | `20.sp` | `28.sp`     | `FontWeight.SemiBold` | Section titles (`บันทึกรายวัน`, `คลังโน้ต`)          |
| `titleMedium`    | `16.sp` | `24.sp`     | `FontWeight.SemiBold` | Note card titles, folder titles                      |
| `bodyLarge`      | `16.sp` | `26.sp`     | `FontWeight.Normal`   | Live & saved Thai transcript text                    |
| `bodyMedium`     | `14.sp` | `20.sp`     | `FontWeight.Normal`   | Supporting copy, chip labels                         |
| `labelSmall`     | `12.sp` | `16.sp`     | `FontWeight.Medium`   | Duration, timestamp, folder/mood metadata            |

### Typography Tone
- friendly, modern, not playful-cartoon
- smooth curves fit the journaling personality

---

## 7. Icon Style

The icon system should look more aesthetic and softer than a generic utility app.

### App icon direction
- microphone + note / transcript motif
- soft rounded square background
- layered line art
- elegant, minimal, recognizable
- subtle gradient allowed

### In-app icons
- thin to medium weight
- rounded line icons
- consistent corner softness
- use accent color sparingly

### Core icons needed
- mic
- upload
- folder
- calendar
- search
- home
- note
- play
- pause
- mood / heart / reflection

---

## 8. Navigation Scheme

Recommended bottom navigation (4 primary tabs):
- `หน้าหลัก` (Home)
- `คลังโน้ต` (Library)
- `บันทึกรายวัน` (Calendar / Daily Journal)
- `ค้นหา` (Search)

Optional later:
- Profile / Me

Avoid too many tabs in MVP.

---

## 9. Component Guidance

## Buttons
- Primary: filled soft rose button
- Secondary: outlined or tonal button
- Icon buttons: circular with soft shadow

## Cards
- Gently elevated
- Good padding
- Simple metadata rows
- No harsh borders

## Search bar
- Rounded
- Soft border or tonal fill
- Thai placeholder text

## Folder cards
- Color-coded but muted
- Small supporting note count
- Icon + title + item count

## Mood chips
- Rounded pills
- Light tonal background
- Selected state uses slightly deeper tint

---

## 10. Motion & Interaction

Use subtle motion only.

- Button press: soft scale down
- Card hover/press: tiny elevation shift (if applicable)
- Record CTA pulse: soft breathing effect
- Waveform movement: light and smooth
- Floating action button: subtle lift
- Page transitions: fast but gentle

Motion should support calmness, not excitement.

---

## 11. Content Tone (Thai UI)

The microcopy should feel:
- kind
- calm
- supportive
- non-judgmental

### Examples
- วันนี้อยากบันทึกอะไร?
- พูดได้เลย เราจะช่วยจดให้
- ความรู้สึกตอนนี้เป็นแบบไหน?
- โน้ตของวันนี้
- ฟังซ้ำและทบทวนเมื่อพร้อม

Avoid robotic or overly formal copy.

---

## 12. Screen Priority for Design Implementation

Priority 1:
- Home
- Live transcription
- Daily journal
- Library

Priority 2:
- Note detail
- Upload progress
- Folder management
- Search results

Priority 3:
- Playback transcript sync
- Mood analytics
- Summary / reflection insight

---

## 13. Example Asset Usage

This package includes `example-picture.png` as a visual reference for:
- overall color tone
- UI arrangement
- softness of shapes
- typography balance
- app mood and product style

It should be used as a direction reference, not treated as final production UI.

---

## 14. Final Design Summary

Voice2Note should feel like:
> a soft, emotionally safe, Thai-first voice journaling app that turns spoken thoughts into organized notes.

It should balance:
- beauty and usefulness
- calmness and clarity
- emotion and structure
- modern UI and easy note capture

