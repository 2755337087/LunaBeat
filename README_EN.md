# LunaBeat

- LunaBeat aims to be like music in the night — delivering a cleaner, more immersive local music experience through the harmony of melody and lyrics.

---

- Starting from version 2.0.0, LyricBox has officially been renamed to LunaBeat.
- This is not only a name change, but also a new stage for the app as it evolves from a local lyric editor into an integrated music playback and lyrics experience.
- Thank you for your continued support. LunaBeat will keep focusing on local music, dynamic lyrics, and a more immersive player experience.
- The story of LyricBox continues — now under a new name.

---

- LunaBeat is a mobile player focused on local music and lyrics, combining music playback, lyric editing, and word-level lyric timing in one app.
- It supports local audio playback, word-level lyric creation including duets and backing vocals, lyric preview and editing, export to `.lrc` / `.elrc` / `.ttml`, and batch processing for audio metadata and lyrics.
- From importing and editing to playback and export, LunaBeat is designed to deliver a more complete and immersive local music and lyrics experience.

## Why LunaBeat

- Create professional word-level timed lyrics directly on mobile.
- Import and export multiple lyric formats to fit common lyric workflows.
- Integrate a music library and metadata tools to reduce switching between apps.
- Built-in batch processing, ideal for organizing large music collections.

## Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/mainPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/musicLibraryPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/searchPage.jpg" width="30%" />
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricEditPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricPrePage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/musicEditPage.jpg" width="30%" />
</p>

## Feature Overview

### 1. Lyric Timing and Editing

- Three-key timing workflow: Start / Continue / End
- Rewind / Fast-forward, Play / Pause, playback speed from 0.25x to 3x, and jump to a specific time
- Undo / Redo for timing, lyrics, and line operations
- Tap the playback time to enter follow mode and locate the selected lyric
- Tap a lyric unit to select it; double-tap to jump to the start time of the current lyric and play from there

### 2. Import

- Plain text lyrics
- LRC, including line-level and word-level lyrics
- Enhanced LRC (ELRC)
- TTML
- Streaming lyric search
- Audio file import

### 3. Export

- LRC, including line-level and word-level lyrics
- TTML, with support for duets and backing vocals

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

### 6. Music Library

- Scan and filter local audio files
- Batch match tags, including cover art, title, artist, and more
- Batch match lyrics, including word-level lyrics, line-level lyrics, and translations
- Batch rename files
- Batch edit tags

### 7. Settings

- Custom dark mode
- Custom rewind and fast-forward duration for lyric timing
- Music library settings
- Audio metadata cover size settings
- Apple Music metadata region settings

## 5-Minute Quick Start (Recommended Order)

### Step 1: Import Audio and Lyrics

1. Import the audio file first.
2. Then choose the lyric import method based on the lyric source: plain text, LRC, ELRC, TTML, or search.

### Step 2: Preprocess Lyrics

1. Use one-click word segmentation to create word-level lyric units.
2. If needed, use empty line removal, lyric merging, or Traditional-to-Simplified Chinese conversion.
3. If the lyrics already have timestamps but are globally offset, use timestamp shifting before fine-tuning.

### Step 3: Start Timing Lyrics (Core Three-Key Workflow)

- Start: Set the start time of the current unit without jumping to the next unit.
- Continue: Set the end time of the current unit, move to the next unit automatically, and write the next unit's start time.
- End: Set the end time of the current unit and move to the next unit, without writing the next unit's start time.

Recommended rhythm: `Start -> Continue -> Continue -> ... -> End`, repeated by sentence.

### Step 4: Fine-Tune Timing and Text

1. Enable follow mode so the current lyric unit is automatically located during playback.
2. Double-tap a lyric to quickly jump back to its start time and review it.
3. Use undo/redo to quickly compare changes before and after editing.
4. Long-press lyrics or lines to split, merge, move, translate, or adjust structure.

### Step 5: Set Duets and Translations (Optional)

- Duet types: left duet (main vocal), right duet (secondary vocal), and background (backing vocal).
- Translations can be imported or edited manually.
- Note: Duet types only take effect in the TTML workflow.

### Step 6: Preview and Export

1. Check the word-level playback effect on the lyric preview page.
2. Export based on your intended use:
   - Use `LRC` for general music players.
   - Use `TTML` when duet or backing vocal information is needed.

## Lyric Timing Interface

- Columns: Start Time / Lyrics / End Time / Translation
- Locate: Jump to the selected lyric
- Time jump: Tap the time in the timing area to jump to a custom time
- Title tap: Get the audio file path

## Recommended Music Library Workflow

1. Scan local audio files, then review and filter the target songs.
2. Batch match tags to complete metadata such as cover art, title, and artist.
3. Batch match lyrics to complete word-level lyrics, line-level lyrics, and translations.
4. Batch rename and batch edit tags to finish organizing your library.

## Data Source Abbreviations

- QM: QQ Music
- NE: NetEase Cloud Music
- AM: Apple Music
- KG: Kugou Music

## FAQ

### 1. When should I export LRC, and when should I export TTML?

- Choose LRC if you only need regular lyric display.
- Choose TTML if you need duets, backing vocals, or a more complete lyric structure.

### 2. Why do I not see the duet effect after setting duet types?

Duet types are only supported in TTML. Please make sure you are using the TTML workflow and exporting as TTML.

### 3. The lyrics are globally early or late. How can I fix them quickly?

Use timestamp shifting for global alignment first, then return to the timing page for detailed fine-tuning.

### 4. What is the most common mistake for beginners?

Usually, it is timing the lyrics before word segmentation. It is recommended to complete word segmentation and text cleanup before starting lyric timing.

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

- [Join the LunaBeat QQ group](https://qm.qq.com/q/N0fBvuWKOY)  Group ID: 964680520
- [Join the LunaBeat Telegram group](https://t.me/+qXs6mjKqwhw3Zjll)
