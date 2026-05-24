package com.example.LyricBox

import android.content.Context
import java.util.Locale

private const val ARTIST_SPLIT_PREFS_NAME = "MusicLibrarySettings"
private const val PREF_KEY_ARTIST_SPLIT_WHITELIST = "artistSplitWhitelist"
private const val PREF_KEY_FEATURING_ARTIST_KEYWORDS = "featuringArtistKeywords"

object ArtistSplitWhitelistStore {
    fun load(context: Context): List<String> {
        val prefs = context.getSharedPreferences(ARTIST_SPLIT_PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY_ARTIST_SPLIT_WHITELIST, null)
        if (saved != null) {
            return normalize(saved.lineSequence().toList())
        }
        return normalize(prefs.getStringSet(PREF_KEY_ARTIST_SPLIT_WHITELIST, emptySet()).orEmpty())
    }

    fun save(context: Context, artists: Collection<String>) {
        context.getSharedPreferences(ARTIST_SPLIT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_ARTIST_SPLIT_WHITELIST, normalize(artists).joinToString("\n"))
            .apply()
    }

    fun parseInput(input: String): List<String> {
        return normalize(input.lineSequence().toList())
    }

    fun normalize(artists: Iterable<String>): List<String> {
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        artists.forEach { artist ->
            val trimmed = artist.trim()
            if (trimmed.isNotEmpty() && seen.add(trimmed.lowercase(Locale.ROOT))) {
                result.add(trimmed)
            }
        }
        return result
    }

    fun fingerprint(artists: Collection<String>): String {
        return normalize(artists).joinToString("\u001F") { it.lowercase(Locale.ROOT) }
    }
}

object FeaturingArtistKeywordStore {
    val defaultKeywords = listOf("feat", "ft", "featuring")
    private val defaultKeywordSet = defaultKeywords.toSet()

    fun load(context: Context): List<String> {
        val prefs = context.getSharedPreferences(ARTIST_SPLIT_PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY_FEATURING_ARTIST_KEYWORDS, null)
        if (saved != null) {
            return normalize(saved.lineSequence().toList())
        }
        val legacySet = prefs.getStringSet(PREF_KEY_FEATURING_ARTIST_KEYWORDS, null)
        if (legacySet != null) {
            return normalize(legacySet)
        }
        return defaultKeywords
    }

    fun save(context: Context, keywords: Collection<String>) {
        context.getSharedPreferences(ARTIST_SPLIT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_FEATURING_ARTIST_KEYWORDS, normalize(keywords).joinToString("\n"))
            .apply()
    }

    fun parseInput(input: String): List<String> {
        return normalize(input.lineSequence().toList())
    }

    fun normalize(keywords: Iterable<String>): List<String> {
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        keywords.forEach { keyword ->
            val normalized = keyword
                .trim()
                .trim('.')
                .lowercase(Locale.ROOT)
            if (normalized.isNotEmpty() && seen.add(normalized)) {
                result.add(normalized)
            }
        }
        return result
    }

    fun customKeywords(enabledKeywords: Collection<String>): List<String> {
        return normalize(enabledKeywords).filterNot { it in defaultKeywordSet }
    }

    fun fingerprint(keywords: Collection<String>): String {
        return normalize(keywords).joinToString("\u001F")
    }
}

object FeaturingArtistExtractor {
    fun extractArtistsFromTitle(
        title: String,
        artistSplitWhitelist: Collection<String>,
        featuringKeywords: Collection<String>
    ): List<String> {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return emptyList()

        val normalizedKeywords = FeaturingArtistKeywordStore.normalize(featuringKeywords)
        if (normalizedKeywords.isEmpty()) return emptyList()

        val keywordAlternation = normalizedKeywords.joinToString("|") { Regex.escape(it) }
        val bracketPattern =
            Regex("(?i)[(\\[（【]\\s*(?:$keywordAlternation)\\.?\\s+([^)）\\]】]+)")
        val inlinePattern =
            Regex("(?i)(?:^|[\\s\\-–—])(?:$keywordAlternation)\\.?\\s+(.+)$")

        val rawSegments = mutableListOf<String>()
        bracketPattern.findAll(normalizedTitle).forEach { match ->
            val segment = match.groupValues.getOrElse(1) { "" }.trim()
            if (segment.isNotEmpty()) {
                rawSegments.add(segment)
            }
        }
        inlinePattern.find(normalizedTitle)?.let { match ->
            val segment = match.groupValues.getOrElse(1) { "" }.trim()
            if (segment.isNotEmpty()) {
                rawSegments.add(segment)
            }
        }

        if (rawSegments.isEmpty()) return emptyList()

        return rawSegments
            .flatMap { segment ->
                val cleaned = segment.replace(Regex("[)）\\]】\\s]+$"), "").trim()
                ArtistNameSplitter.split(cleaned, artistSplitWhitelist).ifEmpty { listOf(cleaned) }
            }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
    }
}

object ArtistNameSplitter {
    private val separatorChars = setOf('/', '／', '&', ';', '；', ',', '，', '、')

    fun split(raw: String, whitelist: Collection<String> = emptyList()): List<String> {
        val source = raw.trim()
        if (source.isEmpty()) return emptyList()

        val protectedArtists = ArtistSplitWhitelistStore.normalize(whitelist)
            .filter { artist -> artist.any(::isSeparatorChar) }
            .map { artist -> artist to normalizeSeparatorVariants(artist) }
            .sortedWith(
                compareByDescending<Pair<String, String>> { it.second.length }
                    .thenBy { it.second.lowercase(Locale.ROOT) }
            )

        if (protectedArtists.isEmpty()) {
            return splitWithoutWhitelist(source)
        }

        val normalizedSource = normalizeSeparatorVariants(source)
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0

        while (index < source.length) {
            val protectedMatch = protectedArtists.firstOrNull { (_, normalizedArtist) ->
                normalizedArtist.isNotEmpty() &&
                    index + normalizedArtist.length <= normalizedSource.length &&
                    normalizedSource.regionMatches(
                        thisOffset = index,
                        other = normalizedArtist,
                        otherOffset = 0,
                        length = normalizedArtist.length,
                        ignoreCase = true
                    ) &&
                    hasArtistBoundaryBefore(normalizedSource, index) &&
                    hasArtistBoundaryAfter(normalizedSource, index + normalizedArtist.length)
            }

            if (protectedMatch != null) {
                val matchLength = protectedMatch.second.length
                current.append(source.substring(index, index + matchLength))
                index += matchLength
                continue
            }

            val char = source[index]
            if (isSeparatorChar(char)) {
                appendCurrentArtist(result, current)
            } else {
                current.append(char)
            }
            index += 1
        }

        appendCurrentArtist(result, current)
        return result
    }

    private fun splitWithoutWhitelist(source: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        source.forEach { char ->
            if (isSeparatorChar(char)) {
                appendCurrentArtist(result, current)
            } else {
                current.append(char)
            }
        }
        appendCurrentArtist(result, current)
        return result
    }

    private fun appendCurrentArtist(result: MutableList<String>, current: StringBuilder) {
        val artist = current.toString().trim()
        if (artist.isNotEmpty()) {
            result.add(artist)
        }
        current.clear()
    }

    private fun hasArtistBoundaryBefore(text: String, index: Int): Boolean {
        var cursor = index - 1
        while (cursor >= 0 && text[cursor].isWhitespace()) {
            cursor -= 1
        }
        return cursor < 0 || isSeparatorChar(text[cursor])
    }

    private fun hasArtistBoundaryAfter(text: String, index: Int): Boolean {
        var cursor = index
        while (cursor < text.length && text[cursor].isWhitespace()) {
            cursor += 1
        }
        return cursor >= text.length || isSeparatorChar(text[cursor])
    }

    private fun normalizeSeparatorVariants(value: String): String {
        return buildString(value.length) {
            value.forEach { char ->
                append(
                    when (char) {
                        '／' -> '/'
                        '；' -> ';'
                        '，' -> ','
                        else -> char
                    }
                )
            }
        }
    }

    private fun isSeparatorChar(char: Char): Boolean {
        return char in separatorChars
    }
}
