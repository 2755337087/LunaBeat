<div align="center">

# LunaBeat

**Let melodies and lyrics meet with every play.**

Android local music player · Word-by-word lyric editor · Music library and tag management

[Download the latest version](https://github.com/2755337087/LunaBeat/releases/latest) · [User guide](#quick-start) · [Report an issue](https://github.com/2755337087/LunaBeat/issues) · [Community](#community-and-feedback)

</div>

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

---

LunaBeat brings local music playback, word-by-word lyric creation, and music library organization into one app. Listen at your own pace and watch lyrics unfold with the melody, or segment a song's lyrics, time each word, add translations and duet information, and export the result for your own lyric workflow.

## Interface preview

### Phone interface

<p align="center">
  <img src="Screenshots/phone.png" alt="LunaBeat · Phone interface" width="960" />
</p>

### Tablet interface

<p align="center">
  <img src="Screenshots/pad.png" alt="LunaBeat · Tablet interface" width="960" />
</p>

## Main features

| Use case | What you can do |
| --- | --- |
| Local music playback | Scan local audio and browse by artist, album, or playlist; enjoy background playback, system media controls, playback queues, and a sleep timer. |
| Immersive lyrics | Preview word-by-word lyrics with light and dark themes, playback bar and background settings; display lyrics on the home screen, in the status bar, or over your car's Bluetooth connection. |
| Word-by-word timing | Time lyrics with the three buttons Start / Continue / End, then refine them with playback speed controls, time seeking, follow mode, undo, and redo. |
| Lyric editing | Segment words, split and merge lyrics, move lines, shift timestamps, convert between Simplified and Traditional Chinese, remove blank lines, and set translations, duets, and backing vocals. |
| Music organization | Manage playlists, favorites, and five-star songs; batch-match covers, tags, and lyrics, edit metadata, and rename files. |

### Import and export

| Content | Import | Export |
| --- | --- | --- |
| Plain-text lyrics | Supported | — |
| LRC (line-by-line / word-by-word) | Supported | Supported |
| Enhanced LRC (ELRC) | Supported | Supported |
| TTML | Supported | Supported, preserving duet and backing-vocal information |

You can also search for lyrics or import audio to start creating. TTML is recommended when you need to preserve structural information such as duets and backing vocals; LRC is suitable for standard lyric display.

<a id="quick-start"></a>

## Quick start

### Listen and organize your music library

1. Open the music library and scan the local audio on your device.
2. Browse by artist, album, playlist, or favorites, then tap a song to play it.
3. Open the full player to switch playback modes, manage the queue, view lyrics, or set a sleep timer.
4. To organize files, batch-match tags and lyrics, then edit tags or rename files.

### Create word-by-word lyrics for a song

1. **Import**: Select audio, then import plain-text, LRC, ELRC, or TTML lyrics. You can also search for lyrics.
2. **Prepare**: Segment words first, then remove blank lines, merge lyrics, or convert between Simplified and Traditional Chinese. If timing already exists, shift the entire timeline to align it first.
3. **Time**: Play the audio and use the three buttons below in time with each lyric line.
4. **Refine**: Double-tap lyrics to seek back and listen. Check the timing with follow mode, undo, and redo; long-press a lyric unit or line to split, merge, move, or edit translations.
5. **Preview and export**: Check the word-by-word effect; choose LRC for standard display, or TTML for duet and backing-vocal information.

| Button | Action |
| --- | --- |
| Start | Set the current unit's start time and stay on that unit. |
| Continue | Set the current unit's end time and move to the next unit, also setting the next unit's start time. |
| End | Set the current unit's end time and move to the next unit, leaving the next unit's start time to be set later. |

Suggested rhythm: `Start → Continue → Continue → … → End`, repeated for each line.

<details>
<summary>Lyric timing tips</summary>

- The time and text columns show start time, lyrics, end time, and translation.
- Tap a lyric unit once to select it; double-tap to seek to its start time and listen again.
- Rewind / fast-forward, play / pause, playback speed controls, and seeking to a specific time are supported.
- Tap the playback time to enter follow mode; tap the time in the timing area to seek to a custom position.
- Duets can be set as left duet (lead vocal), right duet (second vocal), or background (backing vocals), then exported as TTML.

</details>

## Frequently asked questions

<details>
<summary>Should I choose LRC or TTML?</summary>

Choose LRC for standard lyric display, or TTML to preserve structural information such as duets and backing vocals. Check the word-by-word effect in the preview before exporting.

</details>

<details>
<summary>What should I keep in mind when exporting and playing duet lyrics?</summary>

Prefer TTML for exporting duet and backing-vocal information, and view it in a player that supports these effects. The actual display depends on the player's support for the lyric format and its associated information.

</details>

<details>
<summary>How do I fix lyrics that are all early or late?</summary>

Shift timestamps to align the whole timeline first, then return to the timing screen to refine individual sections.

</details>

<details>
<summary>Where should I start if this is my first time timing lyrics?</summary>

Finish word segmentation and text cleanup before timing. Choose a familiar song, use “Start → Continue → … → End” for each line, then listen back and make small adjustments.

</details>

<a id="community-and-feedback"></a>

## Community and feedback

- [GitHub Issues · Bug reports and feature suggestions](https://github.com/2755337087/LunaBeat/issues)
- [Telegram · LunaBeat community group](https://t.me/+qXs6mjKqwhw3Zjll)
- [QQ · LunaBeat community group](https://qm.qq.com/q/N0fBvuWKOY) · Group ID: **964680520**

When reporting an issue, please include the app version, device and Android version, steps to reproduce, and relevant screenshots or file formats to help identify the cause.

## Special thanks

### Technical support

- [TagLib](https://github.com/taglib/taglib)
- [Lyricon](https://github.com/tomakino/lyricon)
- [OpenCC](https://github.com/BYVoid/OpenCC)
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)
- [163Music2Tag](https://gitee.com/Wangs-official/163Music2Tag)
- [TagLib(Kyant0)](https://github.com/Kyant0/taglib)
- [uCrop](https://github.com/Yalantis/uCrop)
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui)
- [lottie-android](https://github.com/airbnb/lottie-android)
- [ICU Transliterator](https://unicode-org.github.io/icu/userguide/transforms/general/)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
- [AMLLTTML歌词站](https://amlldb.bikonoo.com/)
- [LunaBeat歌词站](https://github.com/2755337087/ttml-hub/blob/main/README.md)

### Inspiration

- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)
- [光锥音乐](https://coneplayer.trantor.ink/)
- [Halcyon](https://github.com/Kifranei/Halcyon)
- [Flamingo](https://github.com/Yos-X)
