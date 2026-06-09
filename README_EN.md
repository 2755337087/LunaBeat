# LunaBeat

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

- LunaBeat hopes to bring a cleaner, more immersive local music experience, like music at night, between melody and lyrics.
---
- LunaBeat is a mobile player focused on local music and lyrics, combining music playback, lyric editing, and word-level lyric timing in one app.
- It supports local audio playback, word-level lyric creation including duets and background vocals, lyric preview and editing, export to `.lrc` / `.elrc` / `.ttml`, and batch processing for audio metadata and lyrics.
- From import, editing, and playback to export, LunaBeat aims to provide a more complete and immersive local music and lyrics experience.

## Why LunaBeat

- More than a lyric editor, LunaBeat can also be used as a long-term local music player.
- Create professional word-level timed lyrics directly on mobile.
- Import and export multiple formats to fit common lyric workflows.
- Integrated music library and metadata tools reduce the need to switch between apps.
- Built-in batch processing is suitable for organizing large music collections.

## Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/mainPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/searchPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricEditPage.jpg" width="30%" />
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricPrePage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricPrePage2.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/musicEditPage.jpg" width="30%" />
</p>

## Feature Overview

### 1. Lyric Timing and Editing
- Three-key timing workflow: Start / Continue / End
- Rewind / fast-forward, play / pause, playback speed from 0.25x to 3x, and jump to a specific time
- Undo / redo for timing, lyric, and line operations
- Tap the playback time to enter follow mode and locate the selected lyric
- Tap a lyric unit to select it, and double-tap to jump to the current lyric start time and play

### 2. Import
- Plain text lyrics
- LRC, including line-level and word-level lyrics
- Enhanced LRC (ELRC)
- TTML
- Streaming lyric search
- Audio file import

### 3. Export
- LRC, including line-level and word-level lyrics
- TTML, with support for duets and background vocals

### 4. Preview and Batch Processing
- Preview word-level lyric playback
- One-click word segmentation
- Timestamp shifting
- One-click lyric merging
- Traditional Chinese to Simplified Chinese conversion
- One-click empty line removal

### 5. Lyric Structure Operations (Long Press)
- Lyric operations: edit, add, split, merge, set time, delete
- Line operations: add, merge, split, move, add/edit translation, delete

### 6. Player
- Local music playback with play / pause, previous / next, and progress seeking
- Sequential playback, shuffle, and single repeat
- Now-playing queue management with jump, remove, and drag-to-reorder
- Music library mini player and full player integration
- Background playback, system media notifications, and media key controls
- Sleep timer, with countdown stop or stop after the current song finishes
- Lyric preview integration for checking word-level lyric effects during playback
- Supports common local audio formats, with playback compatibility handling for ALAC

### 7. Music Library
- Scan and filter local audio files
- Playlists, favorite songs, and five-star songs
- Playlist import / export
- Add to playback queue / play next
- Batch match tags, including cover art, title, artist, and more
- Batch match lyrics, including word-level lyrics, line-level lyrics, and translations
- Batch rename files
- Batch edit tags

### 8. Settings
- Custom dark mode
- Custom rewind / fast-forward duration for lyric timing
- Music library settings
- Playback bar style and background
- Lyric display, desktop lyrics, status bar lyrics, and car Bluetooth lyrics
- Audio metadata cover size settings
- Apple Music metadata region settings

## 5-Minute Quick Start (Recommended Order)

### Step 1: Import Audio and Lyrics
1. Import the audio file first.
2. Then choose the lyric import method based on the lyric source: plain text / LRC / ELRC / TTML / search.

### Step 2: Play Local Music
1. Open the music library and scan local audio.
2. Tap a song to start playback. The mini player at the bottom shows the current playback state.
3. Open the full player to switch playback modes, manage the queue, open lyric preview, or set a sleep timer.

### Step 3: Preprocess Lyrics
1. Use one-click word segmentation to create word-level units.
2. If needed, use one-click empty line removal, lyric merging, or Traditional-to-Simplified Chinese conversion.
3. If existing timestamps are globally offset, use timestamp shifting before fine-tuning.

### Step 4: Start Timing Lyrics (Core Three-Key Workflow)
- Start: Set the start time of the current unit without jumping.
- Continue: Set the end time of the current unit, automatically move to the next unit, and write the next unit's start time.
- End: Set the end time of the current unit and move to the next unit, without writing the next unit's start time.

Recommended rhythm: `Start -> Continue -> Continue -> ... -> End`, repeated by sentence.

### Step 5: Fine-Tune Timing and Text
1. Enable follow mode so playback automatically locates the current unit.
2. Double-tap a lyric to quickly jump back to its start time and review it.
3. Use undo / redo to quickly compare before and after changes.
4. Long-press lyrics or lines to split, merge, move, translate, or adjust structure.

### Step 6: Set Duets and Translations (Optional)
- Duet types: left duet (main vocal), right duet (secondary vocal), background (background vocals).
- Translations can be imported or edited manually.
- Note: Duet types only take effect in the TTML workflow.

### Step 7: Preview and Export
1. Check the word-level playback effect on the lyric preview page.
2. Export based on your intended use:
   - Use `LRC` first for general players.
   - Use `TTML` first when duet / background vocal information is needed.

## Lyric Timing Interface

- Columns: start time / lyrics / end time / translation
- Locate: jump to the selected lyric
- Time jump: tap the time in the timing area to jump to a custom time
- Title tap: get the audio path

## Recommended Music Library Workflow

1. Scan local audio, then review and filter target songs.
2. Organize frequent listening by album, playlist, favorites, or five-star songs.
3. Play songs and manage the current queue; set play next or a sleep timer when needed.
4. Batch match tags to complete metadata such as cover art, title, and artist.
5. Batch match lyrics to complete word-level lyrics, line-level lyrics, and translations.
6. Batch rename and batch edit tags to finish organizing your library.

## FAQ

### 1. When should I export LRC, and when should I export TTML?
- Choose LRC if you only need regular lyric display.
- Choose TTML if you need duets, background vocals, or a more complete structure.

### 2. Why do I not see the duet effect after setting duet types?
Duet types are only supported in TTML. Please confirm that you are using the TTML workflow and exporting as TTML.

### 3. The lyrics are globally early or late. How can I fix them quickly?
Use timestamp shifting for global alignment first, then return to the timing page for local fine-tuning.

### 4. What is the most common mistake for beginners?
Usually, it is timing the lyrics before word segmentation. Complete word segmentation and text cleanup before starting timing.

## Special Thanks

Inspired by:
- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [Music Tag](https://www.cnblogs.com/vinlxc/p/11932130.html)

Technical support:

- [TagLib](https://github.com/taglib/taglib)
- [Lyricon](https://github.com/tomakino/lyricon)
- [SongSync](https://github.com/Lambada10/SongSync)
- [LDDC](https://github.com/chenmozhijin/LDDC)
- [OpenCC](https://github.com/BYVoid/OpenCC)
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)
- [163Music2Tag](https://gitee.com/Wangs-official/163Music2Tag)
- [TagLib (Kyant0)](https://github.com/Kyant0/taglib)
- [uCrop](https://github.com/Yalantis/uCrop)
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui)
- [EdgeTranslucent](https://github.com/qinci/EdgeTranslucent)
- [lottie-android](https://github.com/airbnb/lottie-android)
- [ICU Transliterator](https://unicode-org.github.io/icu/userguide/transforms/general/)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)

## Community and Feedback

- [Join the LunaBeat Telegram group](https://t.me/+qXs6mjKqwhw3Zjll)
- [Join the LunaBeat QQ group](https://qm.qq.com/q/N0fBvuWKOY)  Group ID: 964680520
