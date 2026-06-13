# Handoff: Alkahf — Library / Downloads

## Overview
Alkahf is an offline-first Android hifz app. The **Library** is the provisioning surface: it manages
the raw materials the practice screens consume — **reciters** (which voice), **downloaded audio**
(what plays offline) and **drill presets** (saved loop routines). Users visit it occasionally — add a
voice, free up space, tweak a routine — not daily. It is a top-level destination (bottom nav,
**Library active**).

## About the design files
`design/Library.dc.html` is a **design reference built in HTML — a visual spec, NOT production code**.
Recreate natively in the Alkahf codebase (Kotlin, Jetpack Compose, Material 3; audio downloads via the
app's download manager + Media3). Open in a browser online (loads Google Fonts); keep `support.js`
beside it. `library_preview.png` = rendered reference (top of screen; Downloads & Presets sit below
the fold — scroll to see them).

## Platform
- Compose / Material 3, minSdk 26, phone portrait. HTML px maps 1:1 → dp/sp.
- Device frame, status bar, gesture pill = **mock chrome, do not build**.
- Scaffold + bottom NavigationBar (**Library active**). Single scrolling column.
- Screen bg **#F5F0E6**; cards **#FCFAF4** (neutral) / **#EBF1ED** (active/green), 1dp borders
  **#EAE3D4** / **#D6E4DC**.
- Downloads are per-surah, per-reciter; all audio stored locally for offline use. Network is used
  ONLY here, when the user chooses to download.

## Layout (single scroll; sections top → bottom)

### Header (pad 10/22/4dp)
"Library" (31sp/700/#2A2620/ls −0.5) over "Reciters, downloads & presets" (13sp/500/#A39A8B).

### Storage meter (margin 12/18/0; radius 18dp, pad 13/16dp)
Row: "Offline audio" (12.5sp/600/#4A453C) · "1.4 GB of 64 GB" (12sp/600/#A39A8B, nowrap). Below: a
6dp track (radius 3dp, bg #EAE2D0) with a #3E7D6E fill = fraction of device storage used by audio.

### RECITERS section (caption 11sp/700/ls1.4/#B0A691)
Reciter rows (radius 18dp, pad 13/15dp, gap 9dp), each: a 46dp circular avatar with the reciter's
Arabic initial (Amiri Quran 19sp), a title + subtitle, and a trailing control.
- **Active reciter** (green card #EBF1ED / border #D6E4DC): avatar bg #3E7D6E, initial #FBFAF5;
  title #2A2620 15sp/700; subtitle "Murattal · teaching recitation" 12sp/500/#6E8079; trailing
  **ACTIVE** pill (11sp/700/#2F6055 on #DCEAE3, radius 999).
- **Other reciters** (neutral card #FCFAF4 / border #EAE3D4): avatar bg #EFE7D6, initial #8C8475;
  subtitle shows download state + size ("8 of 114 sūrahs · 96 MB", "Not downloaded · ~640 MB",
  12sp/500/#A39A8B); trailing 36dp circular **download** button (1dp border #DCD3C0, tray-arrow
  icon stroke #5C5648). Tapping a reciter makes it active; the download button fetches its audio.

### DOWNLOADS section (caption row: "DOWNLOADS · ḤUṢARĪ" + "Manage" link 11.5sp/600/#2F6055)
Scoped to the active reciter. Item rows (radius 18dp, pad 12/15dp, gap 9dp), each: a 40dp rounded-
square (radius 11dp) leading tile, a title + size subtitle, a trailing action.
- **In progress:** tile bg #F0E8D6 with an amber download glyph (#A8702A); right side shows a percent
  (11.5sp/600/#A8702A) over a 5dp progress track (bg #EAE2D0, fill **#C28A45**). e.g. "Sūrat al-Kahf
  · 62%".
- **Completed:** tile bg #E2EDE8 with a green check (#2F6055); subtitle "Downloaded · 214 MB"; trailing
  **delete** (trash) icon stroke #C2BAA9. e.g. "Juzʼ 30 · 37 sūrahs", "Al-Fātiḥah · Al-Baqarah".

### DRILL PRESETS section (caption 11sp/700/ls1.4/#B0A691)
Saved loop routines (the Loop Player's savable settings). Rows (radius 18dp, pad 13/15dp, gap 9dp):
a 40dp dark tile (#2A2620, radius 11dp) with a play triangle (#F5F0E6), a title (14sp/600/#2A2620)
+ a settings summary subtitle ("Ḥuṣarī · 1.5× gap · 1.0× speed", 12sp/500/#A39A8B, nowrap).
- The active default carries a **DEFAULT** pill (#2F6055 on #DCEAE3) — this is what Home's "Resume
  drill" launches; others show a chevron (#C2BAA9).
- Last item: a dashed **"New preset"** button (1.5dp dashed #D8CFBB, radius 18dp, + glyph + label
  13.5sp/600/#8C8475).

## Behavior & connections
- **Active reciter** here → the voice used by the Loop Player and Review.
- **Downloaded audio** here → what those screens can play offline (per surah, per reciter).
- **Default preset** here → Home's one-tap "Resume drill" and the Loop Player's starting config.
- Tap a reciter row → make active. Tap download button → start fetching that reciter's audio (rows in
  Downloads update live). "Manage" → bulk download management. Trash → delete a downloaded bundle
  (storage meter updates). Tap a preset → open/edit it; "New preset" → create one.
- **v1 reciter scope:** the product spec leans toward one excellent default (Ḥuṣarī) with more added
  later. This screen shows the multi-reciter picker so the structure is right; if v1 ships single-
  reciter, Reciters collapses to one row and the picker arrives later.

## State / data (local; network only for downloads)
- Reciters: id, display name, Arabic initial, style tag, active flag, per-reciter download status
  (sūrahs downloaded / total, bytes).
- Downloads: per (reciter, surah/juzʼ) state — queued / downloading (% + bytes) / complete; total
  bytes used vs device free space (storage meter).
- Presets: id, name, mode (single/range/chain), reciter, range, repeat counts (×N/×M), gap, speed,
  isDefault.

## Tokens
Colors: screen #F5F0E6 · card #FCFAF4 / border #EAE3D4 · active card #EBF1ED / border #D6E4DC ·
ink #2A2620 / #4A453C / #6E8079 / #8C8475 / #A39A8B / #B0A691 · accent #3E7D6E / deep #2F6055 ·
accent tile #E2EDE8 / pill #DCEAE3 · neutral avatar #EFE7D6 · amber (in-progress) #C28A45 / #A8702A
on #F0E8D6 · empty track #EAE2D0 · dark tile #2A2620 · dashed #D8CFBB · trash/chevron #C2BAA9.
Type: UI **Hanken Grotesk** 400–700 (bundle, don't CDN); reciter initials **Amiri Quran** (prod:
KFGQPC). UI 11–31sp. Radii: reciter/download/preset rows 18dp · tiles 11dp · storage card 18dp ·
pills 999dp. Icons: Material Symbols equivalents (download, check, delete, chevron, add, play).

## Files
- `design/Library.dc.html` — prototype (open online; keep support.js beside it)
- `design/support.js` — prototype runtime
- `library_preview.png` — rendered reference (scroll for Downloads & Presets)
