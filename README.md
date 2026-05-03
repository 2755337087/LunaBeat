# LyricBox

LyricBox 是一款面向移动端的专业歌词打轴与编辑工具，支持从导入、编辑、预览到导出的完整流程。你可以用它完成逐字歌词制作（含对唱与背景声），导出 `.lrc` / `.ttml`，并批量处理音频元数据与歌词。

## 为什么用 LyricBox

- 移动端即可完成专业级逐字打轴
- 支持多格式导入导出，适配常见歌词工作流
- 集成音乐库与元数据工具，减少来回切软件的成本
- 内置批量处理能力，适合整理大量歌曲

## 界面预览

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LyricBox/main/Screenshots/mainPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LyricBox/main/Screenshots/musicLibraryPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LyricBox/main/Screenshots/searchPage.jpg" width="30%" />
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LyricBox/main/Screenshots/lyricEditPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LyricBox/main/Screenshots/lyricPrePage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LyricBox/main/Screenshots/musicEditPage.jpg" width="30%" />
</p>

## 功能总览

### 1. 打轴与编辑
- 起始 / 连续 / 结束三键打轴
- 快退 / 快进、播放 / 暂停、倍速（0.25x ~ 3x）、跳转指定时间
- 撤销 / 重做（覆盖时间、歌词、行操作）
- 跟随模式、定位到选中歌词
- 单击选中，双击跳转当前歌词开始时间播放

### 2. 导入
- 纯文本歌词
- LRC（逐行 / 逐字）
- 增强 LRC（ELRC）
- TTML
- 流媒体搜索歌词
- 音频文件导入

### 3. 导出
- LRC（逐行 / 逐字）
- TTML（支持对唱、和声）

### 4. 预览与批量处理
- 逐字播放效果预览
- 一键分词
- 时间戳平移
- 一键合并歌词
- 繁体转简体
- 一键删除空行

### 5. 歌词结构操作（长按）
- 歌词操作：编辑、新增、拆分、合并、设置时间、删除
- 行操作：新增、合并、拆分、移动、添加/修改翻译、删除

### 6. 音乐库
- 扫描本地音频并筛选
- 批量匹配标签（封面、标题、艺术家等）
- 批量匹配歌词（逐字 / 逐行 / 翻译）
- 批量重命名
- 批量编辑标签

### 7. 设置
- 自定义深色模式
- 自定义打轴快进/快退时长
- 音乐库设置
- 音频元数据封面尺寸设置
- AM 元数据地区设置

## 5 分钟快速上手（建议顺序）

### 第 1 步：导入音频与歌词
1. 先导入音频文件。
2. 再按歌词来源选择导入方式（纯文本 / LRC / ELRC / TTML / 搜索）。

### 第 2 步：预处理歌词
1. 使用一键分词，得到逐字单元。
2. 视情况使用一键删除空行、合并歌词、繁转简。
3. 如果已有时间轴但整体偏移，先做时间戳平移再进入精修。

### 第 3 步：开始打轴（核心三键）
- 起始：设置当前单元开始时间，不跳转。
- 连续：设置当前单元结束时间并自动到下一单元，同时写入下一单元开始时间。
- 结束：设置当前单元结束时间并跳到下一单元，但不写下一单元开始时间。

推荐节奏：`起始 -> 连续 -> 连续 -> ... -> 结束`，按句循环。

### 第 4 步：精修时间与文本
1. 打开跟随模式，播放时自动定位当前单元。
2. 双击歌词快速跳到该处开始时间回听。
3. 用撤销/重做快速对比修改前后效果。
4. 长按歌词或行，做拆分/合并/移动/翻译等结构修正。

### 第 5 步：设置对唱与翻译（可选）
- 对唱类型：左对唱（主唱）、右对唱（副唱）、背景（和声）。
- 翻译：支持导入或手动编辑。
- 注意：对唱类型仅在 TTML 工作流中生效。

### 第 6 步：预览并导出
1. 在歌词预览页检查逐字播放效果。
2. 根据用途导出：
   - 通用播放器优先 `LRC`
   - 需要对唱/和声信息优先 `TTML`

## 打轴界面说明

- 列信息：开始时间 / 歌词 / 结束时间 / 翻译
- 定位：可定位到选中歌词
- 时间跳转：点击打轴区域时间可自定义跳转
- 标题点击：可获取音频路径

## 音乐库工作流建议

1. 扫描本地音频，先清点与过滤目标歌曲。
2. 批量匹配标签，补全封面/标题/艺术家等元数据。
3. 批量匹配歌词，统一补齐逐字/逐行与翻译。
4. 批量重命名与批量编辑，完成最终整理。

## 数据源缩写说明

- QM：QQ 音乐
- NE：网易云音乐
- AM：Apple Music
- KG：酷狗音乐

## 常见问题（FAQ）

### 1. 什么时候导出 LRC，什么时候导出 TTML？
- 只需要常规歌词显示：选 LRC。
- 需要对唱、和声、更完整结构：选 TTML。

### 2. 为什么我设置了对唱但看不到效果？
对唱类型仅支持 TTML。请确认当前是 TTML 工作流，并以 TTML 导出。

### 3. 歌词整体早了/晚了，如何快速修正？
先用时间戳平移做整体校准，再回到打轴页做局部精修，效率最高。

### 4. 新手最容易出错的步骤是什么？
通常是“先打轴后分词”。建议先完成分词与文本清理，再开始打轴。

## 特别感谢

灵感来源：
- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)

技术支持：
- [FFmpegKit](https://github.com/arthenica/ffmpeg-kit)
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

## 交流与反馈

[点击加入 LyricBox 交流 QQ 群](https://qm.qq.com/q/N0fBvuWKOY)  群号：964680520
