package com.example.LyricBox.utils

import android.content.Context
import com.example.LyricBox.LyricLine
import com.example.LyricBox.buildSavedLyricFromLines as buildWordLrcFromTimingPage
import com.example.LyricBox.buildTtmlContent as buildTtmlFromTimingPage
import com.example.LyricBox.saveTtmlToFile as saveTtmlToFileFromTimingPage
import com.example.LyricBox.toEnhancedLrc as buildEnhancedLrcFromTimingPage

enum class LyricExportFormat {
    LRC_WORD,
    LRC_LINE,
    ENHANCED_LRC,
    TTML
}

object LyricSaveEmbedUtils {
    private fun isBlankLyricLineForExport(lyricLine: LyricLine): Boolean {
        return lyricLine.timeUnits.isEmpty() || lyricLine.timeUnits.all { it.text.isBlank() }
    }

    private fun firstExportStartTime(lyricLine: LyricLine, fallback: String): String {
        return lyricLine.timeUnits.firstOrNull { it.text.isNotBlank() }?.startTime
            ?: lyricLine.timeUnits.firstOrNull()?.startTime
            ?: fallback
    }

    fun buildWordLrc(lyricLines: List<LyricLine>): String {
        return buildWordLrcFromTimingPage(lyricLines)
    }

    fun buildLineLrc(
        lyricLines: List<LyricLine>,
        showLineEndTime: Boolean
    ): String {
        return buildString {
            var previousLineStartTime = "00:00.000"
            lyricLines.forEach { lyricLine ->
                if (isBlankLyricLineForExport(lyricLine)) {
                    append("[$previousLineStartTime]\n")
                    return@forEach
                }

                val startTime = firstExportStartTime(lyricLine, previousLineStartTime)
                previousLineStartTime = startTime
                val endTime = lyricLine.timeUnits.last().endTime
                val lineText = lyricLine.timeUnits.joinToString("") { it.text }

                if (showLineEndTime) {
                    append("[$startTime]$lineText[$endTime]")
                } else {
                    append("[$startTime]$lineText")
                }
                append("\n")

                if (lyricLine.translation.isNotEmpty()) {
                    append("[$startTime]${lyricLine.translation}\n")
                }
            }
        }
    }

    fun buildEnhancedLrc(
        lyricLines: List<LyricLine>,
        showDuet: Boolean
    ): String {
        return buildEnhancedLrcFromTimingPage(lyricLines, showDuet)
    }

    fun buildTtml(
        lyricLines: List<LyricLine>,
        creators: List<String> = emptyList()
    ): String {
        return buildTtmlFromTimingPage(lyricLines, creators)
    }

    fun buildLyricsByFormat(
        format: LyricExportFormat,
        lyricLines: List<LyricLine>,
        showLineEndTime: Boolean = false,
        showDuet: Boolean = true,
        creators: List<String> = emptyList()
    ): String {
        return when (format) {
            LyricExportFormat.LRC_WORD -> buildWordLrc(lyricLines)
            LyricExportFormat.LRC_LINE -> buildLineLrc(lyricLines, showLineEndTime)
            LyricExportFormat.ENHANCED_LRC -> buildEnhancedLrc(lyricLines, showDuet)
            LyricExportFormat.TTML -> buildTtml(lyricLines, creators)
        }
    }

    suspend fun embedLyrics(
        context: Context,
        sourceAudioPath: String,
        lyricsContent: String,
        mediaStoreId: Long = -1L
    ): WriteResult {
        if (sourceAudioPath.isBlank()) {
            return WriteResult(
                success = false,
                errorMessage = "音频路径为空，无法嵌入歌词",
                needPermission = false
            )
        }
        return AudioMetadataReader.writeLyrics(
            context = context,
            filePath = sourceAudioPath,
            lyrics = lyricsContent,
            mediaStoreId = mediaStoreId
        )
    }

    fun saveTtmlFile(audioPath: String, ttmlContent: String): Boolean {
        return saveTtmlToFileFromTimingPage(audioPath, ttmlContent)
    }
}
