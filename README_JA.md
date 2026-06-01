# LunaBeat
- LunaBeat は、夜に流れる音楽のように、メロディと言葉のあいだで、より純粋で没入感のあるローカル音楽体験を届けることを目指しています。
---
- バージョン 2.0.0 以降、LyricBox は正式に LunaBeat へ名称を変更しました。
- これは単なる名称変更ではなく、本アプリが「ローカル歌詞エディター」から「歌詞と音楽再生が融合した体験」へ進化する新しい段階を意味しています。
- これまで応援してくださった皆さまに感謝します。LunaBeat はこれからも、ローカル音楽、動的歌詞、没入型プレーヤー体験の向上に注力していきます。
- LyricBox の物語は、LunaBeat という新しい名前でこれからも続いていきます。
---
- LunaBeat は、ローカル音楽と歌詞体験に特化したモバイル向け音楽プレーヤーです。音楽再生、歌詞編集、ワードレベルのタイミング調整機能を一体化しています。
- ローカル音声の再生、ワードレベル歌詞の作成（デュエットやバックボーカル対応）、歌詞のプレビューと編集に対応し、.lrc / .elrc / .ttml 形式での書き出しが可能です。また、音声メタデータと歌詞の一括処理にも対応しています。
- インポート、編集、再生、エクスポートまで、LunaBeat はより完全で没入感のあるローカル音楽・歌詞体験を提供することを目指しています。

## LunaBeat を選ぶ理由

- モバイル端末だけで本格的なワードレベルの歌詞タイミング調整が可能
- 複数形式のインポートとエクスポートに対応し、一般的な歌詞制作ワークフローに適応
- 音楽ライブラリとメタデータ編集ツールを統合し、アプリを切り替える手間を削減
- 大量の楽曲整理に適した一括処理機能を搭載

## 画面プレビュー

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

## 機能概要

### 1. タイミング調整と編集
- 開始 / 連続 / 終了 の 3 つのボタンによるタイミング調整
- 巻き戻し / 早送り、再生 / 一時停止、再生速度（0.25x ~ 3x）、指定時間へのジャンプ
- 元に戻す / やり直し（時間、歌詞、行操作に対応）
- 再生時間をタップして追従モードに入り、選択した歌詞へ移動
- 歌詞ユニットをタップして選択、ダブルタップで現在の歌詞開始時間へジャンプして再生

### 2. インポート
- プレーンテキスト歌詞
- LRC（行単位 / ワードレベル）
- 拡張 LRC（ELRC）
- TTML
- ストリーミングサービスからの歌詞検索
- 音声ファイルのインポート

### 3. エクスポート
- LRC（行単位 / ワードレベル）
- TTML（デュエット、バックボーカル対応）

### 4. プレビューと一括処理
- ワードレベル再生効果のプレビュー
- ワンタップ分割
- タイムスタンプのシフト
- 歌詞の一括結合
- 繁体字から簡体字への変換
- 空行の一括削除

### 5. 歌詞構造操作（長押し）
- 歌詞操作：編集、追加、分割、結合、時間設定、削除
- 行操作：追加、結合、分割、移動、翻訳の追加 / 変更、削除

### 6. 音楽ライブラリ
- ローカル音声のスキャンとフィルタリング
- タグの一括マッチング（カバー、タイトル、アーティストなど）
- 歌詞の一括マッチング（ワードレベル / 行単位 / 翻訳）
- 一括リネーム
- タグの一括編集

### 7. 設定
- ダークモードのカスタマイズ
- タイミング調整時の早送り / 巻き戻し時間のカスタマイズ
- 音楽ライブラリ設定
- 音声メタデータのカバーサイズ設定
- AM メタデータ地域設定

## 5 分で始めるクイックガイド（推奨手順）

### ステップ 1：音声と歌詞をインポート
1. まず音声ファイルをインポートします。
2. 次に歌詞の入手元に応じて、インポート方法（プレーンテキスト / LRC / ELRC / TTML / 検索）を選択します。

### ステップ 2：歌詞を前処理
1. ワンタップ分割を使用して、ワードレベルのユニットを作成します。
2. 必要に応じて、空行の一括削除、歌詞の結合、繁体字から簡体字への変換を行います。
3. 既存のタイムラインに全体的なズレがある場合は、先にタイムスタンプのシフトで調整してから細かい編集に進みます。

### ステップ 3：タイミング調整を開始（核心となる 3 ボタン）
- 開始：現在のユニットの開始時間を設定します。次のユニットへは移動しません。
- 連続：現在のユニットの終了時間を設定し、自動で次のユニットへ移動します。同時に次のユニットの開始時間も書き込みます。
- 終了：現在のユニットの終了時間を設定し、次のユニットへ移動します。ただし次のユニットの開始時間は書き込みません。

推奨リズム：`開始 -> 連続 -> 連続 -> ... -> 終了` を、フレーズごとに繰り返します。

### ステップ 4：時間とテキストを微調整
1. 追従モードをオンにすると、再生中に現在のユニットへ自動で移動します。
2. 歌詞をダブルタップすると、その開始時間へすばやくジャンプして確認できます。
3. 元に戻す / やり直しを使って、変更前後の効果をすばやく比較できます。
4. 歌詞または行を長押しして、分割、結合、移動、翻訳などの構造調整を行います。

### ステップ 5：デュエットと翻訳を設定（任意）
- デュエットタイプ：左デュエット（メインボーカル）、右デュエット（サブボーカル）、背景（バックボーカル）。
- 翻訳：インポートまたは手動編集に対応しています。
- 注意：デュエットタイプは TTML ワークフローでのみ有効です。

### ステップ 6：プレビューしてエクスポート
1. 歌詞プレビュー画面で、ワードレベル再生効果を確認します。
2. 用途に応じて書き出します：
   - 一般的なプレーヤー向けには `LRC` を優先
   - デュエット / バックボーカル情報が必要な場合は `TTML` を優先

## タイミング調整画面の説明

- 列情報：開始時間 / 歌詞 / 終了時間 / 翻訳
- 位置移動：選択した歌詞へ移動できます
- 時間ジャンプ：タイミング調整エリアの時間をタップして任意の時間へジャンプできます
- タイトルタップ：音声ファイルのパスを取得できます

## 音楽ライブラリのワークフロー提案

1. ローカル音声をスキャンし、対象楽曲を確認・フィルタリングします。
2. タグを一括マッチングし、カバー、タイトル、アーティストなどのメタデータを補完します。
3. 歌詞を一括マッチングし、ワードレベル / 行単位 / 翻訳をまとめて補完します。
4. 一括リネームとタグ一括編集を行い、最終整理を完了します。

## データソース略称

- QM：QQ 音楽
- NE：网易云音乐
- AM：Apple Music
- KG：酷狗音乐

## よくある質問（FAQ）

### 1. どのような場合に LRC を書き出し、どのような場合に TTML を書き出すべきですか？
- 通常の歌詞表示だけが必要な場合：LRC を選択してください。
- デュエット、バックボーカル、より完全な構造が必要な場合：TTML を選択してください。

### 2. デュエットを設定したのに効果が表示されないのはなぜですか？
デュエットタイプは TTML のみ対応しています。現在の作業フローが TTML であることを確認し、TTML 形式で書き出してください。

### 3. 歌詞全体が早い / 遅い場合、どうすればすばやく修正できますか？
まずタイムスタンプのシフトで全体を調整し、その後タイミング調整画面に戻って部分的に微調整すると効率的です。

### 4. 初心者が最も間違えやすい手順は何ですか？
多くの場合、「先にタイミング調整をしてから分割する」ことです。先に分割とテキスト整理を完了してから、タイミング調整を始めることをおすすめします。

## Special Thanks

Inspired by:
- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)

Technical Support:

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
## コミュニティとフィードバック

- [LunaBeat 交流 QQ グループに参加](https://qm.qq.com/q/N0fBvuWKOY)  グループ番号：964680520
- [LunaBeat Telegram グループに参加](https://t.me/+qXs6mjKqwhw3Zjll)
