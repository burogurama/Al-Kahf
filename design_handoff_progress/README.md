# Handoff: Alkahf — Progress (memorization map + stats)

## Overview
Alkahf is an offline-first Android hifz app. The **Progress** screen shows the whole-mushaf
memorization state at a glance: a 604-page heatmap grid (the hero), percentage memorized overall and
by juzʼ, and quiet activity stats (streak, āyāt reviewed, time). All stats derive from actual app
activity — no manual bookkeeping beyond marking portions memorized and grading reviews.

## About the design files
`design/Progress.dc.html` is a **design reference built in HTML — a visual spec, NOT production
code**. Recreate natively in the Alkahf codebase (Kotlin, Jetpack Compose, Material 3). Open in a
browser online (loads Google Fonts); keep `support.js` beside it. `progress_preview.png` = rendered
reference. The page grid in the prototype is generated from a per-page state model — mirror that
with a real Room query.

## Platform
- Compose / Material 3, minSdk 26, phone portrait. HTML px maps 1:1 → dp/sp.
- Device frame, status bar, gesture pill = **mock chrome, do not build**.
- Top-level destination: Scaffold with the app's bottom NavigationBar (**Progress active**).
- Screen bg **#F5F0E6**; cards **#FBF7ED**, 1dp border **#ECE3D0**.

## Layout

### Header (pad 10/22/4dp, row, space-between, bottom-aligned)
- Left: "Progress" (31sp/700/#2A2620/ls −0.5) over "562 āyāt held in memory"
  (13sp/500/#A39A8B, nowrap — bind to real count).
- Right: "9.0%" (28sp/700/**#3E7D6E**, the % sign 16sp) over "OF THE QURAN"
  (11sp/600/#A39A8B/ls 0.3). Percent = memorized āyāt / 6,236.

### Mushaf map card (radius 24dp, pad 16/16/13dp) — the hero
- Header row: "THE MUSHAF · 604 PAGES" (11sp/700/ls1.4/#8C8475) · "44 memorized"
  (11.5sp/600/#A39A8B). Both nowrap.
- **Grid: one square per mushaf page (604 total).** Squares ~10dp, radius 2.5dp, 2dp gap,
  flex-wrap with **direction RTL** — page ١ at top-RIGHT, flowing right-to-left then down,
  matching how the physical mushaf reads. (Madinah print = 604 pages; the data model is
  page-aware from day one.)
- Square color = that page's rolled-up state (see Rollup rule):
  Strong **#2F6055** · Memorized **#6FA395** · Learning **#D8B45A** · Not started **#EAE2D0**.
- Legend row (top margin 12dp, gap 13dp): 9dp swatch + label (11sp/600/#8C8475) for each state.
- **Tap a square → open that page in the Mushaf view.** Squares are small; use a pointer-input
  handler on the grid that maps touch position → page index (don't try to make each square a
  44dp target).

### Rollup rule — HONESTY (product decision, confirmed)
**A page displays the WEAKEST state among its ayat.** A page that is 90% strong but has one
learning/weak ayah shows **amber (Learning)** — not green. The map must push the user toward
weak spots rather than flatter them; this matches the app's stumble-driven honesty (stumbles
auto-lower review grades). Precedence: any not-started ayah on a touched page → Learning;
any learning ayah → Learning; all memorized but any not yet strong → Memorized; all strong →
Strong. Pages with no touched ayat → Not started.

### By-juzʼ card (radius 22dp, pad 15/18dp)
- Caption "BY JUZʼ" (11sp/700/ls1.4/#8C8475, 12dp below).
- One row per juzʼ the user has touched (gap 11dp): name "Juzʼ 30" (13sp/600/#4A453C, fixed
  64dp) · progress track (7dp tall, radius 4dp, bg #EAE2D0) with fill **anchored RIGHT**
  (RTL — mushaf context) · value "100%" (12.5sp/700, fixed 38dp right-aligned).
- Fill color by status: complete **#2F6055** (value text matches), in progress **#3E7D6E**,
  mostly-learning **#D8B45A**. Sort: highest progress first. Untouched juzʼ are omitted
  (tap-through to a full 30-juzʼ list is a later concern).

### Activity row (3 equal tiles, gap 10dp; radius 20dp, pad 13/16dp)
Value (22sp/700/#2A2620) over label (11.5sp/600/#A39A8B):
**12** day streak · **142** āyāt this week · **3.2h** time spent.
Streaks stay this quiet by design — no flames, no fanfare.

### Bottom navigation
Same component as Home (bg #F8F3EA, top border #EAE3D4; active pill 60×30dp #DCEAE3, icon/label
#2F6055/700; inactive #8C8475). **Progress** (bar-chart icon) is active.

## State / data (local only, Room/SQLite)
- Per-ayah memorization state (not_started / learning / memorized / strong) → rolled up per page
  (weakest-state rule) and per juzʼ (% of ayat memorized+strong).
- Overall: memorized āyāt count, % of 6,236; memorized page count.
- Activity: current streak, āyāt reviewed in the trailing week, total practice time.
- All values shown are bindings; the prototype's numbers (562 āyāt, 9.0%, 44 pages, juzʼ
  30/29/15) are sample data for a "two juzʼ + working on al-Kahf" user.

## Tokens
Colors: screen #F5F0E6 · card #FBF7ED / border #ECE3D0 · ink #2A2620 / #4A453C / #8C8475 /
#A39A8B · accent #3E7D6E / deep #2F6055 / light #6FA395 · learning #D8B45A · empty #EAE2D0 ·
nav #F8F3EA / #EAE3D4 / pill #DCEAE3.
Type: UI **Hanken Grotesk** 400–700 (bundle, don't CDN). UI sizes 11–31sp. No Arabic text on
this screen (Arabic-Indic digits optional for page numbers in a future page-detail view).
Radii: cards 20–24dp · map squares 2.5dp · tracks 4dp. Icons: Material Symbols equivalents.

## Files
- `design/Progress.dc.html` — prototype (open online; keep support.js beside it)
- `design/support.js` — prototype runtime
- `progress_preview.png` — rendered reference
