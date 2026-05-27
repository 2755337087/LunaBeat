package com.example.LyricBox

import android.util.Log

enum class LyricParseType {
    SPL_LRC,
    ENHANCED_LRC,
    TTML
}

data class LyricParseResult(
    val lyrics: List<String>,
    val lyricLines: List<LyricLine>
)

data class PlainTextLyricLine(
    val beginTime: String,
    val endTime: String,
    val text: String,
    val translation: String
)

object LyricParsingUtils {
    private val lrcTimestampTagRegex = Regex("""^\[\d{2}:\d{2}\.\d{2,3}]$""")
    private val bracketTagRegex = Regex("""\[[^\[\]]*]""")
    private val bgLineRegex = Regex("""^\[bg:\s*.*]$""", RegexOption.IGNORE_CASE)

    private fun normalizeMillisTo3(rawMillis: String): String {
        return rawMillis.take(3).padEnd(3, '0')
    }

    private fun decodeXmlEntities(text: String): String {
        val withHexEntities = Regex("""&#x([0-9a-fA-F]+);""").replace(text) { match ->
            val codePoint = match.groupValues[1].toIntOrNull(16)
            codePoint?.let { runCatching { String(Character.toChars(it)) }.getOrNull() } ?: match.value
        }
        val withNumericEntities = Regex("""&#(\d+);""").replace(withHexEntities) { match ->
            val codePoint = match.groupValues[1].toIntOrNull()
            codePoint?.let { runCatching { String(Character.toChars(it)) }.getOrNull() } ?: match.value
        }
        return withNumericEntities
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
    }

    private fun stripNonTimestampBracketTags(line: String, keepBgLineTag: Boolean = false): String {
        val trimmed = line.trim()
        if (keepBgLineTag && bgLineRegex.matches(trimmed)) {
            return trimmed
        }
        return bracketTagRegex.replace(line) { match ->
            if (lrcTimestampTagRegex.matches(match.value)) {
                match.value
            } else {
                ""
            }
        }
    }

    private fun splitTranslationLines(value: String): List<String> {
        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun joinTranslationLines(lines: List<String>): String {
        return lines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    private fun mergeTranslationValues(vararg values: String?): String {
        val mergedLines = mutableListOf<String>()
        values.forEach { value ->
            splitTranslationLines(value.orEmpty()).forEach { line ->
                if (line !in mergedLines) {
                    mergedLines.add(line)
                }
            }
        }
        return joinTranslationLines(mergedLines)
    }

    private fun normalizePlainTextSegment(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun hasWrappedBrackets(text: String): Boolean {
        if (text.length < 2) return false
        return (text.startsWith("(") && text.endsWith(")")) ||
            (text.startsWith("（") && text.endsWith("）"))
    }

    private fun formatBackgroundSegment(text: String): String {
        val normalized = normalizePlainTextSegment(text)
        if (normalized.isEmpty()) return ""
        return if (hasWrappedBrackets(normalized)) normalized else "($normalized)"
    }

    private fun joinReadableSegments(first: String, second: String): String {
        val left = normalizePlainTextSegment(first)
        val right = normalizePlainTextSegment(second)
        return when {
            left.isEmpty() -> right
            right.isEmpty() -> left
            else -> "$left $right"
        }
    }

    private fun lineRawText(line: LyricLine): String {
        return line.timeUnits.joinToString(separator = "") { it.text }
    }

    private fun lineBegin(line: LyricLine): String {
        return line.timeUnits.firstOrNull()?.startTime ?: "00:00.000"
    }

    private fun lineEnd(line: LyricLine): String {
        return line.timeUnits.lastOrNull()?.endTime ?: lineBegin(line)
    }

    private fun maxTimestamp(first: String, second: String): String {
        return if (parseTimeToSeconds(first) >= parseTimeToSeconds(second)) first else second
    }

    private fun findBackgroundPartnerIndex(
        lines: List<LyricLine>,
        mainIndex: Int,
        usedBackgroundIndices: Set<Int>
    ): Int? {
        val mainLine = lines[mainIndex]
        val mainKey = mainLine.lineKey
        if (mainKey.isNotEmpty()) {
            val byKey = lines.indices.firstOrNull { candidateIndex ->
                val candidate = lines[candidateIndex]
                candidate.agentType == LyricAgentType.BACKGROUND &&
                    candidate.lineKey == mainKey &&
                    candidate.timeUnits.isNotEmpty() &&
                    !usedBackgroundIndices.contains(candidateIndex)
            }
            if (byKey != null) return byKey
        }

        val nextIndex = mainIndex + 1
        if (nextIndex in lines.indices) {
            val next = lines[nextIndex]
            if (
                next.agentType == LyricAgentType.BACKGROUND &&
                next.timeUnits.isNotEmpty() &&
                !usedBackgroundIndices.contains(nextIndex)
            ) {
                return nextIndex
            }
        }
        return null
    }

    fun buildPureTextLinesFromParsedLyrics(lines: List<LyricLine>): List<PlainTextLyricLine> {
        if (lines.isEmpty()) return emptyList()

        val mergedLines = mutableListOf<PlainTextLyricLine>()
        val usedBackgroundIndices = mutableSetOf<Int>()

        lines.forEachIndexed { index, mainLine ->
            if (mainLine.agentType == LyricAgentType.BACKGROUND) return@forEachIndexed
            if (mainLine.timeUnits.isEmpty()) return@forEachIndexed

            val mainText = normalizePlainTextSegment(lineRawText(mainLine))
            val mainTranslation = joinTranslationLines(splitTranslationLines(mainLine.translation))
            val mainBegin = lineBegin(mainLine)
            val mainEnd = lineEnd(mainLine)

            val bgIndex = findBackgroundPartnerIndex(lines, index, usedBackgroundIndices)
            val bgLine = bgIndex?.let { lines[it] }
            if (bgIndex != null) {
                usedBackgroundIndices.add(bgIndex)
            }

            val bgText = bgLine?.let { normalizePlainTextSegment(lineRawText(it)) }.orEmpty()
            val bgTranslation = bgLine?.let { joinTranslationLines(splitTranslationLines(it.translation)) }.orEmpty()
            val bgBegin = bgLine?.let { lineBegin(it) }.orEmpty()
            val bgEnd = bgLine?.let { lineEnd(it) }.orEmpty()
            val bgBeforeMain = bgBegin.isNotEmpty() && parseTimeToSeconds(bgBegin) < parseTimeToSeconds(mainBegin)

            val bgDisplay = formatBackgroundSegment(bgText)
            val lineText = if (bgBeforeMain) {
                joinReadableSegments(bgDisplay, mainText)
            } else {
                joinReadableSegments(mainText, bgDisplay)
            }
            if (lineText.isEmpty()) return@forEachIndexed

            val bgTranslationDisplay = formatBackgroundSegment(bgTranslation)
            val mainTranslationLines = splitTranslationLines(mainTranslation)
            val mergedTranslation = when {
                mainTranslationLines.isNotEmpty() -> {
                    mainTranslationLines.joinToString("\n") { translationLine ->
                        if (bgTranslationDisplay.isEmpty()) {
                            normalizePlainTextSegment(translationLine)
                        } else if (bgBeforeMain) {
                            joinReadableSegments(bgTranslationDisplay, translationLine)
                        } else {
                            joinReadableSegments(translationLine, bgTranslationDisplay)
                        }
                    }
                }
                bgTranslationDisplay.isNotEmpty() -> bgTranslationDisplay
                else -> ""
            }

            val lineBegin = if (bgBeforeMain) bgBegin else mainBegin
            val lineEnd = if (bgEnd.isNotEmpty()) maxTimestamp(mainEnd, bgEnd) else mainEnd

            mergedLines.add(
                PlainTextLyricLine(
                    beginTime = lineBegin,
                    endTime = lineEnd,
                    text = lineText,
                    translation = mergedTranslation
                )
            )
        }

        return mergedLines
    }

    fun buildPureTextLrcFromParsedLyrics(lines: List<LyricLine>): String {
        val mergedLines = buildPureTextLinesFromParsedLyrics(lines)
        if (mergedLines.isEmpty()) return ""
        return buildString {
            mergedLines.forEach { line ->
                append("[${line.beginTime}]${line.text}")
                append('\n')
                splitTranslationLines(line.translation).forEach { translationLine ->
                    append("[${line.beginTime}]$translationLine")
                    append('\n')
                }
            }
        }.trimEnd()
    }

    private fun extractRoleSpanText(content: String, role: String): String? {
        val pattern = Regex("""<span[^>]*ttm:role="$role"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content) ?: return null
        return decodeXmlEntities(match.groupValues[1].trim()).takeIf { it.isNotEmpty() }
    }

    fun parseByType(type: LyricParseType, content: String): LyricParseResult {
        return when (type) {
            LyricParseType.SPL_LRC -> {
                val (lyrics, lines) = parseSPLLrcLyrics(content)
                LyricParseResult(lyrics, lines)
            }
            LyricParseType.ENHANCED_LRC -> {
                val (lyrics, lines) = parseElrcLyrics(content)
                LyricParseResult(lyrics, lines)
            }
            LyricParseType.TTML -> {
                val lines = parseTtmlLyrics(content)
                val lyrics = lines.map { line -> line.timeUnits.joinToString("") { it.text } }
                LyricParseResult(lyrics, lines)
            }
        }
    }

    fun parseSPLLrcLyrics(content: String): Pair<List<String>, List<LyricLine>> {
        val lines = content.lines()
            .map { stripNonTimestampBracketTags(it) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val lyrics = mutableListOf<String>()
        val lyricLines = mutableListOf<LyricLine>()
        
        // 辅助函数：将时间戳转换为标准的三位毫秒格式
        fun normalizeTimeTag(timeTag: String): String {
            val parts = timeTag.split(":", ".")
            if (parts.size == 3) {
                val m = parts[0]
                val s = parts[1]
                val ms = normalizeMillisTo3(parts[2])
                return "$m:$s.$ms"
            }
            return timeTag
        }
        
        // 先解析所有行
        val parsedLines = lines.map { line ->
            val lineLyrics = StringBuilder()
            val lineTimes = mutableListOf<LyricTimeUnit>()
            var firstTimeTag = ""
            
            val timeTagPattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
            val matches = timeTagPattern.findAll(line)
            val matchList = matches.toList()
            
            if (matchList.isEmpty()) {
                lineLyrics.append(line)
                lineTimes.add(LyricTimeUnit(line, "00:00.000", "00:00.000"))
            } else {
                firstTimeTag = normalizeTimeTag(matchList[0].value.substring(1, matchList[0].value.length - 1))
                
                if (matchList.size == 1 && matchList[0].range.first == 0) {
                    val timeTag = matchList[0].value
                    val startTime = normalizeTimeTag(timeTag.substring(1, timeTag.length - 1))
                    val textAfterTag = line.substring(matchList[0].range.last + 1)
                    
                    if (textAfterTag.isNotEmpty()) {
                        lineLyrics.append(textAfterTag)
                        lineTimes.add(LyricTimeUnit(textAfterTag, startTime, "00:00.000"))
                    }
                } else if (matchList.size == 1 && matchList[0].range.first > 0) {
                    val textBeforeTag = line.substring(0, matchList[0].range.first)
                    val timeTag = matchList[0].value
                    val startTime = normalizeTimeTag(timeTag.substring(1, timeTag.length - 1))
                    val textAfterTag = line.substring(matchList[0].range.last + 1)
                    
                    if (textBeforeTag.isNotEmpty()) {
                        lineLyrics.append(textBeforeTag)
                        lineTimes.add(LyricTimeUnit(textBeforeTag, "00:00.000", "00:00.000"))
                    }
                    
                    if (textAfterTag.isNotEmpty()) {
                        lineLyrics.append(textAfterTag)
                        lineTimes.add(LyricTimeUnit(textAfterTag, startTime, "00:00.000"))
                    }
                } else {
                    var pendingSpaces = ""
                    
                    matchList.forEachIndexed { index, match ->
                        val timeTag = match.value
                        val startTime = normalizeTimeTag(timeTag.substring(1, timeTag.length - 1))
                        
                        val endPos = match.range.last + 1
                        val nextMatch = if (index < matchList.size - 1) matchList[index + 1] else null
                        val nextStartPos = nextMatch?.range?.first ?: line.length
                        
                        val textAfterTag = line.substring(endPos, nextStartPos)
                        
                        if (textAfterTag.isNotEmpty()) {
                            if (textAfterTag.all { it == ' ' }) {
                                pendingSpaces += textAfterTag
                            } else {
                                val fullText = pendingSpaces + textAfterTag
                                lineLyrics.append(fullText)
                                val endTime = if (nextMatch != null) {
                                    normalizeTimeTag(nextMatch.value.substring(1, nextMatch.value.length - 1))
                                } else {
                                    "00:00.000"
                                }
                                lineTimes.add(LyricTimeUnit(fullText, startTime, endTime))
                                pendingSpaces = ""
                            }
                        }
                    }
                }
            }
            
            Triple(lineLyrics.toString(), lineTimes, firstTimeTag)
        }
        
        // 处理翻译匹配
        var i = 0
        while (i < parsedLines.size) {
            val (lineLyric, lineTimes, firstTimeTag) = parsedLines[i]
            
            // 检查下一行是否是翻译
            val translationLines = mutableListOf<String>()
            var nextIndex = i + 1
            while (nextIndex < parsedLines.size) {
                val nextLine = parsedLines[nextIndex]
                val nextFirstTimeTag = nextLine.third
                val nextIsSingleUntimedLine = nextFirstTimeTag.isEmpty() &&
                    nextLine.second.size == 1 &&
                    nextLine.second.first().startTime == "00:00.000" &&
                    nextLine.second.first().endTime == "00:00.000" &&
                    nextLine.first.isNotBlank()
                val canUseAsTranslation = firstTimeTag.isNotEmpty() &&
                    nextLine.second.size == 1 &&
                    (firstTimeTag == nextFirstTimeTag || nextIsSingleUntimedLine)
                if (!canUseAsTranslation) break
                translationLines.add(nextLine.first)
                nextIndex++
            }
            val translation = joinTranslationLines(translationLines)
            i = nextIndex - 1
            
            lyrics.add(lineLyric)
            lyricLines.add(LyricLine(lineTimes, translation, LyricAgentType.LEFT, ""))
            i++
        }
        
        return Pair(lyrics, lyricLines)
    }

    fun parseElrcLyrics(content: String): Pair<List<String>, List<LyricLine>> {
        val lines = content.lines()
            .map { stripNonTimestampBracketTags(it, keepBgLineTag = true) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val lyrics = mutableListOf<String>()
        val lyricLines = mutableListOf<LyricLine>()
        
        // 支持 [mm:ss.xx]/[mm:ss.xxx] 和 <mm:ss.xx>/<mm:ss.xxx>
        val timeTagPattern = Regex("<(\\d{2}):(\\d{2})\\.(\\d{2,3})>")
        val lineTimePattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
        val agentPattern = Regex("^(v1|v2):\\s*")
        val bgPattern = Regex("^\\[bg:\\s*(.*?)\\]\\s*$", RegexOption.DOT_MATCHES_ALL)
        
        data class ParsedLine(
            val lineTime: String,
            val agentType: LyricAgentType,
            val timeUnits: List<LyricTimeUnit>,
            val lineText: String,
            val isTranslation: Boolean
        )
        
        val parsedLines = mutableListOf<ParsedLine>()
        
        for (line in lines) {
            var agentType = LyricAgentType.LEFT
            var processedLine = line
            
            // 检查是否是背景歌词
            val bgMatch = bgPattern.find(processedLine)
            if (bgMatch != null) {
                val bgContent = bgMatch.groupValues[1]
                processedLine = bgContent
                agentType = LyricAgentType.BACKGROUND
            }
            
            val lineTimeMatch = lineTimePattern.find(processedLine)
            val lineTime = if (lineTimeMatch != null) {
                lineTimeMatch.groupValues.let {
                    String.format("%02d:%02d.%s",
                        it[1].toIntOrNull() ?: 0,
                        it[2].toIntOrNull() ?: 0,
                        normalizeMillisTo3(it[3])
                    )
                }
            } else {
                ""
            }
            processedLine = processedLine.replace(lineTimePattern, "")
            
            // 只有非背景歌词才检查 v1/v2
            if (agentType != LyricAgentType.BACKGROUND) {
                val agentMatch = agentPattern.find(processedLine)
                if (agentMatch != null) {
                    val agent = agentMatch.groupValues[1]
                    agentType = if (agent == "v1") LyricAgentType.LEFT else LyricAgentType.RIGHT
                    processedLine = processedLine.removeRange(agentMatch.range)
                }
            }
            
            val timeMatches = timeTagPattern.findAll(processedLine).toList()
            
            if (timeMatches.isEmpty()) {
                val text = processedLine.trim()
                if (text.isNotEmpty()) {
                    parsedLines.add(ParsedLine(
                        lineTime = lineTime,
                        agentType = agentType,
                        timeUnits = listOf(LyricTimeUnit(text, "00:00.000", "00:00.000")),
                        lineText = text,
                        isTranslation = true
                    ))
                }
                continue
            }
            
            val timeUnits = mutableListOf<LyricTimeUnit>()
            val lineText = StringBuilder()
            
            for (i in timeMatches.indices) {
                val currentMatch = timeMatches[i]
                val startTime = currentMatch.groupValues.let {
                    String.format("%02d:%02d.%s",
                        it[1].toIntOrNull() ?: 0,
                        it[2].toIntOrNull() ?: 0,
                        normalizeMillisTo3(it[3])
                    )
                }
                
                val textStart = currentMatch.range.last + 1
                val textEnd = if (i + 1 < timeMatches.size) timeMatches[i + 1].range.first else processedLine.length
                val text = processedLine.substring(textStart, textEnd)
                
                val endTime = if (i + 1 < timeMatches.size) {
                    timeMatches[i + 1].groupValues.let {
                        String.format("%02d:%02d.%s",
                            it[1].toIntOrNull() ?: 0,
                            it[2].toIntOrNull() ?: 0,
                            normalizeMillisTo3(it[3])
                        )
                    }
                } else {
                    "00:00.000"
                }
                
                if (text.isNotEmpty()) {
                    lineText.append(text)
                    timeUnits.add(LyricTimeUnit(text, startTime, endTime))
                }
            }
            
            if (timeUnits.isNotEmpty()) {
                val effectiveLineTime = when {
                    lineTime.isNotEmpty() -> lineTime
                    timeUnits.isNotEmpty() -> timeUnits.first().startTime
                    else -> ""
                }
                parsedLines.add(ParsedLine(
                    lineTime = effectiveLineTime,
                    agentType = agentType,
                    timeUnits = timeUnits,
                    lineText = lineText.toString(),
                    isTranslation = false
                ))
            }
        }
        
        var i = 0
        while (i < parsedLines.size) {
            val currentLine = parsedLines[i]
            
            if (currentLine.isTranslation) {
                i++
                continue
            }
            
            val translationLines = mutableListOf<String>()
            var nextIndex = i + 1
            while (nextIndex < parsedLines.size) {
                val nextLine = parsedLines[nextIndex]
                val canUseAsTranslation = nextLine.isTranslation &&
                    currentLine.lineTime.isNotEmpty() &&
                    currentLine.lineTime == nextLine.lineTime
                if (!canUseAsTranslation) break
                translationLines.add(nextLine.lineText)
                nextIndex++
            }
            val translation = joinTranslationLines(translationLines)
            i = nextIndex - 1
            
            lyrics.add(currentLine.lineText)
            lyricLines.add(LyricLine(currentLine.timeUnits, translation, currentLine.agentType, ""))
            i++
        }
        
        return Pair(lyrics, lyricLines)
    }

    fun parseTtmlTime(timeStr: String): String {
        val input = timeStr.trim()
        if (input.isEmpty()) return "00:00.000"

        fun parseMillis(raw: String?): Int {
            if (raw.isNullOrEmpty()) return 0
            return raw.take(3).padEnd(3, '0').toIntOrNull() ?: 0
        }

        // 支持 HH:MM:SS(.SSS)
        val hmsMatch = Regex("""^(\d+):(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?$""").matchEntire(input)
        if (hmsMatch != null) {
            val hours = hmsMatch.groupValues[1].toIntOrNull() ?: 0
            val minutes = hmsMatch.groupValues[2].toIntOrNull() ?: 0
            val seconds = hmsMatch.groupValues[3].toIntOrNull() ?: 0
            val ms = parseMillis(hmsMatch.groupValues.getOrNull(4))
            return String.format("%02d:%02d.%03d", hours * 60 + minutes, seconds, ms)
        }

        // 支持 MM:SS(.SSS)
        val msMatch = Regex("""^(\d+):(\d{1,2})(?:\.(\d{1,3}))?$""").matchEntire(input)
        if (msMatch != null) {
            val minutes = msMatch.groupValues[1].toIntOrNull() ?: 0
            val seconds = msMatch.groupValues[2].toIntOrNull() ?: 0
            val ms = parseMillis(msMatch.groupValues.getOrNull(3))
            return String.format("%02d:%02d.%03d", minutes, seconds, ms)
        }

        // 兼容纯秒格式（如 24.81）
        val totalSeconds = input.toDoubleOrNull() ?: return "00:00.000"
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        val ms = ((totalSeconds * 1000) % 1000).toInt()
        return String.format("%02d:%02d.%03d", minutes, seconds, ms)
    }

    fun parseSongwritersFromTtml(content: String): List<String> {
        val songwriters = mutableListOf<String>()
        
        Log.d("LyricTiming", "parseSongwritersFromTtml: 开始解析，内容长度=${content.length}")
        
        // 检查内容中是否包含 songwriters 关键字
        val containsSongwriters = content.contains("songwriters", ignoreCase = false)
        val containsSongwriter = content.contains("songwriter", ignoreCase = false)
        Log.d("LyricTiming", "内容中是否包含 'songwriters': $containsSongwriters")
        Log.d("LyricTiming", "内容中是否包含 'songwriter': $containsSongwriter")
        
        // 如果包含，打印相关部分的内容
        if (containsSongwriters || containsSongwriter) {
            val songwritersIndex = content.indexOf("songwriters")
            if (songwritersIndex >= 0) {
                val startIndex = maxOf(0, songwritersIndex - 50)
                val endIndex = minOf(content.length, songwritersIndex + 200)
                Log.d("LyricTiming", "songwriters 附近的内容: ${content.substring(startIndex, endIndex)}")
            }
        }
        
        // 直接在整个内容中查找 songwriters 标签
        val songwritersPattern = Regex("<songwriters>(.*?)</songwriters>", RegexOption.DOT_MATCHES_ALL)
        val songwritersMatch = songwritersPattern.find(content)
        
        Log.d("LyricTiming", "SongwritersMatch: ${songwritersMatch != null}")
        
        if (songwritersMatch != null) {
            val songwritersContent = songwritersMatch.groupValues[1]
            Log.d("LyricTiming", "SongwritersContent: $songwritersContent")
            
            // 找所有songwriter标签
            val songwriterPattern = Regex("<songwriter>(.*?)</songwriter>", RegexOption.DOT_MATCHES_ALL)
            val songwriterMatches = songwriterPattern.findAll(songwritersContent)
            
            for (match in songwriterMatches) {
                val creator = decodeXmlEntities(match.groupValues[1].trim())
                if (creator.isNotEmpty()) {
                    songwriters.add(creator)
                    Log.d("LyricTiming", "找到创作者: $creator")
                }
            }
        }
        
        Log.d("LyricTiming", "最终找到的创作者数量: ${songwriters.size}, 内容: $songwriters")
        
        return songwriters
    }

    fun parseTtmlLyrics(content: String): List<LyricLine> {
        val mainTranslations = mutableMapOf<String, String>()
        val bgTranslations = mutableMapOf<String, String>()
        val transliterations = mutableMapOf<String, List<Triple<String, String, String>>>() // key: lineKey, value: list of (begin, end, text)
        val bgTransliterations = mutableMapOf<String, List<Triple<String, String, String>>>() // key: lineKey, value: list of (begin, end, text) for background
        
        // 先在原始内容中解析翻译标签和注音标签，避免预处理破坏结构
        val translationPattern = Regex("<translation([^>]*)>(.*?)</translation>", RegexOption.DOT_MATCHES_ALL)
        val translationMatch = translationPattern.find(content)
        
        // 解析注音标签
        val transliterationPattern = Regex("<transliteration([^>]*)>(.*?)</transliteration>", RegexOption.DOT_MATCHES_ALL)
        val transliterationMatch = transliterationPattern.find(content)
        if (transliterationMatch != null) {
            val transliterationContent = transliterationMatch.groupValues[2]
            val textPattern = Regex("<text for=\"([^\"]+)\">(.*?)</text>", RegexOption.DOT_MATCHES_ALL)
            val textMatches = textPattern.findAll(transliterationContent).toList()
            
            for (textMatch in textMatches) {
                val key = textMatch.groupValues[1]
                var fullText = textMatch.groupValues[2]
                
                // 分离主注音和背景注音
                val bgStartPattern = Regex("""<span[^>]*ttm:role="x-bg"[^>]*>""")
                val bgStartMatch = bgStartPattern.find(fullText)
                
                if (bgStartMatch != null) {
                    // 有背景注音，需要分离出来
                    val bgStartPos = bgStartMatch.range.first
                    val afterBgStart = fullText.substring(bgStartMatch.range.last + 1)
                    
                    val closeSpanPattern = Regex("</span>")
                    var depth = 1
                    var searchPos = 0
                    var bgTransText = ""
                    
                    while (searchPos < afterBgStart.length && depth > 0) {
                        val remaining = afterBgStart.substring(searchPos)
                        val nextOpen = Regex("<span[^>]*>").find(remaining)
                        val nextClose = closeSpanPattern.find(remaining)
                        
                        val openRelPos = nextOpen?.range?.first ?: Int.MAX_VALUE
                        val closeRelPos = nextClose?.range?.first ?: Int.MAX_VALUE
                        
                        if (closeRelPos < openRelPos) {
                            depth--
                            if (depth == 0) {
                                bgTransText = afterBgStart.substring(0, searchPos + closeRelPos)
                                fullText = fullText.substring(0, bgStartPos) + 
                                           afterBgStart.substring(searchPos + nextClose!!.range.last + 1)
                            } else {
                                searchPos += nextClose!!.range.last + 1
                            }
                        } else if (openRelPos < closeRelPos) {
                            depth++
                            searchPos += nextOpen!!.range.last + 1
                        } else {
                            break
                        }
                    }
                    
                    // 解析背景注音
                    val bgSpanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
                    val bgSpanMatches = bgSpanPattern.findAll(bgTransText).toList()
                    
                    val bgSpans = mutableListOf<Triple<String, String, String>>()
                    for (spanMatch in bgSpanMatches) {
                        val spanTag = spanMatch.groups[0]!!.value.substringBefore(">")
                        var spanContent = decodeXmlEntities(spanMatch.groups[1]!!.value)
                        
                        val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                        val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                        
                        val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                        val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                        
                        // 去掉括号
                        spanContent = spanContent.removePrefix("(").removeSuffix(")")
                        
                        bgSpans.add(Triple(beginTime, endTime, spanContent))
                    }
                    
                    if (bgSpans.isNotEmpty()) {
                        bgTransliterations[key] = bgSpans
                    }
                }
                
                // 解析主注音
                val spanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
                val spanMatches = spanPattern.findAll(fullText).toList()
                
                val spans = mutableListOf<Triple<String, String, String>>()
                for (spanMatch in spanMatches) {
                    val spanTag = spanMatch.groups[0]!!.value.substringBefore(">")
                    val spanContent = decodeXmlEntities(spanMatch.groups[1]!!.value)
                    
                    val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                    val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                    
                    val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                    val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                    
                    spans.add(Triple(beginTime, endTime, spanContent))
                }
                
                if (spans.isNotEmpty()) {
                    transliterations[key] = spans
                }
            }
        }
        if (translationMatch != null) {
            val translationAttrs = translationMatch.groupValues[1]
            val translationContent = translationMatch.groupValues[2]
            val isReplacementType = translationAttrs.contains("type=\"replacement\"")
            
            val textPattern = Regex("<text for=\"([^\"]+)\">(.*?)</text>", RegexOption.DOT_MATCHES_ALL)
            val textMatches = textPattern.findAll(translationContent).toList()
            
            for (textMatch in textMatches) {
                val key = textMatch.groupValues[1]
                val fullText = textMatch.groupValues[2]
                
                // 检测是否包含带时间戳的 span 标签（有 begin 属性）
                val hasTimestampSpans = fullText.contains(Regex("""<span[^>]*begin="[^"]+""""))
                
                if (isReplacementType || hasTimestampSpans) {
                    // type="replacement" 格式或包含时间戳 span：直接移除所有 XML 标签，保留纯文本和空格
                    val plainText = decodeXmlEntities(fullText.replace(Regex("<[^>]+>"), "").trim())
                    mainTranslations[key] = plainText
                } else {
                    // 普通格式
                    // 提取主翻译（去掉span标签及其内容）
                    val bgSpanPattern = Regex("<span[^>]*ttm:role=\"x-bg\"[^>]*>.*?</span>", RegexOption.DOT_MATCHES_ALL)
                    val mainText = decodeXmlEntities(bgSpanPattern.replace(fullText, "").trim())
                    mainTranslations[key] = mainText
                    
                    // 提取背景翻译
                    val bgSpanContentPattern = Regex("<span[^>]*ttm:role=\"x-bg\"[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
                    val bgSpanMatch = bgSpanContentPattern.find(fullText)
                    if (bgSpanMatch != null) {
                        val bgText = decodeXmlEntities(bgSpanMatch.groupValues[1].trim().removePrefix("(").removeSuffix(")"))
                        bgTranslations[key] = bgText
                    }
                }
            }
        }
        
        // 处理歌词内容时才进行预处理
        var processedContent = content
            .replace("\n", "")
            .replace("\r", "")
            .replace("  ", "")
        
        val lyricLines = mutableListOf<LyricLine>()
        
        val pPattern = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
        val pMatches = pPattern.findAll(processedContent).toList()
        
        val uniqueAgents = mutableSetOf<String>()
        for (pMatch in pMatches) {
            val pTag = pMatch.groups[0]!!.value
            val agentMatch = Regex("""ttm:agent="([^"]+)"""").find(pTag)
            if (agentMatch != null) {
                uniqueAgents.add(agentMatch.groupValues[1])
            }
        }
        
        val agentList = uniqueAgents.sorted()
        
        for (pMatch in pMatches) {
            val pTag = pMatch.groups[0]!!.value
            var pContent = pMatch.groups[1]!!.value
            
            var agentType = LyricAgentType.LEFT
            
            val agentMatch = Regex("""ttm:agent="([^"]+)"""").find(pTag)
            if (agentMatch != null) {
                val agent = agentMatch.groupValues[1]
                if (agentList.size == 2) {
                    if (agent == agentList[1]) {
                        agentType = LyricAgentType.RIGHT
                    } else {
                        agentType = LyricAgentType.LEFT
                    }
                } else {
                    if (agent == "v2") {
                        agentType = LyricAgentType.RIGHT
                    } else {
                        agentType = LyricAgentType.LEFT
                    }
                }
            }
            
            var lineKey = ""
            val keyMatch = Regex("""itunes:key="([^"]+)"""").find(pTag)
            if (keyMatch != null) {
                lineKey = keyMatch.groupValues[1]
            }
            
            val beginMatchP = Regex("""begin="([^"]+)"""").find(pTag)
            val endMatchP = Regex("""end="([^"]+)"""").find(pTag)
            val beginTimeP = if (beginMatchP != null) parseTtmlTime(beginMatchP.groupValues[1]) else "00:00.000"
            val endTimeP = if (endMatchP != null) parseTtmlTime(endMatchP.groupValues[1]) else "00:00.000"
            
            var mainTranslation = ""
            var bgTranslation = ""
            val mainTimeUnits = mutableListOf<LyricTimeUnit>()
            val bgTimeUnits = mutableListOf<LyricTimeUnit>()
            
            val bgMarker = "___BG_CONTENT_MARKER___"
            var bgContent = ""
            
            val bgStartPattern = Regex("""<span[^>]*ttm:role="x-bg"[^>]*>""")
            val bgStartMatch = bgStartPattern.find(pContent)
            
            if (bgStartMatch != null) {
                val bgStartPos = bgStartMatch.range.first
                val afterBgStart = pContent.substring(bgStartMatch.range.last + 1)
                
                val closeSpanPattern = Regex("</span>")
                var depth = 1
                var searchPos = 0
                
                while (searchPos < afterBgStart.length && depth > 0) {
                    val remaining = afterBgStart.substring(searchPos)
                    val nextOpen = Regex("<span[^>]*>").find(remaining)
                    val nextClose = closeSpanPattern.find(remaining)
                    
                    val openRelPos = nextOpen?.range?.first ?: Int.MAX_VALUE
                    val closeRelPos = nextClose?.range?.first ?: Int.MAX_VALUE
                    
                    if (closeRelPos < openRelPos) {
                        depth--
                        if (depth == 0) {
                            bgContent = afterBgStart.substring(0, searchPos + closeRelPos)
                            pContent = pContent.substring(0, bgStartPos) + bgMarker + 
                                       afterBgStart.substring(searchPos + nextClose!!.range.last + 1)
                        } else {
                            searchPos += nextClose!!.range.last + 1
                        }
                    } else if (openRelPos < closeRelPos) {
                        depth++
                        searchPos += nextOpen!!.range.last + 1
                    } else {
                        break
                    }
                }
            }
            
            val inlineMainTranslation = extractRoleSpanText(pContent, "x-translation")
            val inlineMainRoman = extractRoleSpanText(pContent, "x-roman")
            mainTranslation = mergeTranslationValues(inlineMainTranslation, inlineMainRoman)
            
            val mainContentWithoutTrans = pContent
                .replace(Regex("""<span[^>]*ttm:role="x-translation"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("""<span[^>]*ttm:role="x-roman"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(bgMarker, "")
            
            val mainSpanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
            val mainSpanMatches = mainSpanPattern.findAll(mainContentWithoutTrans).toList()
            
            val spanFullPattern = Regex("<span[^>]*>.*?</span>", RegexOption.DOT_MATCHES_ALL)
            val spanFullMatches = spanFullPattern.findAll(mainContentWithoutTrans).toList()
            
            if (mainSpanMatches.isNotEmpty()) {
                var pendingSpaces = ""
                
                for ((idx, spanMatch) in mainSpanMatches.withIndex()) {
                    val spanFullTag = spanFullMatches[idx].groups[0]!!.value
                    val spanTag = spanFullTag.substringBefore(">")
                    val spanContent = decodeXmlEntities(spanMatch.groups[1]!!.value)
                    
                    val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                    val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                    
                    val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                    val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                    
                    if (spanContent.isNotEmpty()) {
                        val text = pendingSpaces + spanContent
                        mainTimeUnits.add(LyricTimeUnit(text, beginTime, endTime))
                        pendingSpaces = ""
                    }
                    
                    if (idx < spanFullMatches.size - 1) {
                        val currentEnd = spanFullMatches[idx].range.last + 1
                        val nextStart = spanFullMatches[idx + 1].range.first
                        if (nextStart > currentEnd) {
                            val betweenText = mainContentWithoutTrans.substring(currentEnd, nextStart)
                            if (betweenText.all { it == ' ' }) {
                                pendingSpaces = betweenText
                            }
                        }
                    }
                }
            } else {
                val plainText = decodeXmlEntities(mainContentWithoutTrans.trim())
                if (plainText.isNotEmpty()) {
                    mainTimeUnits.add(LyricTimeUnit(plainText, beginTimeP, endTimeP))
                }
            }
            
            // 为当前行的歌词单元分配注音
            val lineTransliterations = transliterations[lineKey]
            if (lineTransliterations != null) {
                val updatedUnits = mutableListOf<LyricTimeUnit>()
                for (unit in mainTimeUnits) {
                    val unitBeginMs = parseTimeToMs(unit.startTime)
                    val unitEndMs = parseTimeToMs(unit.endTime)
                    
                    // 查找时间匹配的注音
                    var matchedTransliteration = ""
                    val charTransliterations = mutableMapOf<Int, String>()
                    val matchingTransliterations = mutableListOf<Triple<String, String, String>>()
                    
                    // 收集所有与这个单元时间匹配的注音
                    for (transInfo in lineTransliterations) {
                        val (transBegin, transEnd, transText) = transInfo
                        val transBeginMs = parseTimeToMs(transBegin)
                        val transEndMs = parseTimeToMs(transEnd)
                        
                        // 检查时间重叠
                        if (transBeginMs < unitEndMs && transEndMs > unitBeginMs) {
                            matchingTransliterations.add(transInfo)
                        }
                    }
                    
                    // 统计CJK字符数量
                    val cjkIndices = unit.text.mapIndexedNotNull { idx, char -> 
                        if (isCJKCharacter(char)) idx else null 
                    }
                    
                    // 尝试将注音按时间位置映射到字符索引，避免前导空格/假名导致顺序错位
                    if (cjkIndices.isNotEmpty() && matchingTransliterations.isNotEmpty()) {
                        val sortedMatchingTransliterations = matchingTransliterations.sortedBy { parseTimeToMs(it.first) }
                        val textLength = unit.text.length
                        if (textLength > 0) {
                            val unitDuration = (unitEndMs - unitBeginMs).coerceAtLeast(1L)
                            val charDuration = unitDuration.toDouble() / textLength.toDouble()
                            val remainingIndices = cjkIndices.toMutableSet()

                            for ((transBegin, transEnd, transText) in sortedMatchingTransliterations) {
                                if (transText.isEmpty()) continue
                                val transBeginMs = parseTimeToMs(transBegin)
                                val transEndMs = parseTimeToMs(transEnd)
                                val midMs = ((transBeginMs + transEndMs) / 2L).coerceIn(unitBeginMs, unitEndMs)
                                val estimatedIdx = (((midMs - unitBeginMs) / charDuration).toInt())
                                    .coerceIn(0, textLength - 1)

                                val chosenIdx = if (estimatedIdx in remainingIndices) {
                                    estimatedIdx
                                } else {
                                    remainingIndices.minByOrNull { idx -> kotlin.math.abs(idx - estimatedIdx) }
                                }

                                if (chosenIdx != null) {
                                    charTransliterations[chosenIdx] = transText
                                    remainingIndices.remove(chosenIdx)
                                }
                            }
                        }

                        // 如果没有成功分配到单字符，使用整体注音
                        if (charTransliterations.isEmpty()) {
                            matchedTransliteration = sortedMatchingTransliterations[0].third
                        }
                    } else if (matchingTransliterations.isNotEmpty()) {
                        // 没有CJK字符，使用整体注音
                        matchedTransliteration = matchingTransliterations[0].third
                    }
                    
                    updatedUnits.add(unit.copy(transliteration = matchedTransliteration, charTransliterations = charTransliterations))
                }
                mainTimeUnits.clear()
                mainTimeUnits.addAll(updatedUnits)
            }
            
            if (bgContent.isNotEmpty()) {
                val inlineBgTranslation = extractRoleSpanText(bgContent, "x-translation")
                val inlineBgRoman = extractRoleSpanText(bgContent, "x-roman")
                bgTranslation = mergeTranslationValues(inlineBgTranslation, inlineBgRoman)
                
                val bgContentWithoutTrans = bgContent
                    .replace(Regex("""<span[^>]*ttm:role="x-translation"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
                    .replace(Regex("""<span[^>]*ttm:role="x-roman"[^>]*>.*?</span>""", RegexOption.DOT_MATCHES_ALL), "")
                
                val bgSpanMatches = mainSpanPattern.findAll(bgContentWithoutTrans).toList()
                val bgSpanFullMatches = spanFullPattern.findAll(bgContentWithoutTrans).toList()
                
                if (bgSpanMatches.isNotEmpty()) {
                    var pendingSpaces = ""
                    
                    for ((idx, spanMatch) in bgSpanMatches.withIndex()) {
                        val spanFullTag = bgSpanFullMatches[idx].groups[0]!!.value
                        val spanTag = spanFullTag.substringBefore(">")
                        var spanContent = decodeXmlEntities(spanMatch.groups[1]!!.value)
                        
                        val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
                        val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
                        
                        val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
                        val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
                        
                        spanContent = spanContent.removePrefix("(").removeSuffix(")")
                        
                        if (spanContent.isNotEmpty()) {
                            val text = pendingSpaces + spanContent
                            bgTimeUnits.add(LyricTimeUnit(text, beginTime, endTime))
                            pendingSpaces = ""
                        }
                        
                        if (idx < bgSpanFullMatches.size - 1) {
                            val currentEnd = bgSpanFullMatches[idx].range.last + 1
                            val nextStart = bgSpanFullMatches[idx + 1].range.first
                            if (nextStart > currentEnd) {
                                val betweenText = bgContentWithoutTrans.substring(currentEnd, nextStart)
                                if (betweenText.all { it == ' ' }) {
                                    pendingSpaces = betweenText
                                }
                            }
                        }
                    }
                }
                
                // 为当前行的背景歌词单元分配注音
                val lineBgTransliterations = bgTransliterations[lineKey]
                if (lineBgTransliterations != null) {
                    val updatedBgUnits = mutableListOf<LyricTimeUnit>()
                    for (unit in bgTimeUnits) {
                        val unitBeginMs = parseTimeToMs(unit.startTime)
                        val unitEndMs = parseTimeToMs(unit.endTime)
                        
                        var matchedTransliteration = ""
                        val charTransliterations = mutableMapOf<Int, String>()
                        val matchingTransliterations = mutableListOf<Triple<String, String, String>>()
                        
                        for (transInfo in lineBgTransliterations) {
                            val (transBegin, transEnd, transText) = transInfo
                            val transBeginMs = parseTimeToMs(transBegin)
                            val transEndMs = parseTimeToMs(transEnd)
                            
                            if (transBeginMs < unitEndMs && transEndMs > unitBeginMs) {
                                matchingTransliterations.add(transInfo)
                            }
                        }
                        
                        val cjkIndices = unit.text.mapIndexedNotNull { idx, char ->
                            if (isCJKCharacter(char)) idx else null
                        }
                        
                        if (cjkIndices.isNotEmpty() && matchingTransliterations.isNotEmpty()) {
                            val sortedMatchingTransliterations = matchingTransliterations.sortedBy { parseTimeToMs(it.first) }
                            val textLength = unit.text.length
                            if (textLength > 0) {
                                val unitDuration = (unitEndMs - unitBeginMs).coerceAtLeast(1L)
                                val charDuration = unitDuration.toDouble() / textLength.toDouble()
                                val remainingIndices = cjkIndices.toMutableSet()

                                for ((transBegin, transEnd, transText) in sortedMatchingTransliterations) {
                                    if (transText.isEmpty()) continue
                                    val transBeginMs = parseTimeToMs(transBegin)
                                    val transEndMs = parseTimeToMs(transEnd)
                                    val midMs = ((transBeginMs + transEndMs) / 2L).coerceIn(unitBeginMs, unitEndMs)
                                    val estimatedIdx = (((midMs - unitBeginMs) / charDuration).toInt())
                                        .coerceIn(0, textLength - 1)

                                    val chosenIdx = if (estimatedIdx in remainingIndices) {
                                        estimatedIdx
                                    } else {
                                        remainingIndices.minByOrNull { idx -> kotlin.math.abs(idx - estimatedIdx) }
                                    }

                                    if (chosenIdx != null) {
                                        charTransliterations[chosenIdx] = transText
                                        remainingIndices.remove(chosenIdx)
                                    }
                                }
                            }

                            if (charTransliterations.isEmpty()) {
                                matchedTransliteration = sortedMatchingTransliterations[0].third
                            }
                        } else if (matchingTransliterations.isNotEmpty()) {
                            matchedTransliteration = matchingTransliterations[0].third
                        }
                        
                        updatedBgUnits.add(unit.copy(transliteration = matchedTransliteration, charTransliterations = charTransliterations))
                    }
                    bgTimeUnits.clear()
                    bgTimeUnits.addAll(updatedBgUnits)
                }
            }
            
            if (mainTimeUnits.isNotEmpty()) {
                // 优先使用从顶部翻译标签解析出的翻译
                val finalMainTranslation = mergeTranslationValues(mainTranslations[lineKey], mainTranslation)
                lyricLines.add(LyricLine(
                    mainTimeUnits,
                    finalMainTranslation,
                    agentType,
                    lineKey
                ))
            }
            
            if (bgTimeUnits.isNotEmpty()) {
                // 优先使用从顶部翻译标签解析出的背景翻译
                val finalBgTranslation = mergeTranslationValues(bgTranslations[lineKey], bgTranslation)
                lyricLines.add(LyricLine(
                    bgTimeUnits,
                    finalBgTranslation,
                    LyricAgentType.BACKGROUND,
                    lineKey
                ))
            }
        }
        
        return lyricLines
    }

    fun parseTimeToSeconds(timeStr: String): Double {
        val parts = timeStr.split(":", ".")
        return if (parts.size == 3) {
            val minutes = parts[0].toDoubleOrNull() ?: 0.0
            val seconds = parts[1].toDoubleOrNull() ?: 0.0
            val ms = parts[2].toDoubleOrNull() ?: 0.0
            minutes * 60 + seconds + ms / 1000
        } else if (parts.size == 2) {
            val minutes = parts[0].toDoubleOrNull() ?: 0.0
            val seconds = parts[1].toDoubleOrNull() ?: 0.0
            minutes * 60 + seconds
        } else {
            0.0
        }
    }

    fun parseSpanContent(content: String): List<LyricTimeUnit> {
        val timeUnits = mutableListOf<LyricTimeUnit>()
        val spanPattern = Regex("<span[^>]*>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
        val spanMatches = spanPattern.findAll(content).toList()
        
        for (spanMatch in spanMatches) {
            val spanTag = spanMatch.groups[0]!!.value.substringBefore(">")
            val spanContent = decodeXmlEntities(spanMatch.groups[1]!!.value)
            
            val beginMatch = Regex("""begin="([^"]+)"""").find(spanTag)
            val endMatch = Regex("""end="([^"]+)"""").find(spanTag)
            
            val beginTime = if (beginMatch != null) parseTtmlTime(beginMatch.groupValues[1]) else "00:00.000"
            val endTime = if (endMatch != null) parseTtmlTime(endMatch.groupValues[1]) else "00:00.000"
            
            if (spanContent.isNotEmpty()) {
                timeUnits.add(LyricTimeUnit(spanContent, beginTime, endTime))
            }
        }
        
        if (timeUnits.isEmpty()) {
            val text = decodeXmlEntities(content.replace(Regex("<[^>]+>"), "").trim())
            if (text.isNotEmpty()) {
                timeUnits.add(LyricTimeUnit(text, "00:00.000", "00:00.000"))
            }
        }
        
        return timeUnits
    }
}
