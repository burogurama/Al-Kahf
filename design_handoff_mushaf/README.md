# Handoff: Alkahf — Mushaf (reading + self-test) screen

## Overview
Alkahf is an offline-first Android app for memorizing the Quran (hifz). This handoff covers the
**Mushaf** screen — the reading view that doubles as the app's self-testing tool. It renders a real
mushaf page (continuous, justified Uthmani text) and supports **hide/reveal recall**: ayat are
concealed so the user recites from memory and reveals to check themselves, marking any word they
**stumble** on. In this app **the Arabic text is the product**, so typography quality and a stable,
"sacred" page layout are paramount — ayah position on the page is itself a memorization cue.

The mockup captures the screen **mid self-test**: the first ayat have been recalled and are crisp,
the later ayat are still concealed.

## About the Design Files
The files in `design/` are a **design reference created in HTML** (a streaming prototype that shows
the intended look, layout, and copy). **They are not production code to copy.** The task is to
**recreate this screen natively in the Alkahf Android codebase** — Kotlin, Jetpack Compose,
Material 3 — using the project's established patterns, theme, and component library. Treat the HTML
purely as a precise visual spec; map every value below onto Compose.

- `design/Mushaf.dc.html` — the screen. Open in a browser **with an internet connection** (it loads
  two web fonts from Google Fonts; see Assets). `support.js` must sit next to it.
- `mushaf_preview.png` — rendered reference image of the full screen (mid self-test).

## Fidelity
**High-fidelity (hifi).** Final colors, typography, spacing, and layout. Recreate pixel-faithfully
within the codebase's Material 3 theme. Where a value below conflicts with an established token in
the app's theme, prefer the app's token but keep the visual result equivalent.

## Platform notes
- Native Android, Jetpack Compose, Material 3. minSdk 26 (Android 8.0+). Phone, portrait.
- Lengths in **dp**, text in **sp** (the HTML uses px at 1:1 design scale — read 1px = 1dp, 1px font = 1sp).
- The device frame, status bar, and gesture pill in the HTML are **mock chrome** for presentation
  only — do **not** build them; the OS provides them.
- This screen **replaces** the bottom navigation bar with an immersive reading layout: a slim top
  app bar, the scrolling page, and a fixed self-test dock at the bottom (the thumb zone). Reading
  chrome should be minimal and ideally auto-hide during pure reading (out of scope to spec here, but
  keep the structure ready for it).
- **RTL is fundamental.** The Arabic content is RTL; page-turn swipes go right-to-left; the reveal
  progression and any progress fills run right-to-left in the mushaf context. Set the page container
  to RTL and let the surrounding UI chrome follow the app's locale (English LTR is fine for the chrome).

---

## Screens / Views

### Mushaf — reading + self-test
**Purpose:** Read the mushaf page and self-test on it. The page is the hero; everything else is quiet
chrome that gets out of the way.

**Structure (top → bottom), inside a Scaffold-like column:**
1. **Top app bar** (fixed, height 54dp)
2. **Page** (scrollable, fills remaining height) — background is a slightly warmer paper than the
   surrounding chrome
3. **Self-test dock** (fixed, thumb zone)

Chrome surface (top bar + dock): **#F5F0E6 / #F8F3EA**. Page surface: **#FBF7ED** (a touch warmer/
lighter than the chrome, so the page reads as a distinct sheet).

---

#### Component: Top app bar
- Row, height 54dp, vertical-centered, horizontal padding 10dp, 1dp bottom border **#ECE5D6**.
- **Leading:** 40×40dp back button — chevron-left, stroke **#5C5648** 2dp.
- **Center (title block, centered):**
  - "Al-Kahf" — 16sp / weight 700 / **#2A2620** / letter-spacing −0.2.
  - "Juzʼ 15 · Page 293" — 11sp / weight 500 / **#A39A8B** / top margin 1dp. (Bind to current location.)
- **Trailing (two 40×40dp actions):**
  1. **Hide-mode toggle (active here):** 34dp circle, background **#DCEAE3**, eye-off icon stroke
     **#2F6055** 1.8dp. The filled circle indicates hide/self-test mode is ON. When OFF: no circle,
     a plain eye icon in muted ink **#8C8475**.
  2. **Text-size:** an "Aa" glyph (small "A" 13sp + large "A" 20sp, baseline-aligned, weight 700,
     **#5C5648**). Opens font-size/spacing controls.

#### Component: The page (mushaf)
Background **#FBF7ED**, content padding 14dp top / 24dp sides / 18dp bottom, laid out as a column
that fills the available height (so the footer sits at the bottom of short pages). Top→bottom:

- **Surah header band** (row, centered, gap 14dp, top margin 2dp):
  - Left & right: 1dp hairlines fading to transparent (`linear-gradient` to/from **#DCD2BD**).
  - Center stack:
    - Surah name in Arabic — font **Amiri Quran**, 25sp, **#2A2620**, line-height 1.2, **nowrap**.
      Text: `سُورَةُ ٱلْكَهْف`
    - "MAKKĪ · 110 ĀYĀT" — 10sp / weight 700 / letter-spacing 2 / **#B0A691** / top margin 5dp.
- **Basmala** (centered) — font **Amiri Quran**, 20sp, **#5C5648**, margins 12dp top / 10dp bottom.
  Text: `بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ`
- **Body (the ayat)** — one continuous **RTL justified** text block:
  - Font **Amiri Quran**, 22sp, line-height 1.92, color **#2A2620**, word-spacing ~1.
  - `text-align: justify` with the last line centered (Compose: justify the paragraph; center the
    final line). This is what makes it read as a real mushaf page rather than ragged text.
  - **Ayah end-markers (medallions):** the glyph `۝` + the Arabic-Indic verse number, rendered in
    accent green **#3E7D6E**, with ~3dp horizontal padding. In production, source these from the
    KFGQPC/QPC font (the end-of-ayah glyph carries the number). Markers stay visible even when an
    ayah's text is concealed — they preserve page structure (the memorization cue).
  - **State per ayah (this is the self-test in progress):**
    - **Revealed/recalled ayat (1–3):** full-strength ink **#2A2620**; green medallions **#3E7D6E**.
    - **Concealed ayat (4–5):** the *text* gets a soft Gaussian **blur (~4.5dp radius)** and a faded
      ink **#B3AA98**; their medallions are de-emphasized to **#C8BFAD** (no green) so you can see
      where they are without reading them. Tapping reveals (word-by-word / whole ayah).
- **Footer** (row, space-between, top margin 20dp, 12dp top padding, 1dp top border **#ECE5D6**):
  - Left: "Ḥizb 30 · Juzʼ 15" — 11sp / weight 600 / **#B0A691** / letter-spacing 0.4.
  - Center: page number in Arabic-Indic digits `٢٩٣` — 12sp / weight 700 / **#8C8475**.
  - Right: "Al-Kahf" — 11sp / weight 600 / **#B0A691** / letter-spacing 0.4.

#### Component: Stumble marker (on the page)
Two coordinated indicators on a word the user marked as stumbled (shown on the word *aṣ-ṣāliḥāt* in
ayah 2 of the mockup):
- **Underline** on the stumbled word: amber **#C28A45**, 2dp thick, offset ~9dp below the baseline
  (so it clears the harakat). In Compose, draw this as a custom underline/`drawBehind` rather than
  the default text underline so the offset clears diacritics.
- **Margin dot** on that ayah's medallion: a 6dp amber **#C28A45** circle pinned to the top-left of
  the medallion glyph. This is the per-ayah "has stumbles" indicator that feeds the review scheduler
  and the page heatmap.
- (Heatmap extension, not in this mockup: ayat with more stumbles can carry a faint amber tint behind
  them. Keep the marker model able to express intensity.)

#### Component: Self-test dock (bottom, thumb zone)
Fixed bar, background **#F8F3EA**, 1dp top border **#E7DFCF**, padding 12dp/16dp/10dp, soft top
shadow `0 -6 18 rgba(40,34,22,0.05)`. Contents:
- **Status row** (space-between, bottom margin 3dp):
  - Left: a 7dp pulsing dot **#3E7D6E** (gentle opacity pulse ~1.8s) + "SELF-TEST" 11sp / weight 700
    / letter-spacing 1.2 / **#2F6055**.
  - Right: "Reciting ayah 4 of 5" — 12sp / weight 600 / **#A39A8B**. (Bind to current test position.)
- **Hint line** (centered): "Tap a word to reveal · long-press for the full ayah" — 11.5sp / weight 500
  / **#A39A8B** / margins 2dp top / 11dp bottom.
- **Action row** (row, gap 10dp):
  - **Stumble button** (secondary, hugs content): height 52dp, padding 0/18dp, radius 15dp, 1.5dp
    border **#E3C99F**, background **#FAF2E4**, label "Stumble" 14.5sp/600 **#A8702A**, leading flag
    icon stroke **#A8702A** 1.8dp, gap 8dp. *Pressed:* bg **#F3E7D2**. Amber because it's the
    mistake/caution action.
  - **Reveal button** (primary, fills): height 52dp, radius 15dp, background **#3E7D6E**, label
    "Reveal next ayah" 15.5sp/600 **#FBFAF5**, leading eye icon stroke **#FBFAF5** 1.9dp, gap 9dp,
    shadow `0 4 13 rgba(62,125,110,0.28)`. *Pressed:* translateY 1dp + reduced shadow.

---

## Interactions & Behavior
- **Tap a word** → reveals the next concealed word (word-by-word reveal). **Long-press an ayah** →
  reveals that whole ayah at once. Reveal removes the blur on that span and restores full-ink text
  and the green medallion.
- **Reveal next ayah** (dock primary) → reveals the current concealed ayah in full and advances the
  "Reciting ayah X of N" counter. The default hide-mode granularity for this app is **ayah-by-ayah**;
  word-by-word is available via tapping.
- **Stumble** (dock secondary) → marks the current word/ayah as stumbled: draws the amber underline +
  margin dot and records it for the scheduler (a stumble lowers that portion's next review grade).
- **Hide-mode toggle** (top bar) → turns self-test concealment on/off. When OFF, the page is a clean
  reader (all ayat crisp, no blur) and the dock can be hidden.
- **Text-size "Aa"** → opens font-size / line-spacing controls (large adjustable sizes are a product
  requirement; generous line spacing for harakat must be preserved at every size).
- **Vertical scroll** moves through the page; **horizontal swipe (right-to-left)** turns to the next
  page (page-turn is the canonical mushaf gesture — preserve page boundaries; do not infinitely
  reflow across pages).
- Concealed ayat keep their medallions visible at all times so page structure is stable.

## State Management
Read/write against local data (Room/SQLite); no network on this screen.
- **Current location:** surah, page number, juzʼ/ḥizb, and the ayah range visible on the page (the
  data model is page/line-aware).
- **Page text:** ayah Unicode (Uthmani) + numbered end-of-ayah glyphs from the bundled Tanzil/QPC
  dataset, with line/page structure preserved.
- **Self-test session state:** which ayat are revealed vs concealed, current "reciting ayah X of N",
  and per-word reveal progress.
- **Stumble records:** per-word / per-ayah stumble marks (persisted; they feed the SM-2 review
  scheduler and the progress heatmap).
- **Reading preferences:** font size, line spacing, theme (light here; a dark night-reading theme is
  planned).

## Design Tokens

**Colors**
| Token | Hex | Use |
|---|---|---|
| Chrome / screen bg | `#F5F0E6` | top bar area, dock base |
| Dock surface | `#F8F3EA` | self-test dock bg |
| Page surface | `#FBF7ED` | the mushaf sheet (warmer paper) |
| Hairline / border | `#ECE5D6` | top bar & footer borders |
| Dock border | `#E7DFCF` | dock top border |
| Header rule | `#DCD2BD` | surah-band hairlines |
| Ink (primary text) | `#2A2620` | recalled ayat, titles |
| Ink secondary | `#5C5648` | basmala, back chevron, Aa |
| Ink muted | `#8C8475` | page number |
| Ink faint | `#A39A8B` / `#B0A691` | sub-labels, footer, status |
| Concealed ink | `#B3AA98` | blurred ayah text |
| Concealed medallion | `#C8BFAD` | de-emphasized ayah marker |
| Accent (primary) | `#3E7D6E` | medallions, reveal button, pulse dot |
| Accent deep | `#2F6055` | active toggle icon, SELF-TEST label |
| Accent tint | `#DCEAE3` | active hide-mode toggle bg |
| Stumble amber | `#C28A45` | stumble underline + margin dot |
| Stumble border | `#E3C99F` on `#FAF2E4` | Stumble button outline / bg |
| Stumble ink | `#A8702A` | Stumble button label/icon |

**Typography** — UI font **Hanken Grotesk** (400/500/600/700); Arabic font **Amiri Quran** (400).
UI scale used: 16 / 14.5 / 12 / 11.5 / 11 / 10 sp. Arabic: body 22sp (line-height 1.92), surah name
25sp, basmala 20sp. The body uses `text-align: justify` with a centered last line.

**Spacing** — page padding 14/24/18dp; dock padding 12/16/10dp; top bar height 54dp; dock buttons 52dp.

**Radius** — dock buttons 15dp; toggle circle full; medallions/text are glyphs.

**Effects** — concealment = Gaussian blur ~4.5dp + faded ink. Reveal button shadow
`0 4 13 rgba(62,125,110,0.28)`; dock top shadow `0 -6 18 rgba(40,34,22,0.05)`; SELF-TEST dot pulses
opacity ~1.8s.

## Assets
- **Fonts (replace CDN with bundled assets in the app):**
  - UI: **Hanken Grotesk** — Google Fonts (OFL). Bundle weights 400/500/600/700.
  - Arabic: **Amiri Quran** — Google Fonts (OFL). NOTE: the product spec targets **KFGQPC Uthmanic
    Script HAFS** (and a planned upgrade to QPC page fonts for exact Madinah 15-line layout). Amiri
    Quran is used in this prototype because it renders Uthmani harakat/ligatures correctly in a
    browser. Use the real KFGQPC / QPC fonts in the app. Arabic shaping, harakat, and ligatures must
    render correctly; generous line spacing for harakat is required at every font size.
- **Icons:** simple line/outline glyphs — use the app's Material Symbols equivalents (chevron-left,
  visibility_off, flag for Stumble, visibility for Reveal). The "Aa" is set type, not an icon.
- **Quran text:** real Unicode (Uthmani) from the bundled Tanzil/QPC dataset — never an image. The
  numbered end-of-ayah medallion comes from the page font.

## Files
- `design/Mushaf.dc.html` — the Mushaf screen prototype (open in a browser, online).
- `design/support.js` — runtime required by the prototype (keep next to the HTML).
- `mushaf_preview.png` — rendered reference image (mid self-test state).
