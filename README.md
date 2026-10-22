<div align="center">

# LunaBeat

**让旋律与歌词，在每一次播放中相遇。**

Android 本地音乐播放器 · 逐字歌词编辑 · 音乐库与标签管理

[下载最新版本](https://github.com/2755337087/LunaBeat/releases/latest) · [使用指南](#快速上手) · [反馈问题](https://github.com/2755337087/LunaBeat/issues) · [交流社群](#交流与反馈)

</div>

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

---

LunaBeat 将本地音乐播放、逐字歌词制作与音乐库整理放在同一个应用中。你可以安静听歌、查看随旋律展开的歌词，也可以为一首歌分词、打轴、添加翻译与对唱信息，再导出到自己的歌词工作流。

## 界面预览

### 手机界面

<p align="center">
  <img src="Screenshots/phone.png" alt="LunaBeat · 手机界面" width="960" />
</p>

### 平板界面

<p align="center">
  <img src="Screenshots/pad.png" alt="LunaBeat · 平板界面" width="960" />
</p>

## 主要功能

| 场景 | 你可以做什么 |
| --- | --- |
| 本地听歌 | 扫描本地音频，按艺术家、专辑或歌单浏览；支持后台播放、系统媒体控制、播放队列与定时停止。 |
| 沉浸看词 | 预览逐字歌词，搭配深浅主题、播放控制条与背景设置；支持桌面歌词、状态栏歌词和车载蓝牙歌词。 |
| 逐字制作 | 使用「起始 / 连续 / 结束」三键打轴，配合倍速、时间跳转、跟随定位、撤销与重做精修时间。 |
| 歌词编辑 | 分词、拆分与合并、行移动、时间戳平移、繁简转换、清理空行，以及翻译、对唱与和声设置。 |
| 音乐整理 | 管理歌单、收藏与五星歌曲，批量匹配封面和标签、匹配歌词、编辑元数据与重命名文件。 |

### 导入与导出

| 内容 | 导入 | 导出 |
| --- | --- | --- |
| 纯文本歌词 | 支持 | — |
| LRC（逐行 / 逐字） | 支持 | 支持 |
| 增强 LRC（ELRC） | 支持 | 支持 |
| TTML | 支持 | 支持，保留对唱与和声信息 |

也可以通过搜索获取歌词，或导入音频开始制作。需要保留对唱与和声等结构信息时，建议选择 TTML；常规歌词显示可选择 LRC。

## 快速上手

### 听歌与整理音乐库

1. 进入音乐库，扫描设备中的本地音频。
2. 按艺术家、专辑、歌单或收藏浏览，点击歌曲开始播放。
3. 进入完整播放器，切换播放模式、管理队列、查看歌词或设置定时停止。
4. 需要整理文件时，批量匹配标签与歌词，再进行标签编辑或重命名。

### 制作一首逐字歌词

1. **导入**：选择音频，再导入纯文本、LRC、ELRC 或 TTML 歌词，也可搜索歌词。
2. **预处理**：先分词，再清理空行、合并歌词或进行繁简转换；已有时间轴可先整体平移校准。
3. **打轴**：播放音频，按一句歌词的节奏使用下方三键。
4. **精修**：双击歌词跳转回听，配合跟随定位、撤销与重做检查时间；长按歌词或行可拆分、合并、移动和编辑翻译。
5. **预览并导出**：检查逐字效果；常规显示选择 LRC，需要对唱与和声信息时选择 TTML。

| 按键 | 行为 |
| --- | --- |
| 起始 | 设置当前单元的开始时间，停留在当前单元。 |
| 连续 | 设置当前单元的结束时间并进入下一单元，同时写入下一单元的开始时间。 |
| 结束 | 设置当前单元的结束时间并进入下一单元，下一单元的开始时间留待设置。 |

推荐节奏：`起始 → 连续 → 连续 → … → 结束`，按句循环。

<details>
<summary>打轴操作提示</summary>

- 时间与文本列包括：开始时间、歌词、结束时间和翻译。
- 单击歌词单元选中，双击跳到该单元的开始时间回听。
- 支持快退 / 快进、播放 / 暂停、倍速播放与跳转指定时间。
- 点击播放时间可进入跟随模式；点击打轴区域时间可自定义跳转。
- 对唱可设置为左对唱（主唱）、右对唱（副唱）或背景（和声），并通过 TTML 导出。

</details>

## 常见问题

<details>
<summary>应该选择 LRC 还是 TTML？</summary>

常规歌词显示选择 LRC；需要保留对唱、和声等结构信息时选择 TTML。导出前可先在预览页确认逐字效果。

</details>

<details>
<summary>设置对唱后，导出与播放时需要注意什么？</summary>

建议优先通过 TTML 导出对唱与和声信息，并在支持对应效果的播放器中查看。实际显示效果取决于播放器对歌词格式与相关信息的支持。

</details>

<details>
<summary>歌词整体提前或延后，如何调整？</summary>

使用时间戳平移先做整体校准，再回到打轴页精修局部时间。

</details>

<details>
<summary>第一次打轴，建议从哪里开始？</summary>

先完成分词与文本清理，再开始打轴。选择熟悉的歌曲，按句使用「起始 → 连续 → … → 结束」，随后回听并微调。

</details>

## 交流与反馈

- [GitHub Issues · 问题反馈与功能建议](https://github.com/2755337087/LunaBeat/issues)
- [Telegram · LunaBeat 交流群组](https://t.me/+qXs6mjKqwhw3Zjll)
- [QQ · LunaBeat 交流群](https://qm.qq.com/q/N0fBvuWKOY)，群号：**964680520**

反馈问题时，请尽量附上应用版本、设备与 Android 版本、复现步骤，以及相关截图或文件格式，方便定位。

## 特别感谢

### 技术支持

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

### 灵感来源

- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)
- [光锥音乐](https://coneplayer.trantor.ink/)
- [Halcyon](https://github.com/Kifranei/Halcyon)
- [Flamingo](https://github.com/Yos-X)
