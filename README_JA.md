<div align="center">

# LunaBeat

**再生するたびに、メロディーと歌詞が出会う。**

Android ローカル音楽プレーヤー · 単語ごとの歌詞編集 · 音楽ライブラリとタグの管理

[最新版をダウンロード](https://github.com/2755337087/LunaBeat/releases/latest) · [使い方](#quick-start) · [問題を報告](https://github.com/2755337087/LunaBeat/issues) · [コミュニティ](#community-and-feedback)

</div>

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

---

LunaBeat は、ローカル音楽の再生、単語ごとの歌詞制作、音楽ライブラリの整理を一つのアプリにまとめました。音楽をゆっくり楽しみ、メロディーに合わせて流れる歌詞を眺めることも、歌詞を分割してタイミングを付け、翻訳やデュエット情報を追加し、自分の制作環境にエクスポートすることもできます。

## 画面プレビュー

### スマートフォンの画面

<p align="center">
  <img src="Screenshots/phone.png" alt="LunaBeat · スマートフォンの画面" width="960" />
</p>

### タブレットの画面

<p align="center">
  <img src="Screenshots/pad.png" alt="LunaBeat · タブレットの画面" width="960" />
</p>

## 主な機能

| 用途 | できること |
| --- | --- |
| ローカル音楽の再生 | ローカル音声をスキャンし、アーティスト、アルバム、プレイリスト別に表示。バックグラウンド再生、システムのメディア操作、再生キュー、スリープタイマーに対応。 |
| 没入感のある歌詞表示 | 単語ごとの歌詞をプレビューし、ライト・ダークテーマ、再生バー、背景を設定。ホーム画面、ステータスバー、車載 Bluetooth での歌詞表示に対応。 |
| 単語ごとのタイミング調整 | 「開始 / 連続 / 終了」の3ボタンでタイミングを付け、再生速度、時間ジャンプ、追従モード、元に戻す・やり直す機能で細かく調整。 |
| 歌詞の編集 | 単語分割、分割・結合、行の移動、タイムスタンプの一括シフト、中国語の簡体字・繁体字変換、空行の削除、翻訳・デュエット・コーラスの設定。 |
| 音楽の整理 | プレイリスト、お気に入り、星5の曲を管理。カバー画像・タグ・歌詞の一括照合、メタデータ編集、ファイル名の変更。 |

### インポートとエクスポート

| 内容 | インポート | エクスポート |
| --- | --- | --- |
| プレーンテキストの歌詞 | 対応 | — |
| LRC（行ごと / 単語ごと） | 対応 | 対応 |
| 拡張 LRC（ELRC） | 対応 | 対応 |
| TTML | 対応 | 対応。デュエットとコーラスの情報を保持 |

歌詞を検索したり、音声をインポートして制作を始めたりすることもできます。デュエットやコーラスなどの構造情報を保持するには TTML、通常の歌詞表示には LRC をおすすめします。

<a id="quick-start"></a>

## クイックスタート

### 音楽を聴く・ライブラリを整理する

1. 音楽ライブラリを開き、端末内のローカル音声をスキャンします。
2. アーティスト、アルバム、プレイリスト、お気に入りから曲を探し、タップして再生します。
3. フルプレーヤーを開くと、再生モードの切り替え、キューの管理、歌詞表示、スリープタイマーの設定ができます。
4. ファイルを整理する場合は、タグと歌詞を一括照合してから、タグ編集やファイル名の変更を行います。

### 一曲の歌詞に単語ごとのタイミングを付ける

1. **インポート**：音声を選び、プレーンテキスト、LRC、ELRC、TTML の歌詞を読み込みます。歌詞の検索も利用できます。
2. **下準備**：先に単語分割を行い、空行の削除、歌詞の結合、簡体字・繁体字変換を済ませます。既存のタイミングがある場合は、まず全体をシフトして位置を合わせます。
3. **タイミング調整**：音声を再生し、歌詞の一行のリズムに合わせて下の3ボタンを使います。
4. **微調整**：歌詞をダブルタップして聴き直し、追従モードや元に戻す・やり直す機能で時間を確認します。歌詞の単位や行を長押しすると、分割、結合、移動、翻訳編集ができます。
5. **プレビューとエクスポート**：単語ごとの表示を確認し、通常の表示には LRC、デュエットとコーラス情報が必要な場合は TTML を選びます。

| ボタン | 動作 |
| --- | --- |
| 開始 | 現在の単位の開始時間を設定し、その単位にとどまります。 |
| 連続 | 現在の単位の終了時間を設定して次へ進み、次の単位の開始時間も設定します。 |
| 終了 | 現在の単位の終了時間を設定して次へ進みます。次の単位の開始時間は後で設定します。 |

おすすめの操作：`開始 → 連続 → 連続 → … → 終了` を一行ごとに繰り返します。

<details>
<summary>タイミング調整のヒント</summary>

- 時間とテキストの列には、開始時間、歌詞、終了時間、翻訳が表示されます。
- 歌詞の単位を1回タップすると選択し、ダブルタップするとその開始時間に移動して聴き直せます。
- 巻き戻し / 早送り、再生 / 一時停止、再生速度の変更、指定時間への移動に対応しています。
- 再生時間をタップすると追従モードに入り、タイミング調整エリアの時間をタップすると移動先を指定できます。
- デュエットは左（主唱）、右（副唱）、背景（コーラス）に設定でき、TTML でエクスポートできます。

</details>

## よくある質問

<details>
<summary>LRC と TTML のどちらを選べばよいですか？</summary>

通常の歌詞表示には LRC、デュエットやコーラスなどの構造情報を保持するには TTML を選んでください。エクスポート前にプレビューで単語ごとの表示を確認できます。

</details>

<details>
<summary>デュエット設定後のエクスポートや再生で注意することはありますか？</summary>

デュエットとコーラスの情報は TTML でのエクスポートを優先し、対応する表示機能を持つプレーヤーで確認することをおすすめします。実際の表示は、プレーヤーの歌詞形式や関連情報への対応状況によって異なります。

</details>

<details>
<summary>歌詞全体が早い、または遅い場合はどう調整しますか？</summary>

まずタイムスタンプを一括シフトして全体を合わせ、その後タイミング調整画面で部分ごとに微調整します。

</details>

<details>
<summary>初めてタイミングを付ける場合は、どこから始めればよいですか？</summary>

単語分割とテキストの整理を終えてから始めてください。よく知っている曲を選び、一行ごとに「開始 → 連続 → … → 終了」を使い、その後聴き直して調整します。

</details>

<a id="community-and-feedback"></a>

## コミュニティとフィードバック

- [GitHub Issues · 不具合報告と機能の提案](https://github.com/2755337087/LunaBeat/issues)
- [Telegram · LunaBeat コミュニティグループ](https://t.me/+qXs6mjKqwhw3Zjll)
- [QQ · LunaBeat コミュニティグループ](https://qm.qq.com/q/N0fBvuWKOY) · グループ番号: **964680520**

問題を報告する際は、アプリのバージョン、端末と Android のバージョン、再現手順、関連するスクリーンショットやファイル形式を添えると、原因の特定に役立ちます。

## 謝辞

### 技術サポート

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

### インスピレーション

- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)
- [光锥音乐](https://coneplayer.trantor.ink/)
- [Halcyon](https://github.com/Kifranei/Halcyon)
- [Flamingo](https://github.com/Yos-X)
