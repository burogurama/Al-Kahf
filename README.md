# Alkahf

An offline-first Android app for memorizing the Qur'an (ḥifẓ).

Alkahf is a personal memorization tool, not a general Qur'an reader: every screen
exists to help you learn a new portion (sabaq), drill it until solid, and keep
older portions from fading. It works fully offline, needs no account, and keeps
all progress on-device.

## Features

- **Mushaf with hide/reveal recall** — browse by sūrah, juz, or page; a self-test
  mode conceals the text so you recite from memory and reveal word-by-word or
  āyah-by-āyah. Tap to mark a stumble; stumbles feed the review scheduler.
- **Sabaq** — set a sūrah (or range) to learn; the app splits it into sections,
  tracks per-āyah state, and advances as you memorize.
- **Drills (loop player)** — automated repetition: single-āyah and cumulative
  chain modes, recite-back gaps, with configurable reciter, speed, and counts.
  Each sabaq gets an auto-managed drill on the home screen.
- **Istiḥḍār (review)** — an SM-2 spaced-repetition scheduler surfaces due
  portions daily, drawn from what you've memorized.
- **Two riwāyāt — Ḥafṣ and Warsh** — switch the reading app-wide in Settings, or
  peek at the other reading temporarily from within the Mushaf. Authentic KFGQPC
  text and fonts for each, with a Warsh↔Ḥafṣ verse mapping so per-āyah audio lines
  up across the two countings.
- **Reciters** — built-in voices streamed/cached from everyayah.com, plus your own
  imported per-sūrah audio aligned with **Tawqīt** timing. Each reciter is tagged
  by riwāyah.
- **Daily reminders** — schedule reminders to do your sabaq, each with a Listen
  shortcut that opens the drill already reciting.
- **Bilingual & themed** — English and Arabic (RTL) UI; light, dark, and system
  themes.

See [`APP_DEFINITION.md`](APP_DEFINITION.md) for the full product spec.

## Tech stack

- **Kotlin**, **Jetpack Compose**, **Material 3**
- **Room** — read-only `quran.db` / `quran_warsh.db` (bundled assets) and a
  writable `user.db` for progress
- **Media3 (ExoPlayer)** for audio
- minSdk 26 · targetSdk/compileSdk 35

## Project layout

```
app/src/main/
  assets/           quran.db, quran_warsh.db (bundled Qur'an text)
  res/font/         KFGQPC Hafs/Warsh + UI fonts
  java/app/alkahf/
    ui/             Compose screens (mushaf, loop, review, library, …)
    data/           repository, Room entities/DAOs, audio store
    notify/         daily reminder scheduling + notifications
tools/              build_quran_db.py and source Qur'an data
```

## Building

A standard Android/Gradle project. With the Android SDK installed (and
`local.properties` pointing at it):

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # build and install on a connected device/emulator
```

Built APKs land in `app/build/outputs/apk/`.

## Data & attribution

- Qur'an text and the Uthmanic fonts (Ḥafṣ and Warsh) are from the **King Fahd
  Glorious Qur'an Printing Complex (KFGQPC)**.
- Per-āyah recitation audio is fetched on demand from **everyayah.com** and cached
  for offline use.

Please respect the licensing terms of these sources when redistributing.
