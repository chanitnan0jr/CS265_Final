# CS265 — AI Context Index

> **อ่านไฟล์นี้ก่อน; ไม่ต้องโหลดทุกสไลด์พร้อมกัน**  
> **Assessment source:** `../requirement.md` (Rubric 20 ข้อ)  
> **Progress & evidence:** `../Checklist.md` (20-item verification tracker)  
> **Context policy:** อ่านเฉพาะ BMxx.md ที่เกี่ยวข้องกับงานปัจจุบัน อ้าง `CS265 BMxx, slide N`

| Reference | Focus | Relevant rubric |
|---|---|---|
| `BM01.md` | Mobile constraints, device variety, touch/sound input, navigation, app types | REQ-1.3, 3.1, 4.1 |
| `BM02.md` | Android platforms, lifecycle, back stack, app quality, accessibility/security | REQ-1.3, 3.1–3.4, 4.1–4.4 |
| `BM03.md` | Android Studio project, XML/Compose layouts, dimensions, multi-device layout resources | REQ-1.3, 3.1–3.3 |
| `BM04.md` | Event listeners, keyboard/click/touch, Intent extras between Activities, Manifest/theme | REQ-1.3, 2.1–2.4, 4.1, 3.4 |
| `BM05.md` | Android resources, locale/density/orientation qualifiers, adaptive layouts, day/night modes | REQ-1.3, 3.1–3.4, 2.2–2.4 |
| `BM06.md` | Runtime configuration changes, Activity recreation, `configChanges`, locale/fontScale/uiMode | REQ-1.3, 3.2–3.4, 2.2–2.4; 4.4 (related, not sufficient) |
| `BM07.md` | Android input controls: EditText/Button, autocomplete, radio/checkbox, Spinner, date/time pickers, Switch, RatingBar | REQ-1.3, 1.1–1.2, 2.1–2.4, 3.1, 3.3 |
| `BM08.md` | SQLite CRUD, RecyclerView, Intent record IDs, delete confirmation, Activity Result, image-to-Base64 example | REQ-1.3, 1.1–1.2, 2.1–2.4, 4.1, 4.3, 5.1 |
| `BM09.md` | Course project planning, Use Case Diagram/Specification, screen flows, positive/negative/boundary tests, test-case evidence | REQ-1.1–1.3, 2.1, 5.1, 5.4 |

**Coverage:** สรุปครบ BM01–BM09 ตามไฟล์ต้นฉบับที่ได้รับทั้ง 9 ชุด. **BM08** มีชื่อบท *SQLite Database* และ **BM09** มีชื่อบท *Course Project Planning and Requirements* ตามหน้าปกจริงของเอกสาร แม้หัวข้อในตารางสอนของ BM01 จะต่างออกไป.

## Retrieval rule for AI

1. ใช้ `requirement.md` เพื่อดูเกณฑ์ที่ต้องผ่าน; ใช้ BMxx เป็น *แหล่งอ้างอิงเนื้อหาวิชา* ไม่ใช่รายการฟีเจอร์บังคับใหม่
2. เริ่มจาก `INDEX.md` → เลือก 1–2 ไฟล์ที่เกี่ยวข้อง → อ้าง concept พร้อมเลขหน้า → เทียบโค้ด/ผลทดสอบจริง
3. สำหรับคำถามเฉพาะ เช่น `background/foreground Activity lifecycle` ให้ใช้ `BM02.md`; สำหรับ `input forms / Spinner / Picker` ให้ใช้ `BM07.md`; สำหรับ `rotation/locale/font-scale configuration changes` ให้ใช้ `BM06.md`; สำหรับ `Use Case/Test Cases/การนำเสนอภาพหน้าจอ` ให้ใช้ `BM09.md` ไม่ต้องโหลดทุกบท
4. ต้องแยก `[Slide fact]`, `[Project application]`, `[Evidence verified]`; ห้ามอ้างว่าได้ implement/ผ่านทดสอบแล้วโดยไม่มีหลักฐาน

**Note:** `../reference.md` เดิมเป็น BM01 ฉบับละเอียด (48KB); ควรใช้ `BM01.md` ฉบับย่อแทนในการป้อน AI context ตามปกติ
