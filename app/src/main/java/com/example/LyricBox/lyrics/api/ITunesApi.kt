package com.example.LyricBox.lyrics.api

import android.util.Log
import com.example.LyricBox.lyrics.models.Lyrics
import com.example.LyricBox.lyrics.models.Source
import com.example.LyricBox.lyrics.models.SongInfo
import com.example.LyricBox.utils.ChineseConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ITunesApi {
    companion object {
        private const val TAG = "ITunesApi"
        private const val BASE_URL = "https://itunes.apple.com/search"
        private const val LOOKUP_URL = "https://itunes.apple.com/lookup"
        private const val DEFAULT_COVER_SIZE = 1000
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getCoverUrl(artworkUrl: String, coverSize: Int): String {
        return artworkUrl.replace(Regex("\\d+x\\d+bb"), "${coverSize}x${coverSize}bb")
    }

    private fun convertToSimplified(songInfo: SongInfo): SongInfo {
        return songInfo.copy(
            title = songInfo.title?.let { ChineseConverter.toSimplified(it) },
            artist = songInfo.artist.map { ChineseConverter.toSimplified(it) },
            album = songInfo.album?.let { ChineseConverter.toSimplified(it) },
            genre = songInfo.genre?.let { ChineseConverter.toSimplified(it) },
            albumArtist = songInfo.albumArtist?.let { ChineseConverter.toSimplified(it) },
            composer = songInfo.composer?.let { ChineseConverter.toSimplified(it) },
            lyricist = songInfo.lyricist?.let { ChineseConverter.toSimplified(it) },
            comment = songInfo.comment?.let { ChineseConverter.toSimplified(it) },
            copyright = songInfo.copyright?.let { ChineseConverter.toSimplified(it) }
        )
    }

    suspend fun search(keyword: String, country: String = "HK", convertToSimplified: Boolean = false, limit: Int = 5, coverSize: Int = DEFAULT_COVER_SIZE): List<SongInfo> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "iTunes搜索关键词: $keyword, 国家: $country, 封面尺寸: $coverSize")
                val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
                val url = "$BASE_URL?term=$encodedKeyword&entity=song&country=$country&limit=$limit"
                
                Log.d(TAG, "请求URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: return@withContext emptyList()
                    Log.d(TAG, "响应: $responseText")
                    
                    if (responseText.isBlank()) {
                        Log.w(TAG, "iTunes返回空响应")
                        return@withContext emptyList()
                    }
                    
                    val json = JSONObject(responseText)
                    val results = json.optJSONArray("results") ?: return@withContext emptyList()
                    
                    Log.d(TAG, "找到结果数: ${results.length()}")
                    
                    (0 until results.length()).mapNotNull { i ->
                        val item = results.optJSONObject(i) ?: return@mapNotNull null
                        
                        val trackId = item.optLong("trackId", 0L)
                        if (trackId == 0L) return@mapNotNull null
                        
                        val artistName = item.optString("artistName", "")
                        val trackName = item.optString("trackName", "")
                        val collectionName = item.optString("collectionName", "")
                        val collectionArtistName = item.optString("collectionArtistName", "")
                        val artworkUrl100 = item.optString("artworkUrl100", "")
                        val releaseDate = item.optString("releaseDate", "")
                        val trackTimeMillis = item.optLong("trackTimeMillis", 0L)
                        val trackNumber = item.optInt("trackNumber", 0)
                        val discNumber = item.optInt("discNumber", 0)
                        val primaryGenreName = item.optString("primaryGenreName", "")
                        
                        val coverUrl = if (artworkUrl100.isNotEmpty()) {
                            getCoverUrl(artworkUrl100, coverSize)
                        } else {
                            null
                        }
                        
                        val year = if (releaseDate.isNotEmpty() && releaseDate.length >= 10) {
                            releaseDate.substring(0, 10)
                        } else if (releaseDate.isNotEmpty() && releaseDate.length >= 4) {
                            releaseDate.substring(0, 4)
                        } else {
                            null
                        }
                        
                        val artists = if (artistName.isNotEmpty()) {
                            artistName.split(Regex("[,&]")).map { it.trim() }.filter { it.isNotEmpty() }
                        } else {
                            emptyList()
                        }
                        
                        val finalAlbumArtist = if (collectionArtistName.isNotEmpty()) {
                            collectionArtistName
                        } else if (artistName.isNotEmpty()) {
                            artistName
                        } else {
                            null
                        }
                        
                        val songInfo = SongInfo(
                            source = Source.ITUNES,
                            id = trackId.toString(),
                            mid = trackId.toString(),
                            title = if (trackName.isNotEmpty()) trackName else null,
                            artist = artists,
                            album = if (collectionName.isNotEmpty()) collectionName else null,
                            duration = if (trackTimeMillis > 0) trackTimeMillis else null,
                            year = year,
                            trackNumber = if (trackNumber > 0) trackNumber.toString() else null,
                            discNumber = if (discNumber > 0) discNumber.toString() else null,
                            genre = if (primaryGenreName.isNotEmpty()) primaryGenreName else null,
                            albumArtist = finalAlbumArtist,
                            coverUrl = coverUrl
                        )
                        
                        if (convertToSimplified) {
                            convertToSimplified(songInfo)
                        } else {
                            songInfo
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "iTunes搜索失败", e)
                emptyList()
            }
        }
    }

    private suspend fun getAlbumCopyright(collectionId: Long, country: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取专辑版权: collectionId=$collectionId, country=$country")
                val url = "$LOOKUP_URL?id=$collectionId&entity=album&country=$country"
                Log.d(TAG, "专辑请求URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: return@withContext null
                    Log.d(TAG, "专辑响应: $responseText")
                    
                    if (responseText.isBlank()) {
                        Log.w(TAG, "iTunes专辑API返回空响应")
                        return@withContext null
                    }
                    
                    val json = JSONObject(responseText)
                    val results = json.optJSONArray("results") ?: return@withContext null
                    
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val wrapperType = item.optString("wrapperType", "")
                        if (wrapperType == "collection") {
                            val copyright = item.optString("copyright", "")
                            if (copyright.isNotEmpty()) {
                                Log.d(TAG, "找到专辑版权: $copyright")
                                return@withContext copyright
                            }
                        }
                    }
                    
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取专辑版权失败", e)
                null
            }
        }
    }

    suspend fun getMusicDetail(songInfo: SongInfo, country: String = "HK", convertToSimplified: Boolean = false, coverSize: Int = DEFAULT_COVER_SIZE): SongInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取iTunes详情: ${songInfo.title}, id=${songInfo.id}, country=$country, 封面尺寸: $coverSize")
                
                val url = "$LOOKUP_URL?id=${songInfo.id}&country=$country"
                Log.d(TAG, "请求URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: return@withContext null
                    Log.d(TAG, "响应: $responseText")
                    
                    if (responseText.isBlank()) {
                        Log.w(TAG, "iTunes详情API返回空响应")
                        return@withContext null
                    }
                    
                    val json = JSONObject(responseText)
                    val results = json.optJSONArray("results") ?: return@withContext null
                    if (results.length() == 0) return@withContext null
                    
                    val item = results.optJSONObject(0) ?: return@withContext null
                    
                    val artistName = item.optString("artistName", "")
                    val trackName = item.optString("trackName", "")
                    val collectionName = item.optString("collectionName", "")
                    val collectionArtistName = item.optString("collectionArtistName", "")
                    val artworkUrl100 = item.optString("artworkUrl100", "")
                    val releaseDate = item.optString("releaseDate", "")
                    val trackTimeMillis = item.optLong("trackTimeMillis", 0L)
                    val trackNumber = item.optInt("trackNumber", 0)
                    val discNumber = item.optInt("discNumber", 0)
                    val primaryGenreName = item.optString("primaryGenreName", "")
                    
                    val collectionId = item.optLong("collectionId", 0L)
                    val copyright = if (collectionId > 0L) {
                        getAlbumCopyright(collectionId, country)
                    } else {
                        null
                    }
                    
                    val coverUrl = if (artworkUrl100.isNotEmpty()) {
                        getCoverUrl(artworkUrl100, coverSize)
                    } else {
                        songInfo.coverUrl
                    }
                    
                    val year = if (releaseDate.isNotEmpty() && releaseDate.length >= 10) {
                        releaseDate.substring(0, 10)
                    } else if (releaseDate.isNotEmpty() && releaseDate.length >= 4) {
                        releaseDate.substring(0, 4)
                    } else {
                        songInfo.year
                    }
                    
                    val artists = if (artistName.isNotEmpty()) {
                        artistName.split(Regex("[,&]")).map { it.trim() }.filter { it.isNotEmpty() }
                    } else {
                        songInfo.artist
                    }
                    
                    val finalAlbumArtist = if (collectionArtistName.isNotEmpty()) {
                        collectionArtistName
                    } else if (artistName.isNotEmpty()) {
                        artistName
                    } else {
                        songInfo.albumArtist
                    }
                    
                    val detailedSongInfo = SongInfo(
                        source = songInfo.source,
                        id = songInfo.id,
                        mid = songInfo.mid,
                        hash = songInfo.hash,
                        title = if (trackName.isNotEmpty()) trackName else songInfo.title,
                        subtitle = songInfo.subtitle,
                        artist = artists,
                        album = if (collectionName.isNotEmpty()) collectionName else songInfo.album,
                        duration = if (trackTimeMillis > 0) trackTimeMillis else songInfo.duration,
                        year = year,
                        trackNumber = if (trackNumber > 0) trackNumber.toString() else songInfo.trackNumber,
                        discNumber = if (discNumber > 0) discNumber.toString() else songInfo.discNumber,
                        genre = if (primaryGenreName.isNotEmpty()) primaryGenreName else songInfo.genre,
                        albumArtist = finalAlbumArtist,
                        composer = songInfo.composer,
                        lyricist = songInfo.lyricist,
                        comment = songInfo.comment,
                        copyright = copyright,
                        coverUrl = coverUrl
                    )
                    
                    if (convertToSimplified) {
                        convertToSimplified(detailedSongInfo)
                    } else {
                        detailedSongInfo
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取iTunes详情失败", e)
                null
            }
        }
    }

    suspend fun getLyrics(songInfo: SongInfo): Lyrics? {
        Log.d(TAG, "iTunes不提供歌词API")
        return null
    }
}
