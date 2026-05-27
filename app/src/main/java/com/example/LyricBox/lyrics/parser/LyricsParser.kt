package com.example.LyricBox.lyrics.parser

import com.example.LyricBox.lyrics.models.LyricsData
import com.example.LyricBox.lyrics.models.LyricsLine
import com.example.LyricBox.lyrics.models.LyricsWord
import com.example.LyricBox.lyrics.models.LyricsType

object LyricsParser {
    fun judgeLyricsType(lyrics: LyricsData): LyricsType {
        var lyricsType = LyricsType.PLAIN_TEXT
        for (line in lyrics) {
            if (line.words.size > 1) {
                return LyricsType.VERBATIM
            }
            if (line.start != null) {
                lyricsType = LyricsType.LINE_BY_LINE
            }
        }
        return lyricsType
    }
    
    fun plaintext2data(plaintext: String): LyricsData {
        return plaintext.split("\n").map { line ->
            LyricsLine(null, null, listOf(LyricsWord(null, null, line)))
        }
    }
}

object QrcParser {
    private val qrcPattern = Regex("""<Lyric_1 LyricType="1" LyricContent="(.*?)"/>""", RegexOption.DOT_MATCHES_ALL)
    private val tagSplitPattern = Regex("""^\[(\w+):([^\]]*)\]${'$'}""")
    private val lineSplitPattern = Regex("""^\[(\d+),(\d+)\](.*)${'$'}""")
    private val wordSplitPattern = Regex("""(?:\[\d+,\d+\])?(?<content>(?:(?!\(\d+,\d+\)).)*)\((?<start>\d+),(?<duration>\d+)\)""")
    private val wordTimestampPattern = Regex("""^\(\d+,\d+\)${'$'}""")
    
    fun parse(qrcString: String): Pair<Map<String, String>, LyricsData> {
        val qrcMatch = qrcPattern.find(qrcString)
        if (qrcMatch == null || qrcMatch.groupValues[1].isEmpty()) {
            if (qrcString.contains("[") && qrcString.contains("]")) {
                return try {
                    LrcParser.parse(qrcString)
                } catch (e: Exception) {
                    Pair(emptyMap(), emptyList())
                }
            }
            return Pair(emptyMap(), emptyList())
        }
        
        val tags = mutableMapOf<String, String>()
        val lrcList = mutableListOf<LyricsLine>()
        
        for (rawLine in qrcMatch.groupValues[1].split("\n")) {
            val line = rawLine.trim()
            val lineMatch = lineSplitPattern.find(line)
            
            if (lineMatch != null) {
                val lineStart = lineMatch.groupValues[1].toLong()
                val lineDuration = lineMatch.groupValues[2].toLong()
                val lineEnd = lineStart + lineDuration
                val lineContent = lineMatch.groupValues[3]
                
                if (lineContent.startsWith("(") && lineContent.endsWith(")") && wordTimestampPattern.matches(lineContent)) {
                    lrcList.add(LyricsLine(lineStart, lineEnd, emptyList()))
                    continue
                }
                
                val words = mutableListOf<LyricsWord>()
                wordSplitPattern.findAll(lineContent).forEach { wordMatch ->
                    val content = wordMatch.groups["content"]?.value ?: ""
                    if (content != "\r") {
                        val start = wordMatch.groups["start"]?.value?.toLong() ?: 0
                        val duration = wordMatch.groups["duration"]?.value?.toLong() ?: 0
                        words.add(LyricsWord(start, start + duration, content))
                    }
                }
                
                if (words.isEmpty()) {
                    words.add(LyricsWord(lineStart, lineEnd, lineContent))
                }
                
                lrcList.add(LyricsLine(lineStart, lineEnd, words))
            } else {
                val tagSplitContent = tagSplitPattern.find(line)
                if (tagSplitContent != null) {
                    tags[tagSplitContent.groupValues[1]] = tagSplitContent.groupValues[2]
                }
            }
        }
        
        return Pair(tags, lrcList)
    }
}

object KrcParser {
    private val tagSplitPattern = Regex("""^\[(\w+):([^\]]*)\]${'$'}""")
    private val lineSplitPattern = Regex("""^\[(\d+),(\d+)\](.*)${'$'}""")
    private val wordSplitPattern = Regex("""(?:\[\d+,\d+\])?<(?<start>\d+),(?<duration>\d+),\d+>(?<content>(?:.(?!\d+,\d+,\d+>))*)""")
    
    fun parse(krcString: String): Pair<Map<String, String>, Map<String, LyricsData>> {
        val tags = mutableMapOf<String, String>()
        val origList = mutableListOf<LyricsLine>()
        
        for (rawLine in krcString.split("\n")) {
            val line = rawLine.trim()
            if (!line.startsWith("[")) continue
            
            val tagMatch = tagSplitPattern.find(line)
            if (tagMatch != null) {
                tags[tagMatch.groupValues[1]] = tagMatch.groupValues[2]
                continue
            }
            
            val lineMatch = lineSplitPattern.find(line)
            if (lineMatch != null) {
                val lineStart = lineMatch.groupValues[1].toLong()
                val lineDuration = lineMatch.groupValues[2].toLong()
                val lineEnd = lineStart + lineDuration
                val lineContent = lineMatch.groupValues[3]
                
                val words = mutableListOf<LyricsWord>()
                wordSplitPattern.findAll(lineContent).forEach { wordMatch ->
                    val start = wordMatch.groups["start"]?.value?.toLong() ?: 0
                    val duration = wordMatch.groups["duration"]?.value?.toLong() ?: 0
                    val content = wordMatch.groups["content"]?.value ?: ""
                    words.add(LyricsWord(lineStart + start, lineStart + start + duration, content))
                }
                
                if (words.isEmpty()) {
                    words.add(LyricsWord(lineStart, lineEnd, lineContent))
                }
                
                origList.add(LyricsLine(lineStart, lineEnd, words))
            }
        }
        
        val result = mutableMapOf<String, LyricsData>()
        if (origList.isNotEmpty()) result["orig"] = origList
        
        val languageTag = tags["language"]?.trim()
        if (!languageTag.isNullOrEmpty()) {
            try {
                val languageJson = android.util.Base64.decode(languageTag, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
                val languageObj = org.json.JSONObject(languageJson)
                val contentArray = languageObj.optJSONArray("content") ?: return Pair(tags, result)
                
                val romaList = mutableListOf<LyricsLine>()
                val tsList = mutableListOf<LyricsLine>()
                
                for (i in 0 until contentArray.length()) {
                    val languageItem = contentArray.optJSONObject(i) ?: continue
                    val type = languageItem.optInt("type", -1)
                    val lyricContent = languageItem.optJSONArray("lyricContent") ?: continue
                    
                    when (type) {
                        0 -> {
                            var offset = 0
                            for (j in origList.indices) {
                                val origLine = origList[j]
                                if (origLine.words.all { it.text.isBlank() }) {
                                    offset++
                                    continue
                                }
                                
                                val contentIndex = j - offset
                                if (contentIndex >= 0 && contentIndex < lyricContent.length()) {
                                    val wordContents = lyricContent.optJSONArray(contentIndex) ?: continue
                                    val romaWords = mutableListOf<LyricsWord>()
                                    for (k in origLine.words.indices) {
                                        if (k < wordContents.length()) {
                                            val wordText = wordContents.optString(k, "")
                                            romaWords.add(LyricsWord(origLine.words[k].start, origLine.words[k].end, wordText))
                                        }
                                    }
                                    if (romaWords.isNotEmpty()) {
                                        romaList.add(LyricsLine(origLine.start, origLine.end, romaWords))
                                    }
                                }
                            }
                        }
                        1 -> {
                            for (j in origList.indices) {
                                val origLine = origList[j]
                                if (j < lyricContent.length()) {
                                    val lineContents = lyricContent.optJSONArray(j) ?: continue
                                    if (lineContents.length() > 0) {
                                        val translationText = lineContents.optString(0, "")
                                        if (translationText.isNotEmpty()) {
                                            tsList.add(LyricsLine(
                                                origLine.start, 
                                                origLine.end, 
                                                listOf(LyricsWord(origLine.start, origLine.end, translationText))
                                            ))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (romaList.isNotEmpty()) result["roma"] = romaList
                if (tsList.isNotEmpty()) result["ts"] = tsList
            } catch (e: Exception) {
                android.util.Log.e("KrcParser", "解析翻译失败", e)
            }
        }
        
        return Pair(tags, result)
    }
}

object YrcParser {
    private val lineSplitPattern = Regex("""^\[(\d+),(\d+)\](.*)${'$'}""")
    private val wordSplitPattern = Regex("""(?:\[\d+,\d+\])?\((?<start>\d+),(?<duration>\d+),\d+\)(?<content>(?:.(?!\d+,\d+,\d+\)))*)""")
    
    fun parse(yrcString: String): LyricsData {
        val lrcList = mutableListOf<LyricsLine>()
        
        for (rawLine in yrcString.split("\n")) {
            val line = rawLine.trim()
            if (!line.startsWith("[")) continue
            
            val lineMatch = lineSplitPattern.find(line)
            if (lineMatch != null) {
                val lineStart = lineMatch.groupValues[1].toLong()
                val lineDuration = lineMatch.groupValues[2].toLong()
                val lineEnd = lineStart + lineDuration
                val lineContent = lineMatch.groupValues[3]
                
                val words = mutableListOf<LyricsWord>()
                wordSplitPattern.findAll(lineContent).forEach { wordMatch ->
                    val start = wordMatch.groups["start"]?.value?.toLong() ?: 0
                    val duration = wordMatch.groups["duration"]?.value?.toLong() ?: 0
                    val content = wordMatch.groups["content"]?.value ?: ""
                    words.add(LyricsWord(start, start + duration, content))
                }
                
                if (words.isEmpty()) {
                    words.add(LyricsWord(lineStart, lineEnd, lineContent))
                }
                
                lrcList.add(LyricsLine(lineStart, lineEnd, words))
            }
        }
        
        return lrcList
    }
}

object LrcParser {
    private val tagSplitPattern = Regex("""^\[(\w+):([^\]]*)\]${'$'}""")
    private val lineSplitPattern = Regex("""^\[(\d+):(\d+)\.(\d+)\](.*)${'$'}""")
    private val wordSplitPattern = Regex("""((?:(?!\[\d+:\d+\.\d+\]).)*)(?:\[(\d+):(\d+)\.(\d+)\])?""")
    
    fun parse(lrcString: String): Pair<Map<String, String>, LyricsData> {
        val tags = mutableMapOf<String, String>()
        val lrcList = mutableListOf<LyricsLine>()
        
        for (rawLine in lrcString.split("\n")) {
            val line = rawLine.trim()
            if (line.isEmpty() || !line.startsWith("[")) continue
            
            val tagMatch = tagSplitPattern.find(line)
            if (tagMatch != null) {
                tags[tagMatch.groupValues[1]] = tagMatch.groupValues[2]
                continue
            }
            
            val lineMatch = lineSplitPattern.find(line)
            if (lineMatch != null) {
                val m = lineMatch.groupValues[1].toLong()
                val s = lineMatch.groupValues[2].toLong()
                val msStr = lineMatch.groupValues[3]
                val start = time2ms(m, s, msStr)
                val lineContent = lineMatch.groupValues[4]
                
                val words = mutableListOf<LyricsWord>()
                val wordParts = wordSplitPattern.findAll(lineContent).toList()
                
                if (wordParts.isNotEmpty()) {
                    var currentStart = start
                    for ((index, wordPart) in wordParts.withIndex()) {
                        val wordStr = wordPart.groupValues[1]
                        val eM = wordPart.groupValues[2]
                        val eS = wordPart.groupValues[3]
                        val eMs = wordPart.groupValues[4]
                        
                        if (wordStr.isNotEmpty()) {
                            val wordEnd = if (eM.isNotEmpty() && eS.isNotEmpty() && eMs.isNotEmpty()) {
                                time2ms(eM.toLong(), eS.toLong(), eMs)
                            } else null
                            
                            words.add(LyricsWord(currentStart, wordEnd, wordStr))
                            currentStart = wordEnd ?: currentStart
                        }
                    }
                }
                
                if (words.isEmpty()) {
                    words.add(LyricsWord(start, null, lineContent))
                }
                
                lrcList.add(LyricsLine(start, null, words))
            }
        }
        
        return Pair(tags, lrcList)
    }
    
    private fun time2ms(m: Long, s: Long, msStr: String): Long {
        val msValue = msStr.toLong()
        val ms = if (msStr.length <= 2) {
            msValue * 10
        } else {
            msValue
        }
        return m * 60000 + s * 1000 + ms
    }
}

object VerbatimLrcConverter {
    fun toVerbatimLrc(
        lyricsData: LyricsData,
        translation: LyricsData? = null,
        roma: LyricsData? = null
    ): String {
        val sb = StringBuilder()
        
        for (line in lyricsData) {
            if (line.start == null) continue
            
            if (line.words.isEmpty()) {
                continue
            }
            
            val lineStartTime = formatTime(line.start)
            sb.append("[$lineStartTime]")
            
            for (word in line.words) {
                sb.append(word.text)
                if (word.end != null) {
                    sb.append("[${formatTime(word.end)}]")
                }
            }
            
            sb.append("\n")

            roma?.let { romaData ->
                val romaLine = findMatchingTranslationLine(line.start, romaData)
                if (romaLine != null && romaLine.words.isNotEmpty()) {
                    val romaText = romaLine.words.joinToString("") { it.text }.trim()
                    if (romaText.isNotEmpty() && romaText != "//" && romaText != "/" && !romaText.matches(Regex("^/+$"))) {
                        sb.append("[$lineStartTime]")
                        sb.append(romaText)
                        sb.append("\n")
                    }
                }
            }
            
            translation?.let { ts ->
                val tsLine = findMatchingTranslationLine(line.start, ts)
                if (tsLine != null && tsLine.words.isNotEmpty()) {
                    val translationText = tsLine.words.joinToString("") { it.text }.trim()
                    if (translationText.isNotEmpty() && translationText != "//" && translationText != "/" && !translationText.matches(Regex("^/+$"))) {
                        sb.append("[$lineStartTime]")
                        sb.append(translationText)
                        sb.append("\n")
                    }
                }
            }
        }
        
        return sb.toString()
    }
    
    private fun findMatchingTranslationLine(origStart: Long, translation: LyricsData): LyricsLine? {
        val exactMatch = translation.find { it.start == origStart }
        if (exactMatch != null) return exactMatch
        
        val tolerance = 1000L
        val nearbyMatch = translation.filter { it.start != null && kotlin.math.abs(it.start - origStart) <= tolerance }
            .minByOrNull { kotlin.math.abs(it.start!! - origStart) }
        return nearbyMatch
    }
    
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val milliseconds = ms % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }
}
