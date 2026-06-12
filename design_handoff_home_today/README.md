# Handoff: Alkahf — Home / Today screen

## Overview
Alkahf is an offline-first Android app for memorizing the Quran (hifz). This handoff covers the
**Home / Today** screen — the launch destination that shows the user their plan for the day:
the new portion to learn (*sabaq*), the review queue due today (*murājaʿah*), a one-tap resume
of their last audio drill preset, and a quiet weekly-activity/streak summary. The mushaf page
(real Uthmani text) appears here in miniature as the hero of the *sabaq* card, because in this
app **the Arabic text is the product**.

## About the Design Files
The files in `design/` are a **design reference created in HTML** (a streaming prototype that
shows the intended look, layout, and copy). **They are not production code to copy.** The task is
to **recreate this screen natively in the Alkahf Android codebase** — Kotlin, Jetpack Compose,
Material 3 — using the project's established patterns, theme, and component library. Treat the
HTML purely as a precise visual spec; map every value below onto Compose (`MaterialTheme`,
`Surface`, `Card`, `Text`, `Icon`, `NavigationBar`, etc.).

- `design/Home Today.dc.html` — the screen. Open in a browser **with an internet connection**
  (it loads two web fonts from Google Fonts; see Assets). `support.js` must sit next to it.
- `home_today_preview.png` — rendered reference image of the full screen.

## Fidelity
**High-fidelity (hifi).** Final colors, typography, spacing, and layout. Recreate pixel-faithfully
within the codebase's Material 3 theme. Where a value below conflicts with an established token in
the app's theme, prefer the app's token but keep the visual result equivalent.

## Platform notes
- Native Android, Jetpack Compose, Material 3. minSdk 26 (Android 8.0+). Phone, portrait.
- All values below are expressed in **dp** for lengths and **sp** for text (the HTML uses px at a
  1:1 design scale — read 1px = 1dp, 1px font = 1sp).
- The device frame, status bar, and gesture pill in the HTML are **mock chrome** for presentation
  only — do **not** build them; the OS provides them.
- The screen is a single vertically scrolling column (`LazyColumn`/`Column` + `verticalScroll`).
  At reference sizes the content nearly fills a tall phone; on shorter devices it scrolls. The
  bottom navigation bar is fixed (Scaffold `bottomBar`).

---

## Screens / Views

### Home / Today
**Purpose:** Orient the user the moment they open the app and get them into one of three actions
(learn new, review due, resume drill) in one tap.

**Layout (top → bottom), inside a Scaffold:**
- Scaffold `bottomBar` = Navigation bar (see Components → Bottom navigation).
- Content = scrollable `Column`, horizontal padding **18dp**, vertical gap between cards **9dp**.
  1. **Header block** (padding 10dp/22dp/6dp)
  2. **Sabaq card** (hero)
  3. **Murājaʿah card**
  4. **Resume-drill card**
  5. **This-week card**

Screen background: **#F5F0E6** (warm paper). Status bar icons / content use dark ink **#2A2620**.

---

#### Component: Header
- Two-column row, top-aligned, space-between.
- **Left column:**
  - "Assalāmu ʿalaykum" — 14sp / weight 500 / color **#8C8475** / letter-spacing ~0.2 / `nowrap`.
  - "Today" — 31sp / weight 700 / color **#2A2620** / letter-spacing −0.5 / line-height 1.1 / top margin 3dp.
  - "Thursday, 11 June" — 13sp / weight 500 / color **#A39A8B** / top margin 4dp. (Bind to today's date.)
- **Right column — streak chip (deliberately subtle):**
  - Pill: background **#FCFAF4**, 1dp border **#EAE3D4**, corner radius 999dp (full), padding 7dp/12dp/7dp/10dp, row, gap 7dp, vertically centered.
  - Dot: 8dp circle **#3E7D6E** with a 3dp soft outer glow (`rgba(62,125,110,0.14)`).
  - "12" — 14sp / weight 700 / **#2A2620**.
  - "DAYS" — 11sp / weight 600 / **#A39A8B** / letter-spacing 0.3.

#### Component: Sabaq card (hero — "today's new portion")
- Surface: background **#EBF1ED** (pale green tint — this is what marks it as the primary task),
  1dp border **#D6E4DC**, corner radius 24dp, padding 18dp (top/sides) / 16dp (bottom), `overflow hidden`.
- **Accent rail:** a 3dp-wide vertical bar pinned to the **right** edge, inset 18dp top & bottom,
  radius 3dp, color **#3E7D6E** at 55% opacity. (Right edge because the content is RTL.)
- **Label row** (space-between):
  - "NEW · SABAQ" — 11sp / weight 700 / letter-spacing 1.4 / color **#2F6055**.
  - "Sūrat al-Kahf · 1–5" — 13sp / weight 600 / color **#6E665A** / `nowrap`.
- **Ayah block (the hero):**
  - Direction **RTL**, text-align right, font **Amiri Quran**, 25sp, line-height 1.88,
    color **#2A2620**, word-spacing ~1, margins 12dp top / 2dp bottom.
  - Text (Surah Al-Kahf, ayah 1), exact Unicode (Uthmani, with harakat):
    `ٱلْحَمْدُ لِلَّهِ ٱلَّذِىٓ أَنزَلَ عَلَىٰ عَبْدِهِ ٱلْكِتَٰبَ وَلَمْ يَجْعَل لَّهُۥ عِوَجَا`
  - Followed by the end-of-ayah marker `۝١` rendered at 21sp in **#3E7D6E** with 2dp horizontal padding.
    (In production, source ayah text + the numbered end-glyph from the bundled Tanzil dataset; the
    KFGQPC/Amiri end-of-ayah glyph carries the verse number.)
- **Progress row** (row, gap 10dp, top margin 8dp):
  - Five 9dp state dots, gap 5dp, left→right = ayat 1–5:
    `#3E7D6E` (memorized), `#3E7D6E`, `#D8B45A` (learning), `#E4DBC8` (not started), `#E4DBC8`.
  - "2 of 5 ayat memorized" — 12.5sp / weight 600 / **#6E665A**.
- **Action row** (row, gap 9dp, top margin 13dp):
  - **Primary button** "Continue learning": flex-fill, height 48dp, radius 14dp,
    background **#3E7D6E**, label **#FBFAF5** 15sp/600, leading play triangle (17dp, fill #FBFAF5),
    icon+label gap 8dp, drop shadow `0 4 12 rgba(62,125,110,0.26)`.
    *Pressed:* translateY 1dp + reduced shadow.
  - **Secondary icon button** "Hide & self-test": 48×48dp, radius 14dp, 1dp border **#C7D8CF**,
    background `rgba(255,255,255,0.5)`, eye-off icon stroke **#2F6055** 1.8dp. *Pressed:* bg `rgba(255,255,255,0.85)`.

#### Component: Murājaʿah card (review queue due today)
- Surface: background **#FCFAF4**, 1dp border **#EAE3D4**, radius 24dp, padding 18dp.
- **Label row** (space-between):
  - "REVIEW · MURĀJAʿAH" — 11sp / weight 700 / letter-spacing 1.4 / **#8C8475**.
  - "DUE TODAY" badge — 12sp / weight 700 / text **#B0863A** / background **#F6EFDD** / radius 999dp / padding 4dp/10dp.
- **Count row** (baseline, gap 9dp, top margin 10dp):
  - "5 portions" — 24sp / weight 700 / **#2A2620** / letter-spacing −0.4 / `nowrap`.
  - "≈ 8 min" — 13sp / weight 500 / **#A39A8B** / `nowrap`.
- **Chips** (wrap, gap 7dp, top margin 13dp). Each chip: 13sp / weight 500 / text **#4A453C** /
  background **#F4EFE4** / 1dp border **#EAE3D4** / radius 9dp / padding 6dp/11dp.
  Content: "Al-Fātiḥah", "An-Nās", "Al-Falaq", "Al-Ikhlāṣ", then a "+1" overflow chip (weight 600, text **#8C8475**).
- **Tonal button** "Start review": full width, height 46dp, radius 14dp, background **#E2EDE8**,
  label **#2F6055** 15sp/600, top margin 12dp. *Pressed:* bg **#D6E6DF**.

#### Component: Resume-drill card (quick-resume of last loop preset)
- Surface: background **#FCFAF4**, 1dp border **#EAE3D4**, radius 22dp, padding 12dp/14dp,
  row, gap 14dp, items centered.
- **Play button:** 50dp circle, background **#2A2620**, white play triangle (17dp, fill #F5F0E6). *Pressed:* scale 0.95.
- **Text block** (fills remaining width):
  - "RESUME DRILL" — 11sp / weight 700 / letter-spacing 1 / **#A39A8B**.
  - "Husary · al-Kahf 1–5" — 14.5sp / weight 600 / **#2A2620** / top margin 3dp.
  - "Cumulative chain · 3× each · 5× chain" — 12.5sp / weight 500 / **#8C8475**.
- **Trailing chevron:** 9×15dp, stroke **#C2BAA9** 2dp.

#### Component: This-week card (activity / streak, intentionally quiet)
- Surface: background **#FCFAF4**, 1dp border **#EAE3D4**, radius 22dp, padding 12dp/18dp,
  row, space-between, centered.
- **Left:**
  - "This week" — 14sp / weight 600 / **#2A2620**.
  - "6 of 7 days · 142 ayat" — 12.5sp / weight 500 / **#A39A8B** / top margin 2dp.
- **Right — 7-day dots** (row, gap 7dp, each = column of {square, weekday letter}, gap 5dp):
  - Square 18×18dp, radius 6dp. Filled days **#3E7D6E**; missed day **#E4DBC8**; **today** (Sun)
    **#6FA395** with a 3dp soft glow `rgba(62,125,110,0.16)`.
  - Day letters M T W T F S S — 10sp / weight 600 / **#B6AD9C**; today's letter weight 700 / **#3E7D6E**.
  - Reference week: M✓ T✓ W✓ T✗ F✓ S✓ S(today).

#### Component: Bottom navigation (Material 3 NavigationBar)
- Background **#F8F3EA**, 1dp top border **#EAE3D4**. Five equal destinations.
- Each: column, centered, gap 4dp → {indicator pill containing a 23dp icon} + label 11sp.
- **Active (Today):** pill 60×30dp radius 999dp, background **#DCEAE3**; icon stroke **#2F6055**;
  label weight 700 **#2F6055**.
- **Inactive:** no pill; icon stroke **#8C8475** ~1.7dp; label weight 600 **#8C8475**.
- Destinations & icons (line/outline style): **Today** = home, **Mushaf** = open book,
  **Review** = circular-refresh arrows, **Progress** = bar chart, **Library** = download tray.
  Use the app's Material Symbols equivalents.

---

## Interactions & Behavior
- **Continue learning** → opens the Mushaf/learning view at Al-Kahf ayah 1–5 (the current sabaq).
- **Hide & self-test** (eye-off) → opens the sabaq passage in hide/reveal self-test mode.
- **Start review** → opens the Review (murājaʿah) queue self-test flow.
- **Resume-drill play / card tap** → starts the Loop player immediately with the saved preset
  (reciter Husary, range al-Kahf 1–5, cumulative chaining 3× per ayah / 5× per chain).
- **Bottom nav** → switches top-level destination; Today is selected here.
- Button press states as specified per component (translate / scale / background shift). Keep
  Material 3 ripple if it's the app's convention; the prototype uses subtle press transforms.
- No loading/error/empty states are specified for this screen beyond binding real data (below).

## State Management
This screen is a read-only dashboard derived from local data (Room/SQLite) — no network.
State / data it needs:
- **Today's sabaq:** current surah + ayah range, and per-ayah memorization state
  (`not_started | learning | memorized | strong`) to drive the 5 state dots and the "X of N" line.
- **Review queue:** list of portions due today (from the SM-2 scheduler), count, estimated minutes,
  and the first few portion names for the chips (+overflow count).
- **Last loop preset:** reciter, range, chaining counts (for the resume card).
- **Streak / weekly activity:** current streak day count, and per-day practiced flags for the last
  7 days + ayat-reviewed total.
- **Today's date** for the header.
All of the above already exist (or are planned) in the app's local store; this screen only reads them.

## Design Tokens

**Colors**
| Token | Hex | Use |
|---|---|---|
| Paper / screen bg | `#F5F0E6` | screen background |
| Page (outside phone, mock only) | `#E7E1D4` | n/a in app |
| Surface / card | `#FCFAF4` | murājaʿah, resume, week cards |
| Surface tint (hero) | `#EBF1ED` | sabaq card bg |
| Nav surface | `#F8F3EA` | bottom nav bg |
| Card border | `#EAE3D4` | hairlines |
| Card border (hero) | `#D6E4DC` | sabaq border |
| Chip bg | `#F4EFE4` | review chips |
| Ink (primary text) | `#2A2620` | titles, ayah, play btn |
| Ink secondary | `#6E665A` / `#4A453C` | sub-labels / chip text |
| Ink muted | `#8C8475` | labels |
| Ink faint | `#A39A8B` / `#B6AD9C` | dates, captions, day letters |
| Accent (primary) | `#3E7D6E` | buttons, active dot, filled state |
| Accent deep | `#2F6055` | accent text / active nav |
| Accent tint | `#DCEAE3` | active nav pill |
| Accent tint 2 | `#E2EDE8` | tonal button bg |
| Accent light | `#6FA395` | today's week dot |
| Learning (amber) | `#D8B45A` | ayah state dot |
| Not-started / missed | `#E4DBC8` | empty state dot / missed day |
| Gold (review badge) | `#B0863A` text on `#F6EFDD` | "Due today" badge |

**Typography** — UI font **Hanken Grotesk** (400/500/600/700); Arabic font **Amiri Quran** (400).
Scale used: 31 / 24 / 15 / 14.5 / 14 / 13 / 12.5 / 11 / 10 sp. Arabic 25sp, line-height 1.88.

**Spacing** — card gap 9dp; screen h-padding 18dp; card padding 12–18dp; chip padding 6/11dp.

**Radius** — cards 22–24dp; buttons / chips 9–14dp; pills 999dp; state dots full.

**Shadow** — primary button `0 4 12 rgba(62,125,110,0.26)`. Cards are border-only (no shadow).

## Assets
- **Fonts (replace CDN with bundled assets in the app):**
  - UI: **Hanken Grotesk** — Google Fonts (OFL). Bundle the weights 400/500/600/700.
  - Arabic: **Amiri Quran** — Google Fonts (OFL). NOTE: the product spec targets the **KFGQPC
    Uthmanic Script HAFS** font for v1; Amiri Quran is used in this prototype because it renders
    Uthmani harakat/ligatures correctly in a browser. Use the real KFGQPC font (or the Tanzil/QPC
    page fonts) in the app. Generous line spacing for harakat is required either way.
- **Icons:** all icons are simple line/outline glyphs — use the app's Material Symbols set
  (home, menu_book, refresh/autorenew, bar_chart, download). No custom raster assets.
- **No images.** The Arabic text is real Unicode, not an image.

## Files
- `design/Home Today.dc.html` — the Home/Today screen prototype (open in a browser, online).
- `design/support.js` — runtime required by the prototype (keep next to the HTML).
- `home_today_preview.png` — rendered reference image.
