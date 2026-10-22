<div align="center">

# LunaBeat

**讓旋律與歌詞，在每一次播放中相遇。**

Android 本機音樂播放器 · 逐字歌詞編輯 · 音樂庫與標籤管理

[下載最新版本](https://github.com/2755337087/LunaBeat/releases/latest) · [使用指南](#quick-start) · [回報問題](https://github.com/2755337087/LunaBeat/issues) · [交流社群](#community-and-feedback)

</div>

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

---

LunaBeat 將本機音樂播放、逐字歌詞製作與音樂庫整理放在同一個應用程式中。你可以靜靜聽歌、查看隨旋律展開的歌詞，也可以為一首歌分詞、打軸、加入翻譯與對唱資訊，再匯出到自己的歌詞工作流程。

## 介面預覽

### 手機介面

<p align="center">
  <img src="Screenshots/phone.png" alt="LunaBeat · 手機介面" width="960" />
</p>

### 平板介面

<p align="center">
  <img src="Screenshots/pad.png" alt="LunaBeat · 平板介面" width="960" />
</p>

## 主要功能

| 情境 | 你可以做什麼 |
| --- | --- |
| 本機聽歌 | 掃描本機音訊，依演出者、專輯或播放清單瀏覽；支援背景播放、系統媒體控制、播放佇列與定時停止。 |
| 沉浸看詞 | 預覽逐字歌詞，搭配深淺主題、播放控制列與背景設定；支援桌面歌詞、狀態列歌詞和車載藍牙歌詞。 |
| 逐字製作 | 使用「起始 / 連續 / 結束」三鍵打軸，搭配倍速、時間跳轉、跟隨定位、復原與重做精修時間。 |
| 歌詞編輯 | 分詞、拆分與合併、行移動、時間戳記平移、繁簡轉換、清理空白行，以及翻譯、對唱與和聲設定。 |
| 音樂整理 | 管理播放清單、收藏與五星歌曲，批次比對封面和標籤、比對歌詞、編輯中繼資料與重新命名檔案。 |

### 匯入與匯出

| 內容 | 匯入 | 匯出 |
| --- | --- | --- |
| 純文字歌詞 | 支援 | — |
| LRC（逐行 / 逐字） | 支援 | 支援 |
| 增強 LRC（ELRC） | 支援 | 支援 |
| TTML | 支援 | 支援，保留對唱與和聲資訊 |

也可以透過搜尋取得歌詞，或匯入音訊開始製作。需要保留對唱與和聲等結構資訊時，建議選擇 TTML；一般歌詞顯示可選擇 LRC。

<a id="quick-start"></a>

## 快速上手

### 聽歌與整理音樂庫

1. 進入音樂庫，掃描裝置中的本機音訊。
2. 依演出者、專輯、播放清單或收藏瀏覽，點選歌曲開始播放。
3. 進入完整播放器，切換播放模式、管理佇列、查看歌詞或設定定時停止。
4. 需要整理檔案時，批次比對標籤與歌詞，再編輯標籤或重新命名。

### 製作一首逐字歌詞

1. **匯入**：選擇音訊，再匯入純文字、LRC、ELRC 或 TTML 歌詞，也可搜尋歌詞。
2. **前處理**：先分詞，再清理空白行、合併歌詞或進行繁簡轉換；已有時間軸可先整體平移校準。
3. **打軸**：播放音訊，依一句歌詞的節奏使用下方三鍵。
4. **精修**：點兩下歌詞跳轉回聽，搭配跟隨定位、復原與重做檢查時間；長按歌詞或行可拆分、合併、移動和編輯翻譯。
5. **預覽並匯出**：檢查逐字效果；一般顯示選擇 LRC，需要對唱與和聲資訊時選擇 TTML。

| 按鍵 | 行為 |
| --- | --- |
| 起始 | 設定目前單元的開始時間，停留在目前單元。 |
| 連續 | 設定目前單元的結束時間並進入下一單元，同時寫入下一單元的開始時間。 |
| 結束 | 設定目前單元的結束時間並進入下一單元，下一單元的開始時間留待設定。 |

建議節奏：`起始 → 連續 → 連續 → … → 結束`，按句循環。

<details>
<summary>打軸操作提示</summary>

- 時間與文字欄包含：開始時間、歌詞、結束時間和翻譯。
- 點一下歌詞單元即可選取，點兩下跳到該單元的開始時間回聽。
- 支援快退 / 快進、播放 / 暫停、倍速播放與跳轉指定時間。
- 點選播放時間可進入跟隨模式；點選打軸區域時間可自訂跳轉。
- 對唱可設為左對唱（主唱）、右對唱（副唱）或背景（和聲），並透過 TTML 匯出。

</details>

## 常見問題

<details>
<summary>應該選擇 LRC 還是 TTML？</summary>

一般歌詞顯示選擇 LRC；需要保留對唱、和聲等結構資訊時選擇 TTML。匯出前可先在預覽頁確認逐字效果。

</details>

<details>
<summary>設定對唱後，匯出與播放時需要注意什麼？</summary>

建議優先透過 TTML 匯出對唱與和聲資訊，並在支援對應效果的播放器中查看。實際顯示效果取決於播放器對歌詞格式與相關資訊的支援。

</details>

<details>
<summary>歌詞整體提前或延後，如何調整？</summary>

使用時間戳記平移先做整體校準，再回到打軸頁精修局部時間。

</details>

<details>
<summary>第一次打軸，建議從哪裡開始？</summary>

先完成分詞與文字清理，再開始打軸。選擇熟悉的歌曲，按句使用「起始 → 連續 → … → 結束」，隨後回聽並微調。

</details>

<a id="community-and-feedback"></a>

## 交流與回饋

- [GitHub Issues · 問題回報與功能建議](https://github.com/2755337087/LunaBeat/issues)
- [Telegram · LunaBeat 交流群組](https://t.me/+qXs6mjKqwhw3Zjll)
- [QQ · LunaBeat 交流群](https://qm.qq.com/q/N0fBvuWKOY) · 群組號碼: **964680520**

回報問題時，請盡量附上應用程式版本、裝置與 Android 版本、重現步驟，以及相關截圖或檔案格式，方便定位。

## 特別感謝

### 技術支持

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

### 靈感來源

- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)
- [光锥音乐](https://coneplayer.trantor.ink/)
- [Halcyon](https://github.com/Kifranei/Halcyon)
- [Flamingo](https://github.com/Yos-X)
