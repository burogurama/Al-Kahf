# Handoff: Alkahf — Review (daily murājaʿah + self-grading)

## Overview
Alkahf is an offline-first Android hifz app. The **Review** screen runs the daily spaced-repetition
queue (murājaʿah): the SM-2-style scheduler picks portions due today; each is a hide/reveal
**self-test** the user grades themselves on (**Forgot / Hesitant / Perfect**), which sets the next
interval. Stumbles recorded during the test **lower the grade automatically**. Advancing to the next
portion is **manual** — an explicit "Next portion" button after grading (user controls pacing).

Mockup state: portion 3 of 5 (Sūrat an-Nās 1–6), passage revealed after recall with 1 stumble
recorded, grading dock active. The prototype is interactive: tapping a grade switches the dock to a
graded/confirm state; "Next portion" returns it (simulating the next item).

## About the design files
`design/Review.dc.html` is a **design reference built in HTML — a visual spec, NOT production code**.
Recreate natively in the Alkahf codebase (Kotlin, Jetpack Compose, Material 3). Open in a browser
online (loads Google Fonts); keep `support.js` beside it. `review_preview.png` = rendered reference.

## Platform
- Compose / Material 3, minSdk 26, phone portrait. HTML px maps 1:1 → dp/sp.
- Device frame, status bar, gesture pill = **mock chrome, do not build**.
- Full-screen flow (no bottom nav): top bar → queue strip → passage card (flexes) → grading dock.
- All data local (Room/SQLite); the scheduler runs on-device.
- **RTL:** Arabic passage is RTL, justified, last line centered. English chrome is LTR.

## Layout
Screen bg **#F5F0E6**. Card bg **#FBF7ED**, 1dp border **#ECE3D0**.

### Top app bar (54dp, h-pad 10dp)
Leading 40dp chevron-left (stroke #5C5648 2dp). Center stack: "Daily Review" (16sp/700/#2A2620)
over "Murājaʿah · ≈ 5 min left" (11sp/500/#A39A8B — bind to remaining budget). Trailing 40dp spacer.

### Queue strip (pad 2/22/12dp)
- Row of equal segments (gap 6dp), one per portion due today: 6dp tall, radius 3dp.
  Graded = **#3E7D6E**; current = **#D8B45A**; waiting = **#E4DBC8**.
- Below (space-between, 7dp): "Portion 3 of 5 · An-Nās" (11.5sp/600/#8C8475) and "2 graded"
  (11.5sp/600/#A39A8B). All labels nowrap.

### Passage card (flexes; margin 0/18dp; radius 24dp; pad 20/20/16dp)
- Header row: "SŪRAT AN-NĀS · 1–6" (11sp/700/ls1.4/#8C8475) · "1 STUMBLE" badge
  (11sp/700/#A8702A on #FAF2E4, radius 999, pad 4/10dp; hidden when none).
- Arabic passage, vertically centered (flex spacers above/below): Amiri Quran 23sp, lh 1.95,
  #2A2620, RTL justified, last line centered. Ayah medallions ۝ + Arabic-Indic number #3E7D6E.
- **Stumble marks carried over from the test:** amber underline #C28A45 (2dp, offset ~9dp to clear
  harakat) on the stumbled word (ٱلْوَسْوَاسِ) + 6dp amber dot pinned top-left of that ayah's medallion.
- Footer (top border #ECE5D6, pad-top 12dp, centered): eye icon (stroke #A39A8B 1.8dp) +
  "Revealed after recall · 1 stumble recorded" (12sp/500/#A39A8B).

### Grading dock (pad 14/18/10dp) — two states
**State A — grading (default).** "How was your recall?" (12.5sp/600/#6E665A, centered, 11dp below),
then three equal buttons (row, gap 9dp, height 64dp, radius 16dp), each a column of label (14.5sp/700)
over its next-interval hint (11sp/600, nowrap):
- **Forgot** — border 1.5dp #DEC4BC, bg #F8ECE8, label #A05544, hint "again tomorrow" #C09183.
  Pressed bg #F1E0DA.
- **Hesitant** — border 1.5dp #E3C99F, bg #FAF2E4, label #A8702A, hint "in 3 days" #C9A36B.
  Pressed bg #F3E7D2.
- **Perfect** (primary) — bg #3E7D6E, label #FBFAF5, hint "in 2 weeks" #BCD6CC, shadow
  `0 4 13 rgba(62,125,110,.26)`. Pressed translateY 1dp.
Below: "Stumbles lower the grade automatically" (11sp/500/#B0A691, centered, 9dp above).
**The hints must show the real intervals the scheduler would assign** — they are the honest
consequence of each grade, not decoration.

**State B — graded (after a tap).** Replaces State A:
- Confirmation row (centered, gap 8dp): green check (stroke #3E7D6E 2.4dp) + "Graded Hesitant ·
  next review in 3 days" (13sp/600/#4A453C) + "Change" link (12sp/600/#8C8475, underlined) that
  returns to State A.
- Full-width **"Next portion · Al-Falaq →"** button: height 56dp, radius 16dp, bg #3E7D6E, label
  15.5sp/600 #FBFAF5, shadow `0 4 13 rgba(62,125,110,.28)`. **Manual advance only** — nothing
  auto-advances.
- Below: "2 portions left · ≈ 3 min" (11sp/500/#B0A691, centered).

## Flow & behavior
1. Entered from Home ("Start review") with the day's queue from the SM-2 scheduler (respects the
   configurable daily time/amount budget shown as "≈ X min left").
2. Each portion arrives **concealed** (same hide mode as the Mushaf screen); the user recites from
   memory, reveals (tap word / long-press ayah / reveal all), marking **stumbles** as they go.
3. Once revealed, the grading dock appears (State A). Tapping a grade → State B.
4. **Grade → interval (SM-2 style):** Forgot → tomorrow (reset), Hesitant → short interval (~3 days),
   Perfect → interval grows (weeks for solid portions). **Stumbles recorded during the test cap the
   grade automatically** (e.g. a stumbled "Perfect" is treated as Hesitant); surface this honestly.
5. "Next portion" (manual) loads the next item; the queue strip and counts update. After the last
   portion: session summary → streak/stats update → back to Home.
6. "Change" lets the user re-grade before advancing; once advanced, the grade is committed.

## State / data (local only)
- Day queue: ordered portions due (surah/ayah ranges), each with text, prior interval, stumble
  history; remaining-time estimate.
- Per-portion session state: concealed/revealed progress, stumbles recorded (word + ayah).
- Committed grades → scheduler updates (next due date per portion).
- Budget config (daily minutes/amount) for the "min left" readouts.

## Tokens
Colors: screen #F5F0E6 · card #FBF7ED / border #ECE3D0 · ink #2A2620 / #4A453C / #6E665A /
#8C8475 / #A39A8B / #B0A691 · accent #3E7D6E (graded segments, Perfect, medallions, check) ·
current segment #D8B45A · waiting segment #E4DBC8 · stumble amber #C28A45; badge #A8702A on
#FAF2E4 · Forgot palette #A05544 / #C09183 / #DEC4BC / #F8ECE8 · Hesitant palette #A8702A /
#C9A36B / #E3C99F / #FAF2E4 · on-accent #FBFAF5 / hint #BCD6CC.
Type: UI **Hanken Grotesk** 400–700 (bundle, don't CDN); Arabic **Amiri Quran** in the prototype —
production uses **KFGQPC Uthmanic Script HAFS**. Arabic 23sp / lh 1.95; UI 11–16sp.
Radii: card 24dp · grade buttons/Next 16dp · badges 999dp. Icons: Material Symbols equivalents.

## Files
- `design/Review.dc.html` — interactive prototype (open online; keep support.js beside it)
- `design/support.js` — prototype runtime
- `review_preview.png` — rendered reference (grading state)
