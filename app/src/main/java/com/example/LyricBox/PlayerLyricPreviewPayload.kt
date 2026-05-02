package com.example.LyricBox

import com.example.LyricBox.utils.AudioMetadataReader
import java.io.File

data class PlayerLyricPreviewPayload(
    val lines: List<NewPreviewLyricLine>,
    val creators: List<String>
)

fun buildPlayerLyricPreviewPayload(audioPath: String): PlayerLyricPreviewPayload? {
    val lyricsContent = resolveLyricsContentForPlayer(audioPath) ?: return null
    val format = detectLyricsFormat(lyricsContent)
    val lyricLines = parseLyricsLinesForPlayer(lyricsContent, format)
    if (lyricLines.isEmpty()) return null

    val previewLines = convertLyricsToPreviewLinesForPlayer(lyricLines)
    if (previewLines.isEmpty()) return null

    val creators = if (format == 3) {
        runCatching { LyricParsingUtils.parseSongwritersFromTtml(lyricsContent) }
            .getOrDefault(emptyList())
    } else {
        emptyList()
    }

    return PlayerLyricPreviewPayload(
        lines = previewLines,
        creators = creators
    )
}

private fun resolveLyricsContentForPlayer(audioPath: String): String? {
    val embedded = AudioMetadataReader.readLyrics(audioPath)?.takeIf { it.isNotBlank() }
    val ttmlFile = resolveSameNameTtmlFile(audioPath)
    val externalTtml = ttmlFile?.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    return externalTtml ?: embedded
}

private fun resolveSameNameTtmlFile(audioPath: String): File? {
    val audioFile = File(audioPath)
    val parent = audioFile.parentFile ?: return null
    val target = File(parent, "${audioFile.nameWithoutExtension}.ttml")
    return if (target.exists()) target else null
}

private fun parseLyricsLinesForPlayer(lyricsContent: String, format: Int): List<LyricLine> {
    val trimmed = lyricsContent.trim()
    if (trimmed.isEmpty()) return emptyList()

    return when (format) {
        1 -> LyricParsingUtils.parseByType(LyricParseType.SPL_LRC, trimmed).lyricLines
        2 -> LyricParsingUtils.parseByType(LyricParseType.ENHANCED_LRC, trimmed).lyricLines
        3 -> LyricParsingUtils.parseByType(LyricParseType.TTML, trimmed).lyricLines
        else -> trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                LyricLine(
                    timeUnits = listOf(
                        LyricTimeUnit(
                            text = line,
                            startTime = "00:00.000",
                            endTime = "00:00.000"
                        )
                    ),
                    translation = "",
                    agentType = LyricAgentType.LEFT,
                    lineKey = ""
                )
            }
    }
}

private fun convertLyricsToPreviewLinesForPlayer(lines: List<LyricLine>): List<NewPreviewLyricLine> {
    return lines.mapNotNull { line ->
        if (line.timeUnits.isEmpty()) return@mapNotNull null

        val expandedWords = if (line.timeUnits.size == 1) {
            // 与打轴页预览一致：单 timeUnit 保持逐行歌词，不拆分字符。
            val unit = line.timeUnits.first()
            val beginMs = parseTimeToMs(unit.startTime)
            val endMs = normalizeEndTime(beginMs, parseTimeToMs(unit.endTime))
            val safeText = unit.text.trimStart()
            if (safeText.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    NewPreviewLyricWord(
                        text = safeText,
                        begin = beginMs,
                        end = endMs,
                        transliteration = unit.transliteration,
                        charTransliterations = unit.charTransliterations
                    )
                )
            }
        } else {
            // 与打轴页预览一致：多 timeUnit 展开为逐字符，非空格字符均分时长。
            line.timeUnits.flatMap { unit ->
                val beginMs = parseTimeToMs(unit.startTime)
                val endMs = normalizeEndTime(beginMs, parseTimeToMs(unit.endTime))
                val duration = (endMs - beginMs).coerceAtLeast(0L)
                val text = unit.text
                if (text.isEmpty()) {
                    emptyList()
                } else if (text.length == 1) {
                    listOf(
                        NewPreviewLyricWord(
                            text = text,
                            begin = beginMs,
                            end = endMs,
                            transliteration = if (unit.charTransliterations.isNotEmpty()) {
                                unit.charTransliterations[0] ?: ""
                            } else {
                                ""
                            },
                            charTransliterations = emptyMap()
                        )
                    )
                } else {
                    val nonSpaceChars = text.filter { it != ' ' }
                    val nonSpaceCount = nonSpaceChars.length
                    if (nonSpaceCount == 0) {
                        text.map { char ->
                            NewPreviewLyricWord(
                                text = char.toString(),
                                begin = beginMs,
                                end = beginMs,
                                transliteration = "",
                                charTransliterations = emptyMap()
                            )
                        }
                    } else {
                        val charDuration = duration / nonSpaceCount
                        var currentTime = beginMs
                        var textIndex = 0
                        text.map { char ->
                            if (char == ' ') {
                                textIndex++
                                NewPreviewLyricWord(
                                    text = char.toString(),
                                    begin = currentTime,
                                    end = currentTime,
                                    transliteration = "",
                                    charTransliterations = emptyMap()
                                )
                            } else {
                                val charBegin = currentTime
                                val charEnd = if (currentTime + charDuration >= endMs) endMs else currentTime + charDuration
                                currentTime = charEnd
                                val transliteration = if (unit.charTransliterations.isNotEmpty()) {
                                    unit.charTransliterations[textIndex] ?: ""
                                } else {
                                    ""
                                }
                                textIndex++
                                NewPreviewLyricWord(
                                    text = char.toString(),
                                    begin = charBegin,
                                    end = charEnd,
                                    transliteration = transliteration,
                                    charTransliterations = emptyMap()
                                )
                            }
                        }
                    }
                }
            }
        }

        val trimmedLeading = expandedWords.dropWhile { it.text.isBlank() }
        if (trimmedLeading.isEmpty()) return@mapNotNull null

        NewPreviewLyricLine(
            words = trimmedLeading,
            translation = line.translation.trimStart(),
            isDuet = line.agentType == LyricAgentType.RIGHT,
            isBackground = line.agentType == LyricAgentType.BACKGROUND
        )
    }
}

private fun normalizeEndTime(begin: Long, parsedEnd: Long): Long {
    return when {
        parsedEnd == 0L -> 0L
        parsedEnd >= begin -> parsedEnd
        else -> begin
    }
}
