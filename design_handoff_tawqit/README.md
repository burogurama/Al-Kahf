# Handoff: Alkahf — Tawqīt (audio→page sync)

## Overview
Alkahf is an offline-first Android hifz app. **Tawqīt** (Arabic for "timing") lets a user sync **any**
recitation audio to the mushaf so the page highlights **ayah-by-ayah** in time with the voice — the
same read-along highlight the Mushaf screen uses. It exists because the shipped per-ayah datasets only
cover a few reciters; Tawqīt unlocks *any* source — a downloaded reciter, or an **imported file** (e.g.
a user's own recording or a rip of a favorite reciter), including **continuous (waṣl)** recitations
that don't pause at ayah ends and so can't be auto-segmented.

The mechanic is **manual forced alignment, done by ear, at ayah granularity**: the user plays the audio
and taps a button at the end of each ayah; each tap records that ayah's end timestamp and advances the
highlight. The output is a **timing track** — an ordered list of ayah end-times bound to one audio
source — which the Mushaf and Loop Player then consume exactly like a shipped dataset.

> **Why ayah-level (not word-level):** the app's read-along highlight is per-ayah, so syncing is too.
> This keeps tagging to ~one tap per ayah (≈6 taps for al-Kahf 1–6) instead of hundreds per surah.

## Two screens in this handoff
1. **Tawqīt hub** (`design/Tawqit Hub.dc.html`) — entry point: lists existing timing tracks + a
   "New timing track" flow (source + portion selection) in a bottom sheet.
2. **Tawqīt tagging** (`design/Tawqit.dc.html`) — the live tagging screen where the user plays audio
   and marks ayah ends. **Interactive prototype** — play advances the clock/waveform; "Mark āyah end"
   advances the highlighted ayah and records a timestamp; undo steps back; NUDGE ALL shifts the offset.

## About the design files
These are **design references built in HTML — visual specs, NOT production code**. Recreate natively
(Kotlin, Jetpack Compose, Material 3; audio via Media3/ExoPlayer). Open in a browser online (loads
Google Fonts); keep `support.js` beside them. Previews: `tawqit_hub_preview.png`,
`tawqit_newtrack_sheet.png`, `tawqit_tagging_preview.png`.

## Platform
- Compose / Material 3, minSdk 26, phone portrait. HTML px maps 1:1 → dp/sp.
- Device frame, status bar, gesture pill = **mock chrome, do not build**.
- Reached from **Library → "Tawqīt · Audio Sync"**. Not a bottom-nav destination.
- Fully local: tracks stored in Room/SQLite. Importing reads a local audio file (Storage Access
  Framework picker); no network.
- **RTL:** all Arabic is RTL; the tagging waveform/markers read left→right as a timeline (audio time),
  which is fine — it's a media scrubber, not Quran text.
- Shared palette: screen #F5F0E6 · page/cards #FBF7ED/#FCFAF4 · accent #3E7D6E / deep #2F6055 ·
  amber #C28A45/#A8702A · ink #2A2620 family. UI **Hanken Grotesk**; Arabic **Amiri Quran** (prod:
  KFGQPC). **Do not use a music-note icon** anywhere — recitation is not music; use a **waveform**
  mark (5 vertical rounded bars, stroke #2F6055) for Tawqīt.

---

## Screen 0 — Library entry (the wiring)

Tawqīt is reached from the **Library** screen (`design/Library.dc.html`, preview
`library_entry_preview.png`). A dedicated section sits between **Downloads** and **Drill Presets**:

- Section caption: **"TAWQĪT · AUDIO SYNC"** (11sp/700/ls1.4/#B0A691).
- One tappable row (card #FCFAF4 / border #EAE3D4 / radius 18dp / pad 13/15dp): a 40dp rounded tile
  (#EBF1ED) holding the **waveform** icon (stroke #2F6055), title **"Sync recitation to the page"**
  (14sp/600/#2A2620), subtitle **"2 timing tracks · 1 in progress"** (12sp/500/#A39A8B — bind to the
  real track count/state), and a trailing chevron (#C2BAA9).
- The whole row navigates to the **Tawqīt hub** (in the prototype it's an `<a href="Tawqit Hub.dc.html">`;
  in production it's a normal navigation action to the hub destination).

So the full path is: **Library → Tawqīt · Audio Sync → hub → New timing track (source + portion) →
tagging → Save → timing track** consumed by Mushaf & Loop Player.

---

## Screen 1 — Tawqīt hub

### Top bar (54dp)
Back chevron (to Library) · centered title: Arabic "توقيت" (Amiri Quran 17sp) + "Tawqīt"
(16sp/700/#2A2620) over "Sync any recitation to the page" (11sp/500/#A39A8B) · trailing spacer.

### Explainer card (margin 6/18/0; #EBF1ED / border #D6E4DC; radius 18dp; pad 14/16dp)
A 34dp rounded tile (#DCEAE3) with the **waveform** icon + body copy (13sp/600/#2A4640, lh 1.45):
"Play any recitation and tap at each āyah end. Tawqīt builds the timing so the page highlights in
sync — even for continuous (waṣl) recordings."

### YOUR TIMING TRACKS (caption 11sp/700/ls1.4/#B0A691)
Track rows (radius 18dp, pad 13/15dp, gap 9dp): 42dp rounded-square status tile + title + subtitle +
chevron (#C2BAA9).
- **Complete:** tile #E2EDE8 + green check (#2F6055); title "Al-Kahf · 1–6" (14.5sp/700); subtitle
  "Mishary (imported) · 6 āyāt synced" (12sp/500/#A39A8B).
- **In progress:** tile #F0E8D6 + amber clock (#A8702A); subtitle "Ḥuṣarī · 12 of 40 · resume" — tapping
  resumes tagging where it left off.

### New timing track button (margin 16/18/18dp)
Full-width dashed button (1.5dp dashed #BFD2C9, bg #EFF4F1, radius 16dp): + glyph + "New timing track"
(14.5sp/700/#2F6055). Opens the sheet.

### New-track sheet (bottom sheet over a 34%-dark backdrop; tap backdrop = close)
Sheet bg #F5F0E6, top radius 26dp, pad 10/20/22dp, grab-handle (38×4dp #D8CFBB). **Do not rely on an
entrance animation for visibility** — the visible state must be the resting state (the prototype's
slide-up animation is omitted for that reason).
- Title "New timing track" (19sp/700) + "Choose the audio, then the portion to sync." (12.5sp/500/#A39A8B).
- **AUDIO SOURCE** (caption 10.5sp/700/ls1.2/#B0A691): two selectable rows (radius 15dp, pad 13/14dp).
  Selected row = border #CFE0D7 + bg #EBF1ED + filled 22dp radio (#3E7D6E dot); unselected = border
  #EAE3D4 + bg #FCFAF4 + empty radio (#CBC1AC ring).
  - **Import audio file** (40dp #DCEAE3 tile, download-tray icon): "MP3 or M4A from your device" →
    opens the system file picker (SAF).
  - **Downloaded reciter** (40dp circular #EFE7D6 avatar, Arabic initial): "Ḥuṣarī · already on device".
- **PORTION** (caption): a Sūrah selector (flex, "Al-Kahf", chevron-down → sūrah list) + an Āyāt range
  box (fixed 132dp: "1 – 10" with − / + steppers, #F0E8D6 chips).
- **Start button** (full-width, 54dp, #3E7D6E, white label + arrow): label is **"Choose file & start
  tagging"** when Import is selected, **"Start tagging"** for a downloaded reciter. → opens the tagging
  screen (import first triggers the file picker).

---

## Screen 2 — Tawqīt tagging (the core, interactive)

Layout: top bar → ayah list (scroll) → fixed tagging dock. Page area bg #FBF7ED.

### Top bar (54dp, bottom border #ECE5D6)
Close (✕, to hub) · centered "توقيت Tawqīt" + source subtitle ("Mishary (imported) · Al-Kahf 1–6",
11sp/500/#A39A8B) · trailing **Save** (14sp/700/#2F6055) — commits the timing track.

### Ayah list (scroll, pad 14/20/8dp)
Caption "TAGGING AYAH ENDS · Āyah N of M" (11sp/700/ls1.4/#B0A691, centered). Then, top→bottom:
- **Tagged ayat** (opacity 0.7): a row per done ayah — a green **time chip** ("0:12", 11sp/700/#2F6055
  on #E2EDE8, radius 7dp) + the ayah text (Amiri Quran 18sp, faded #9A917F) ending in its medallion.
- **Current ayah** — highlighted card (#EBF1ED / border #CFE0D7 / radius 18dp / pad 14/15dp): a pulsing
  7dp dot (#3E7D6E) + "NOW PLAYING · ĀYAH ‹n›" (10.5sp/700/ls1.2/#2F6055, nowrap), then the ayah
  (Amiri Quran 25sp, lh 1.95, #1F4F45) + green medallion. This is the ayah whose end you're about to mark.
- **Next ayah** (opacity 0.5): a dimmed preview row with a dashed "next" chip — lets the user anticipate.

### Tagging dock (#F8F3EA, top border #E7DFCF, pad 12/16/8dp, top shadow)
- **Waveform** (44dp, radius 11dp, bg #F1EADC): ~56 bars; bars before the playhead **#6FA395**, after
  **#DAD0BC**. Vertical **markers** (2dp #2F6055) at each recorded ayah-end time. **Playhead** (2dp
  #C2410C) at current position. (Production: render the real decoded waveform; markers from the track.)
- **Clock row:** "0:41 / 1:32" (13sp/700, tabular) · a **speed** chip "Tagging at 0.75×" (tap to cycle
  0.5/0.75/1×) — tagging slow makes taps less frantic; **stored timestamps are always at 1× scale**.
- **Transport row:** **Undo** (54dp, ↩ icon) steps the highlight back one ayah and removes that mark;
  **Play/Pause** (54dp, toggles); **Mark āyah end** (primary, flex, 56dp, #3E7D6E, white, leading dot)
  records the current time as this ayah's end and advances to the next.
- **Hint + offset:** "Tap when āyah ‹n› finishes" (11sp/500/#B0A691) · **NUDGE ALL** −/+ control with an
  offset readout ("+0.00s", ±0.05s steps). This applies a **global latency offset** to the whole track
  to compensate the user's constant reaction lag (~250ms) after tagging — far easier than tapping
  perfectly. Per-ayah fine-tuning (tap a tagged ayah to re-mark just it) is a reasonable later add.

### Interaction model (verified in the prototype)
Position audio at an ayah start → Play → when you hear ayah *n* finish, tap **Mark āyah end** (= end of
*n*, start of *n+1*); highlight advances. Undo rewinds one. Tag at reduced speed if helpful, then NUDGE
ALL to correct lag, then **Save**.

## State / data (local; no network except the import read)
- **Timing track:** id, audioSourceRef (downloaded reciter+surah, OR imported file URI), surah, ayah
  range, ordered **ayahEndTimesMs[]** (at 1× scale), globalOffsetMs, status (in-progress/complete),
  lastTaggedAyah (for resume).
- **Tagging session:** current ayah index, playhead ms, isPlaying, taggingSpeed, pending marks.
- Consumed by Mushaf + Loop Player: given playhead time → current ayah via the track (binary search),
  for the read-along highlight. A track with offset applied = `endTime + globalOffset`.

## Tokens
Colors: page #FBF7ED · dock #F8F3EA / border #E7DFCF · waveform bg #F1EADC, played #6FA395, unplayed
#DAD0BC, marker #2F6055, playhead #C2410C · current card #EBF1ED / border #CFE0D7 / ink #1F4F45 ·
time chip #2F6055 on #E2EDE8 · tagged text #9A917F · next preview #B3AA98 · accent #3E7D6E / #2F6055 ·
sheet selected #EBF1ED/#CFE0D7, radio #3E7D6E / #CBC1AC · controls border #E2DAC9 on #FBF7ED, chip #F0E8D6.
Type: UI **Hanken Grotesk** 400–700 (bundle, don't CDN); Arabic **Amiri Quran** (prod KFGQPC), ayah
25sp current / 18sp list. Radii: cards 15–18dp · dock buttons 15dp · waveform 11dp · sheet top 26dp.
Icons: Material Symbols equivalents — **waveform** for Tawqīt (never a music note), close, save, undo
(u-turn-left), play/pause, download-tray, chevrons, +/−.

## Files
- `design/Library.dc.html` — Library screen showing the Tawqīt entry (the wiring)
- `design/Tawqit Hub.dc.html` — hub + new-track sheet (interactive)
- `design/Tawqit.dc.html` — tagging screen (interactive)
- `design/support.js` — prototype runtime (keep beside all three)
- `library_entry_preview.png`, `tawqit_hub_preview.png`, `tawqit_newtrack_sheet.png`,
  `tawqit_tagging_preview.png` — references
