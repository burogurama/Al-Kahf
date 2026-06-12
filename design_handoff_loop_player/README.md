# Handoff: Alkahf — Loop Player (audio drilling)

## Overview
Alkahf is an offline-first Android hifz app. The **Loop Player** drills a passage by playing per-ayah
reciter audio on repeat. Three loop modes: **Single** (one ayah ×N), **Range** (ayat A→B ×N), and
**Chain** — cumulative chaining, the signature feature (1×N, 2×N, 1–2×M, 3×N, 1–3×M … building the
passage up). Plus a silent recite-back gap after each play, speed control, and word-by-word highlight
synced to audio. Mockup state: Ḥuṣarī, al-Kahf 1–5, Chain mode, repeating 1→3 (pass 2 of 5), ayah 2
sounding, ayah 4 next.

## About the design files
`design/Loop Player.dc.html` is a **design reference in HTML — a visual spec, NOT production code**.
Recreate natively in the Alkahf codebase (Kotlin, Jetpack Compose, Material 3). Open in a browser
online (Google Fonts); keep `support.js` beside it. `loop_player_preview.png` = rendered reference.

## Platform
- Compose / Material 3, minSdk 26, phone portrait. HTML px maps 1:1 → dp/sp.
- Device frame, status bar, gesture pill = **mock chrome, do not build**.
- Full-screen player, no bottom nav; fits one viewport (ayah card flexes to absorb slack).
- **Audio:** Media3/ExoPlayer + background service; media-style lock-screen/notification controls
  (play/pause, prev/next ayah). This UI binds to that session state.
- **RTL:** chain node-track runs right-to-left (ayah ١ rightmost); progress bar **fills from right**.

## Layout (top → bottom)
Screen bg **#F2ECDF**. Cards: bg **#FBF7ED**, 1dp border **#ECE3D0**.

### Top app bar (56dp, h-pad 12dp)
Leading 40dp chevron-**down** (stroke #5C5648 2dp). Center stack: "DRILL SESSION" (11sp/700/ls1.6/
#B0A691) over "Sūrat al-Kahf · Ḥuṣarī" (15sp/700/#2A2620). Trailing 40dp gear (stroke #5C5648
1.7dp) → preset editor.

### Chain card (radius 22dp, pad 16/16/14dp) — the signature
**(a) Loop controls.** Captions "LOOP MODE" / "ĀYĀT TO LOOP" (10sp/700/ls1/#B0A691), then:
- **Mode segmented control** (fills width): track #F0E8D6, radius 12dp, 3dp inset; segments
  **Single · Range · Chain** (12sp, 7dp v-pad, radius 9dp). Selected: bg #FFFDF8, text #2F6055/700,
  shadow `0 1 3 rgba(40,34,22,.10)`. Unselected: text #8C8475/600.
- **Range stepper** (124dp × 40dp): bg #FBF7ED, border #E2DAC9, radius 12dp; 38dp "−" (#8C8475),
  value "1–5" (13.5sp/700/#2A2620), 38dp "+" (#2F6055). **Sets how many ayat to loop**; node map
  follows. Pressed hit-area tint rgba(0,0,0,.04).

**(b) Node track** (row, centered, **direction RTL**). One node per ayah, linked by 26×3dp bars:
- In-chain nodes (1–3): 34dp #3E7D6E circles, Arabic-Indic numeral #FBFAF5 (Amiri Quran 16sp);
  links between them solid #3E7D6E.
- Sounding node (2): 40dp, 2dp ring in #FBF7ED, animated **sound ring** (box-shadow pulse
  rgba(62,125,110,.35)→transparent, ~1.4s).
- Queued nodes (4–5): 34dp transparent, 1.5dp dashed border #CBC1AC, numeral #B7AD99; incoming
  links dashed (#D7CDB8). Solid = built, pulsing = sounding, dashed = next to add.

**(c) Status row** (top margin 13dp): pill "Repeating 1 → 3" (12.5sp/600/#2F6055 on #E2EDE8,
radius 8dp, pad 4/9dp) · "pass **2** of 5" (12.5sp/500/#8C8475; the 2 = #2A2620/700) ·
right "next · +āyah 4" (12sp/600/#B0A691).

### Ayah card (radius 22dp, pad 16/18/18dp, top margin 11dp; flexes, content centered)
- Status row: 5dp dot #3E7D6E + "NOW RECITING · ĀYAH 2" (11sp/700/ls1.4/#2F6055) + 3-bar
  equalizer (2.5dp bars #6FA395, scaleY pulse ~0.7s staggered; static when paused).
- Ayah text: RTL centered, Amiri Quran 28sp, line-height 1.95. **Karaoke highlight** on the word
  being recited (شَدِيدًا): bg #D3E7DE, text #235247, radius 9dp, pad 2/8dp — driven by per-reciter
  word timestamps. Recited words #2A2620; upcoming words faded #BBB2A0; medallion ۝٢ #C8BFAD.

### Progress (RTL, top margin 14dp)
Track 5dp, radius 3dp, bg #E0D7C5; fill anchored **right**, #3E7D6E. Below (space-between, 7dp):
elapsed "0:21" (11.5sp/600/#A39A8B) · pill "↻ then 1.5× silent gap" (11sp/600/#C28A45 on #FAF2E4,
radius 7dp, pad 3/9dp — the recite-back cue) · remaining "0:07" (11.5sp/600/#5C5648).

### Transport (row, space-between, top margin 14dp)
1. **Speed** 50dp square, radius 14dp, border #E2DAC9, bg #FBF7ED, "1×" 14sp/700/#5C5648; cycles
   0.75×–1.5× (gap rescales too). Pressed bg #F0E9D8.
2. **Prev ayah** 52dp circle, transparent, glyph #3A352C 26dp.
3. **Play/Pause** 76dp circle #3E7D6E, white glyph 28dp, shadow `0 6 18 rgba(62,125,110,.32)`;
   pressed translateY 1dp. (Pause shown = playing.)
4. **Next ayah** 52dp circle, glyph #3A352C 26dp.
5. **Repeat** 50dp square (same chrome as Speed): loop icon + "3×" (10sp/700/#5C5648) = per-ayah ×N.

### Preset-params strip (row, gap 8dp, top margin 14dp)
Four equal cards (bg #FBF7ED, border #ECE3D0, radius 13dp, pad 9/6dp, centered): caption
(10sp/700/ls0.6/#B0A691) over value (14sp/700/#2A2620) — PER ĀYAH **3×** · PER CHAIN **5×** ·
GAP **1.5×** · SPEED **1.0×**. Tap any to edit; settings are savable presets (one-tap resume
from Home).

## Interactions
- Mode control → Single/Range/Chain; node map + labels follow.
- − / + stepper → passage length/range; map updates.
- Play/Pause, Prev/Next → mirrored to media notification + lock screen.
- Gear → reciter, range, counts, gap, speed; **save as preset**.
- Chain engine drives the map: sounding node, "Repeating A→B", "pass X of M", "next · +āyah"
  all reflect the sequencer step.

## State / data (local only; Room/SQLite)
- Sequencer: mode, range, sounding ayah, current span A→B, pass X of M, next-to-add.
- Playback: isPlaying, elapsed/remaining, speed, gap multiplier, per-reciter word timestamps.
- Preset: reciter, range, per-ayah ×N, per-chain ×M, gap, speed (named/savable).
- Audio files: downloaded per surah/reciter for offline use.

## Tokens
**Colors:** session bg #F2ECDF · card #FBF7ED · card border #ECE3D0 · control border #E2DAC9 ·
segmented track #F0E8D6 / selected #FFFDF8 · ink #2A2620 / #5C5648 / #8C8475 / #A39A8B / #B0A691 ·
transport glyph #3A352C · accent #3E7D6E / deep #2F6055 / light #6FA395 · highlight #D3E7DE on
#235247 · "Repeating" pill bg #E2EDE8 · dashed node #CBC1AC, dashed link #D7CDB8, queued numeral
#B7AD99 · upcoming word #BBB2A0 · medallion #C8BFAD · progress track #E0D7C5 · amber gap cue
#C28A45 on #FAF2E4 · on-accent #FBFAF5.

**Type:** UI **Hanken Grotesk** 400–700 (bundle, don't CDN); Arabic **Amiri Quran** — production
should use **KFGQPC Uthmanic Script HAFS** (Amiri is the browser stand-in). UI sizes 10–15sp;
Arabic 28sp / lh 1.95. Icons: Material Symbols equivalents (tune, skip, play/pause, repeat).

## Files
- `design/Loop Player.dc.html` — prototype (open online; keep support.js beside it)
- `design/support.js` — prototype runtime
- `loop_player_preview.png` — rendered reference
