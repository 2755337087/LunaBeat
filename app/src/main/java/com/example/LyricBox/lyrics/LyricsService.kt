package com.example.LyricBox.lyrics

import android.util.Log
import com.example.LyricBox.lyrics.api.ITunesApi
import com.example.LyricBox.lyrics.api.KugouApi
import com.example.LyricBox.lyrics.api.NeteaseApi
import com.example.LyricBox.lyrics.api.QQMusicApi
import com.example.LyricBox.lyrics.models.Source
import com.example.LyricBox.lyrics.models.SongInfo
import com.example.LyricBox.lyrics.models.VerbatimLyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class LyricsService {
    companion object {
        private const val TAG = "LyricsService"
    }
    
    private val qqMusicApi = QQMusicApi()
    private val kugouApi = KugouApi()
    private val neteaseApi = NeteaseApi()
    private val itunesApi = ITunesApi()
    
    suspend fun getMusicDetail(songInfo: SongInfo, itunesCountry: String = "HK", itunesConvertToSimplified: Boolean = false, itunesCoverSize: Int = 1000, qmCoverSize: Int = 1000, neCoverSize: Int = 1000): SongInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取歌曲详情: ${songInfo.title} from ${songInfo.source}")
                val result = when (songInfo.source) {
                    Source.QM -> qqMusicApi.getMusicDetail(songInfo, coverSize = qmCoverSize)
                    Source.KG -> null
                    Source.NE -> neteaseApi.getMusicDetail(songInfo, coverSize = neCoverSize)
                    Source.ITUNES -> itunesApi.getMusicDetail(songInfo, country = itunesCountry, convertToSimplified = itunesConvertToSimplified, coverSize = itunesCoverSize)
                }
                Log.d(TAG, "获取详情结果: ${result != null}")
                result
            } catch (e: Exception) {
                Log.e(TAG, "获取歌曲详情失败", e)
                null
            }
        }
    }
    
    suspend fun searchAllSources(keyword: String, itunesCountry: String = "HK", itunesConvertToSimplified: Boolean = false, itunesCoverSize: Int = 1000, qmCoverSize: Int = 1000, neCoverSize: Int = 1000): Map<Source, List<SongInfo>> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<Source, List<SongInfo>>()
            
            val deferredQM = async { 
                try {
                    Log.d(TAG, "QQ音乐搜索: $keyword, 封面尺寸: $qmCoverSize")
                    val result = qqMusicApi.search(keyword, coverSize = qmCoverSize)
                    Log.d(TAG, "QQ音乐搜索结果: ${result.size}首")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "QQ音乐搜索失败", e)
                    emptyList()
                }
            }
            
            val deferredNE = async {
                try {
                    Log.d(TAG, "网易云搜索: $keyword, 封面尺寸: $neCoverSize")
                    val result = neteaseApi.search(keyword, coverSize = neCoverSize)
                    Log.d(TAG, "网易云搜索结果: ${result.size}首")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "网易云搜索失败", e)
                    emptyList()
                }
            }
            
            val deferredITUNES = async {
                try {
                    Log.d(TAG, "iTunes搜索: $keyword, 国家: $itunesCountry, 转简体: $itunesConvertToSimplified, 封面尺寸: $itunesCoverSize")
                    val result = itunesApi.search(keyword, country = itunesCountry, convertToSimplified = itunesConvertToSimplified, coverSize = itunesCoverSize)
                    Log.d(TAG, "iTunes搜索结果: ${result.size}首")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "iTunes搜索失败", e)
                    emptyList()
                }
            }
            
            val (qmResult, neResult, itunesResult) = awaitAll(deferredQM, deferredNE, deferredITUNES)
            
            results[Source.QM] = qmResult as List<SongInfo>
            results[Source.NE] = neResult as List<SongInfo>
            results[Source.ITUNES] = itunesResult as List<SongInfo>
            
            Log.d(TAG, "总搜索结果: QM=${results[Source.QM]?.size ?: 0}, NE=${results[Source.NE]?.size ?: 0}, ITUNES=${results[Source.ITUNES]?.size ?: 0}")
            
            results
        }
    }
    
    suspend fun searchFromSource(
        keyword: String, 
        source: Source,
        itunesCountry: String = "HK", 
        itunesConvertToSimplified: Boolean = false,
        itunesCoverSize: Int = 500,
        qmCoverSize: Int = 1000,
        neCoverSize: Int = 1000
    ): List<SongInfo> {
        return withContext(Dispatchers.IO) {
            try {
                when (source) {
                    Source.QM -> qqMusicApi.search(keyword, coverSize = qmCoverSize)
                    Source.NE -> neteaseApi.search(keyword, coverSize = neCoverSize)
                    Source.ITUNES -> itunesApi.search(
                        keyword, 
                        country = itunesCountry, 
                        convertToSimplified = itunesConvertToSimplified, 
                        coverSize = itunesCoverSize
                    )
                    else -> emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索失败 from $source: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    suspend fun searchAllSourcesForLyrics(keyword: String): Map<Source, List<SongInfo>> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<Source, List<SongInfo>>()
            
            val deferredQM = async { 
                try {
                    Log.d(TAG, "QQ音乐搜索: $keyword")
                    val result = qqMusicApi.search(keyword)
                    Log.d(TAG, "QQ音乐搜索结果: ${result.size}首")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "QQ音乐搜索失败", e)
                    emptyList()
                }
            }
            
            val deferredKG = async {
                try {
                    Log.d(TAG, "酷狗搜索: $keyword")
                    val result = kugouApi.search(keyword)
                    Log.d(TAG, "酷狗搜索结果: ${result.size}首")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "酷狗搜索失败", e)
                    emptyList()
                }
            }
            
            val deferredNE = async {
                try {
                    Log.d(TAG, "网易云搜索: $keyword")
                    val result = neteaseApi.search(keyword)
                    Log.d(TAG, "网易云搜索结果: ${result.size}首")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "网易云搜索失败", e)
                    emptyList()
                }
            }
            
            val (qmResult, kgResult, neResult) = awaitAll(deferredQM, deferredKG, deferredNE)
            
            results[Source.QM] = qmResult as List<SongInfo>
            results[Source.KG] = kgResult as List<SongInfo>
            results[Source.NE] = neResult as List<SongInfo>
            
            Log.d(TAG, "总搜索结果: QM=${results[Source.QM]?.size ?: 0}, KG=${results[Source.KG]?.size ?: 0}, NE=${results[Source.NE]?.size ?: 0}")
            
            results
        }
    }
    
    suspend fun getLyricsFromSource(songInfo: SongInfo): VerbatimLyricsResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取歌词: ${songInfo.title} - ${songInfo.artist} from ${songInfo.source}")
                val lyrics = when (songInfo.source) {
                    Source.QM -> qqMusicApi.getLyrics(songInfo)
                    Source.KG -> kugouApi.getLyrics(songInfo)
                    Source.NE -> neteaseApi.getLyrics(songInfo)
                    Source.ITUNES -> itunesApi.getLyrics(songInfo)
                }
                
                val sourceName = when (songInfo.source) {
                    Source.QM -> "QQ音乐"
                    Source.KG -> "酷狗音乐"
                    Source.NE -> "网易云音乐"
                    Source.ITUNES -> "iTunes"
                }
                
                Log.d(TAG, "歌词获取结果: orig=${lyrics?.orig?.size ?: 0}行, ts=${lyrics?.ts?.size ?: 0}行")
                
                VerbatimLyricsResult(
                    source = songInfo.source,
                    sourceName = sourceName,
                    lyrics = lyrics
                )
            } catch (e: Exception) {
                Log.e(TAG, "获取歌词失败: ${songInfo.title}", e)
                val sourceName = when (songInfo.source) {
                    Source.QM -> "QQ音乐"
                    Source.KG -> "酷狗音乐"
                    Source.NE -> "网易云音乐"
                    Source.ITUNES -> "iTunes"
                }
                VerbatimLyricsResult(
                    source = songInfo.source,
                    sourceName = sourceName,
                    lyrics = null,
                    error = e.message ?: "获取歌词失败"
                )
            }
        }
    }
    
    suspend fun getLyricsFromAllSources(
        keyword: String,
        maxPerSource: Int = 10
    ): List<VerbatimLyricsResult> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "开始搜索: $keyword")
            
            val searchResults = searchAllSourcesForLyrics(keyword)
            
            val allResults = mutableListOf<VerbatimLyricsResult>()
            
            for ((source, songs) in searchResults) {
                val limitedSongs = songs.take(maxPerSource)
                Log.d(TAG, "处理${source}的${limitedSongs.size}首歌曲")
                
                for (song in limitedSongs) {
                    val result = getLyricsFromSource(song)
                    if (result.lyrics != null && result.lyrics.orig.isNotEmpty()) {
                        allResults.add(result)
                        Log.d(TAG, "添加有效歌词: ${song.title}")
                    }
                }
            }
            
            Log.d(TAG, "最终结果: ${allResults.size}个有效歌词")
            allResults
        }
    }
}
