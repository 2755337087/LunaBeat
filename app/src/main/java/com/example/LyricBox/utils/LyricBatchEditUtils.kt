package com.example.LyricBox.utils

import com.example.LyricBox.LyricLine

object LyricBatchEditUtils {
    private fun linePlainText(line: LyricLine): String {
        return line.timeUnits.joinToString("") { it.text }.trim()
    }

    private fun isLogicalEmptyLine(line: LyricLine): Boolean {
        if (line.timeUnits.isEmpty()) return true
        val text = linePlainText(line)
        return text.isEmpty() || text == "//"
    }

    fun toSimplifiedText(text: String): String {
        return ChineseConverter.toSimplified(text)
    }

    fun hasEmptyLines(lines: List<LyricLine>): Boolean {
        return lines.any { isLogicalEmptyLine(it) }
    }

    fun emptyLineIndices(lines: List<LyricLine>): List<Int> {
        return lines.indices.filter { isLogicalEmptyLine(lines[it]) }
    }

    fun removeEmptyLines(lines: List<LyricLine>): List<LyricLine> {
        return lines.filterNot { isLogicalEmptyLine(it) }
    }

    fun shiftTimestamps(
        lines: List<LyricLine>,
        selectedLineIndices: Set<Int>,
        shiftMs: Long
    ): List<LyricLine> {
        return lines.mapIndexed { index, line ->
            if (index !in selectedLineIndices) {
                line
            } else {
                val newTimeUnits = line.timeUnits.map { unit ->
                    unit.copy(
                        startTime = adjustTime(unit.startTime, shiftMs),
                        endTime = adjustTime(unit.endTime, shiftMs)
                    )
                }
                line.copy(timeUnits = newTimeUnits)
            }
        }
    }

    fun convertToSimplified(
        lines: List<LyricLine>,
        selectedLineIndices: Set<Int>
    ): List<LyricLine> {
        return lines.mapIndexed { index, line ->
            if (index !in selectedLineIndices) {
                line
            } else {
                val newTimeUnits = line.timeUnits.map { unit ->
                    unit.copy(text = toSimplifiedText(unit.text))
                }
                line.copy(
                    timeUnits = newTimeUnits,
                    translation = toSimplifiedText(line.translation)
                )
            }
        }
    }

    fun formatTimeline(lines: List<LyricLine>): List<LyricLine> {
        data class LyricLineWithTime(
            val line: LyricLine,
            val firstTimeMs: Long
        )

        val linesWithTime = lines.mapNotNull { line ->
            line.timeUnits.firstOrNull()?.startTime?.let { startTime ->
                LyricLineWithTime(line, parseTimeToMs(startTime))
            }
        }

        val timeGroupMap = mutableMapOf<Long, MutableList<LyricLine>>()
        for (item in linesWithTime.sortedBy { it.firstTimeMs }) {
            val groupedLines = timeGroupMap.getOrPut(item.firstTimeMs) { mutableListOf() }
            groupedLines.add(item.line)
        }

        val result = mutableListOf<LyricLine>()
        for ((_, groupedLines) in timeGroupMap) {
            if (groupedLines.size == 1) {
                result.add(groupedLines[0])
            } else {
                val mainLine = groupedLines.find { it.timeUnits.size > 1 || it.translation.isEmpty() } ?: groupedLines[0]
                val translationLine = groupedLines.find {
                    it != mainLine && (it.translation.isNotEmpty() || it.timeUnits.size == 1)
                }

                val finalTranslation = if (translationLine != null) {
                    val textFromTranslationLine = translationLine.timeUnits.joinToString("") { it.text }
                    if (textFromTranslationLine.isNotEmpty()) textFromTranslationLine else translationLine.translation
                } else {
                    mainLine.translation
                }

                result.add(mainLine.copy(translation = finalTranslation))
            }
        }

        return result
    }

    fun formatTime(timeMs: Long): String {
        val seconds = (timeMs / 1000) % 60
        val minutes = (timeMs / 60000) % 60
        val milliseconds = timeMs % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }

    fun parseTimeToMs(timeStr: String): Long {
        val parts = timeStr.split(":", ".")
        if (parts.size == 3) {
            val minutes = parts[0].toLongOrNull() ?: 0L
            val seconds = parts[1].toLongOrNull() ?: 0L
            val msStr = parts[2]
            val msValue = msStr.toLongOrNull() ?: 0L
            val milliseconds = if (msStr.length <= 2) {
                msValue * 10
            } else {
                msValue
            }
            return minutes * 60 * 1000 + seconds * 1000 + milliseconds
        }
        return 0L
    }

    fun adjustTime(timeStr: String, shiftMs: Long): String {
        val currentTimeMs = parseTimeToMs(timeStr)
        val newTimeMs = maxOf(0L, currentTimeMs + shiftMs)
        return formatTime(newTimeMs)
    }
}
