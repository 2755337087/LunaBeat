package com.example.LyricBox.lyrics.models

data class LyricsWord(
    val start: Long?,
    val end: Long?,
    val text: String
)

data class LyricsLine(
    val start: Long?,
    val end: Long?,
    val words: List<LyricsWord>
)

typealias LyricsData = List<LyricsLine>

data class Lyrics(
    val tags: Map<String, String> = emptyMap(),
    val orig: LyricsData = emptyList(),
    val ts: LyricsData = emptyList(),
    val roma: LyricsData = emptyList()
)

enum class LyricsType {
    PLAIN_TEXT,
    LINE_BY_LINE,
    VERBATIM
}

enum class Source {
    QM,
    KG,
    NE,
    ITUNES
}

data class SongInfo(
    val source: Source,
    val id: String,
    val mid: String? = null,
    val hash: String? = null,
    val title: String?,
    val subtitle: String? = null,
    val artist: List<String>,
    val album: String? = null,
    val duration: Long? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val discNumber: String? = null,
    val genre: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val lyricist: String? = null,
    val comment: String? = null,
    val copyright: String? = null,
    val coverUrl: String? = null
)

data class LyricInfo(
    val source: Source,
    val id: String,
    val accesskey: String? = null,
    val creator: String? = null,
    val duration: Long? = null,
    val score: Int? = null,
    val songinfo: SongInfo
)

data class SearchResult(
    val songInfo: SongInfo,
    val lyrics: List<LyricInfo> = emptyList()
)

data class VerbatimLyricsResult(
    val source: Source,
    val sourceName: String,
    val lyrics: Lyrics?,
    val error: String? = null,
    val rawTtml: String? = null
)
