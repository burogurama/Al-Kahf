# Alkahf — App Definition

## What it is

Alkahf is a native Android app for memorizing the Quran (hifz). It is a personal
memorization tool, not a general Quran reader: every screen and feature exists to
help the user learn new portions, drill them until solid, and keep older portions
from fading. It works fully offline, requires no account, and stores all progress
on-device.

## Target user

A Muslim actively memorizing the Quran — anywhere from a beginner working on
short surahs to a hafiz maintaining the full mushaf. They typically work in daily
sessions: a new portion to learn (sabaq), a recent portion to consolidate, and
older portions to review (murajaah). The app should fit that workflow rather than
impose a new one. The primary content language is Arabic (RTL); the UI language is
English (Arabic UI may come later).

## Core features

### 1. Reading with hide/reveal recall

The mushaf view doubles as a self-testing tool:

- Browse the Quran by surah, juz, or page.
- Hide mode: the text of an ayah (or a whole passage) is concealed; the user
  recites from memory and reveals word-by-word or ayah-by-ayah to check
  themselves. Tapping reveals the next word; a long-press or control reveals the
  whole ayah.
- The first word(s) of an ayah can optionally stay visible as a cue.
- Mistake marking: while self-testing, the user can mark a word or ayah as
  "stumbled" with one tap. Stumbles feed the review scheduler and show up as
  heatmap-style indicators on the page.

### 2. Audio recitation loops (drilling)

Automated repetition drills using per-ayah recordings of well-known reciters:

- Single-ayah loop: repeat one ayah N times or until stopped.
- Range loop: repeat a passage (ayah A to B) N times.
- Chaining (cumulative) mode: the traditional hifz method — ayah 1 ×N, ayah 2 ×N,
  then 1–2 ×M, ayah 3 ×N, then 1–3 ×M, and so on, building the passage up
  cumulatively. This is the signature feature; no mainstream app automates it well.
- Pause-after-ayah: a configurable silent gap after each play (e.g. 1.5× the ayah
  duration) so the user recites it back from memory before the next repetition.
- Playback speed control (0.75×–1.5×).
- Word-by-word highlighting in sync with the audio (timestamp data exists per
  reciter), which combines with hide mode: hear the ayah, reveal it word-by-word
  as it is recited.
- Loop settings (reciter, range, repeat counts, gap, speed) are savable presets
  so a user's drilling routine is one tap to start.
- Audio is downloaded per surah per reciter for offline use; a download manager
  shows what is stored locally.
- Playback continues in the background with media-style notification controls
  (drilling often happens with the screen off or while walking).

### 3. Spaced repetition review (murajaah)

- The user marks portions (ayah ranges, pages, or surahs) as memorized.
- The app schedules reviews with a spaced-repetition algorithm (SM-2 style):
  recently memorized portions come up often, solid ones stretch out to weeks.
- A daily review queue: "today you should review X, Y, Z." Each review is a
  hide/reveal self-test; the user grades themselves (e.g. perfect / hesitant /
  forgot), which sets the next interval. Stumble marks recorded during the test
  lower the grade automatically.
- The queue respects a configurable daily time/amount budget.

### 4. Progress tracking

- Per-ayah memorization state: not started / learning / memorized / strong.
- Visual progress: a mushaf-wide map (604 pages or 114 surahs) colored by state,
  plus percentage memorized by juz/surah/whole Quran.
- Streaks and daily activity (days practiced, ayat reviewed, time spent).
- All stats derive from actual app activity; nothing requires manual bookkeeping
  beyond marking portions memorized and grading reviews.

## The script (khatt) — important design constraint

- v1 renders real Unicode Uthmani text (Tanzil dataset) with the KFGQPC Uthmanic
  Script HAFS font. Arabic shaping, harakat, and ligatures must render correctly;
  the text is the product, so typography quality is paramount.
- The data model is page/line-aware from day one, with a planned upgrade to exact
  Madinah mushaf 15-line page layout (QPC page fonts). Memorizers rely heavily on
  visual page memory — ayah position on the page is a memorization cue — so the
  design should treat "the page" as a stable, sacred layout, not reflowable text.
- Design implications: generous line spacing for harakat, large adjustable font
  sizes, no decoration competing with the text, correct RTL behavior everywhere
  (pagination swipes right-to-left, progress bars fill right-to-left in the
  mushaf context).

## Design direction

- Calm, focused, distraction-free. The mushaf page is the hero; chrome disappears
  while reading/testing.
- Comfortable for long sessions and night use: light (cream/paper) and dark
  themes, both tuned for Arabic text legibility.
- Respectful, modern aesthetic — not skeuomorphic "wooden mushaf" clichés, but
  also not a sterile productivity app. Geometric restraint over ornament.
- One-handed operation matters: reveal/stumble/grade actions reachable by thumb
  during a self-test; loop controls operable without looking (and from the lock
  screen via media controls).
- Material 3 / Jetpack Compose component language as the base.

## Key screens (suggested)

1. Home / Today — today's plan: new portion, review queue, streak, quick-resume
   of the last loop preset.
2. Mushaf — the reading/testing view with hide-reveal modes and stumble marking.
3. Loop player — drilling session screen: current ayah text, repetition progress
   (e.g. "ayah 3 of 5, repeat 2 of 5, chain 1–3 next"), transport controls.
4. Review — the daily spaced-repetition queue and the self-grading flow.
5. Progress — the mushaf-wide memorization map and stats.
6. Library / Downloads — reciters, downloaded audio, presets.
7. Settings — fonts/sizes, themes, daily budget, algorithm tuning.

## Technical constraints (context for design)

- Native Android, Kotlin, Jetpack Compose, Material 3. Android 8.0+ (minSdk 26).
- Fully offline-first: Quran text bundled in the app; audio downloaded on demand;
  all user data local (Room/SQLite). No accounts, no analytics, no network except
  audio downloads.
- Audio via Media3/ExoPlayer with background playback service.
- Phone-first, portrait-first; tablet support is a later concern.

## Out of scope (v1)

- Translations and tafsir (it is a memorization tool; may come later as optional
  recall aids).
- Recording the user's own recitation / speech recognition correction.
- Social features, accounts, cloud sync, gamification beyond streaks.
- iOS.

## Open product questions

1. Should the chaining drill's default counts (×N per ayah, ×M per chain) follow
   a known method (e.g. 3/5/10), and which?
2. Hide mode default granularity: word-by-word reveal vs. ayah-by-ayah?
3. How prominent should streaks be? Useful motivation vs. unwanted gamification —
   the tone of this app suggests subtle.
4. Does v1 need multiple reciters, or one excellent default (e.g. Husary,
   the canonical teaching recitation) with more added later?
