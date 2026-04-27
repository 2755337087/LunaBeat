package com.example.LyricBox

private val ttmlRootTagRegex = Regex("""<tt\b[^>]*\bxmlns\b""", RegexOption.IGNORE_CASE)
private val elrcWordTimestampRegex = Regex("""<\d{2}:\d{2}\.\d{2,3}>""")
private val lrcTimestampRegex = Regex("""\[\d{2}:\d{2}\.\d{2,3}\]""")

/**
 * 检测歌词文本格式，返回 MusicLibrary/SongMetadata 页面使用的格式索引：
 * 0 = 纯文本歌词, 1 = LRC逐行/逐字歌词, 2 = 增强LRC/ELRC歌词, 3 = TTML歌词
 */
fun detectLyricsFormat(lyrics: String): Int {
    val trimmed = lyrics.trim()

    if (ttmlRootTagRegex.containsMatchIn(trimmed)) {
        return 3
    }

    if (elrcWordTimestampRegex.containsMatchIn(trimmed)) {
        return 2
    }

    val lines = trimmed.lines().filter { it.isNotBlank() }
    var lineWithMultipleTimestamps = 0

    for (line in lines.take(20)) {
        if (lrcTimestampRegex.findAll(line).count() > 1) {
            lineWithMultipleTimestamps++
        }
    }

    if (lineWithMultipleTimestamps > 0) {
        return 1
    }

    if (lrcTimestampRegex.containsMatchIn(trimmed)) {
        return 1
    }

    return 0
}

