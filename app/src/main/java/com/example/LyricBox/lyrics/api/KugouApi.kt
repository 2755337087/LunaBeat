package com.example.LyricBox.lyrics.api

import android.util.Base64
import android.util.Log
import com.example.LyricBox.lyrics.decryptor.KrcDecryptor
import com.example.LyricBox.lyrics.models.*
import com.example.LyricBox.lyrics.parser.KrcParser
import com.example.LyricBox.lyrics.parser.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class KugouApi {
    companion object {
        private const val TAG = "KugouApi"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private var dfid: String? = null
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    private fun generateMid(): String {
        return md5(System.currentTimeMillis().toString())
    }
    
    private suspend fun initDfid() {
        if (dfid != null) return
        
        withContext(Dispatchers.IO) {
            try {
                val mid = generateMid()
                val params = mapOf(
                    "appid" to "1014",
                    "platid" to "4",
                    "mid" to mid
                )
                
                val sortedValues = params.values.map { it.toString() }.sorted()
                val signature = md5("1014${sortedValues.joinToString("")}1014")
                
                val url = "https://userservice.kugou.com/risk/v1/r_register_dev?" +
                        "appid=1014&platid=4&mid=$mid&signature=$signature"
                
                val request = Request.Builder()
                    .url(url)
                    .post("{\"uuid\":\"\"}".toRequestBody())
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: return@withContext
                    val responseJson = JSONObject(responseBody)
                    dfid = responseJson.optJSONObject("data")?.optString("dfid", "-") ?: "-"
                }
            } catch (e: Exception) {
                dfid = "-"
            }
        }
    }
    
    suspend fun search(keyword: String, page: Int = 1): List<SongInfo> {
        initDfid()
        
        return withContext(Dispatchers.IO) {
            try {
                val mid = generateMid()
                val clienttime = (System.currentTimeMillis() / 1000).toInt()
                
                val params = mutableMapOf(
                    "sorttype" to "0",
                    "keyword" to keyword,
                    "pagesize" to "5",
                    "page" to page.toString()
                )
                
                val signatureParams = mapOf(
                    "userid" to "0",
                    "appid" to "3116",
                    "token" to "",
                    "clienttime" to clienttime.toString(),
                    "iscorrection" to "1",
                    "uuid" to "-",
                    "mid" to mid,
                    "dfid" to (dfid ?: "-"),
                    "clientver" to "11070",
                    "platform" to "AndroidFilter"
                )
                
                params.putAll(signatureParams)
                
                val sortedParams = params.toSortedMap()
                val signatureStr = "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA" +
                        sortedParams.entries.joinToString("") { "${it.key}=${it.value}" } +
                        "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA"
                params["signature"] = md5(signatureStr)
                
                val url = "http://complexsearch.kugou.com/v2/search/song?" +
                        params.entries.joinToString("&") { "${it.key}=${it.value}" }
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Android14-1070-11070-201-0-SearchSong-wifi")
                    .header("mid", mid)
                    .header("KG-Rec", "1")
                    .header("KG-RC", "1")
                    .header("x-router", "complexsearch.kugou.com")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: return@withContext emptyList()
                    val responseJson = JSONObject(responseBody)
                    
                    val errorCode = responseJson.optInt("error_code", -1)
                    if (errorCode !in listOf(0, 200)) {
                        return@withContext emptyList()
                    }
                    
                    val lists = responseJson.optJSONObject("data")?.optJSONArray("lists") ?: return@withContext emptyList()
                    
                    (0 until lists.length()).mapNotNull { i ->
                        val songInfo = lists.optJSONObject(i) ?: return@mapNotNull null
                        
                        val singers = songInfo.optJSONArray("Singers")?.let { arr ->
                            (0 until arr.length()).mapNotNull { j ->
                                arr.optJSONObject(j)?.optString("name")
                            }
                        } ?: emptyList()
                        
                        val coverUrl = songInfo.optString("ID")?.let { rid ->
                            "https://artistpicserver.kuwo.cn/pic.web?corp=kuwo&type=rid_pic&pictype=300&size=300&rid=$rid"
                        }
                        
                        SongInfo(
                            source = Source.KG,
                            id = songInfo.opt("ID")?.toString() ?: "",
                            hash = songInfo.optString("FileHash"),
                            title = songInfo.optString("SongName"),
                            subtitle = songInfo.optString("Auxiliary"),
                            artist = singers,
                            album = songInfo.optString("AlbumName"),
                            duration = songInfo.optLong("Duration") * 1000,
                            coverUrl = coverUrl
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "搜索失败", e)
                emptyList()
            }
        }
    }
    
    suspend fun getLyrics(songInfo: SongInfo): Lyrics? {
        initDfid()
        
        return withContext(Dispatchers.IO) {
            try {
                val mid = generateMid()
                
                val keyword = "${songInfo.artist.joinToString("、")} - ${songInfo.title}"
                
                val params = mutableMapOf(
                    "album_audio_id" to songInfo.id,
                    "duration" to (songInfo.duration ?: 0).toString(),
                    "hash" to (songInfo.hash ?: ""),
                    "keyword" to keyword,
                    "lrctxt" to "1",
                    "man" to "no"
                )
                
                val signatureParams = mapOf(
                    "appid" to "3116",
                    "clientver" to "11070"
                )
                
                params.putAll(signatureParams)
                
                val sortedParams = params.toSortedMap()
                val signatureStr = "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA" +
                        sortedParams.entries.joinToString("") { "${it.key}=${it.value}" } +
                        "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA"
                params["signature"] = md5(signatureStr)
                
                val url = "https://lyrics.kugou.com/v1/search?" +
                        params.entries.joinToString("&") { "${it.key}=${it.value}" }
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Android14-1070-11070-201-0-Lyric-wifi")
                    .header("mid", mid)
                    .build()
                
                val candidates = client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: return@withContext null
                    val responseJson = JSONObject(responseBody)
                    responseJson.optJSONArray("candidates")
                } ?: return@withContext null
                
                if (candidates.length() == 0) return@withContext null
                
                val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
                val lyricId = firstCandidate.opt("id")?.toString() ?: return@withContext null
                val accesskey = firstCandidate.optString("accesskey") ?: return@withContext null
                
                val downloadParams = mutableMapOf(
                    "accesskey" to accesskey,
                    "charset" to "utf8",
                    "client" to "mobi",
                    "fmt" to "krc",
                    "id" to lyricId,
                    "ver" to "1"
                )
                
                downloadParams.putAll(signatureParams)
                
                val downloadSortedParams = downloadParams.toSortedMap()
                val downloadSignatureStr = "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA" +
                        downloadSortedParams.entries.joinToString("") { "${it.key}=${it.value}" } +
                        "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA"
                downloadParams["signature"] = md5(downloadSignatureStr)
                
                val downloadUrl = "http://lyrics.kugou.com/download?" +
                        downloadParams.entries.joinToString("&") { "${it.key}=${it.value}" }
                
                val downloadRequest = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Android14-1070-11070-201-0-Lyric-wifi")
                    .header("mid", mid)
                    .build()
                
                client.newCall(downloadRequest).execute().use { response ->
                    val responseBody = response.body?.string() ?: return@withContext null
                    val responseJson = JSONObject(responseBody)
                    
                    val content = responseJson.optString("content") ?: return@withContext null
                    val contentType = responseJson.optInt("contenttype", 0)
                    
                    if (contentType == 2) {
                        val decoded = Base64.decode(content, Base64.DEFAULT).toString(Charsets.UTF_8)
                        val (tags, lyricsData) = LrcParser.parse(decoded)
                        Lyrics(tags = tags, orig = lyricsData)
                    } else {
                        val decoded = Base64.decode(content, Base64.DEFAULT)
                        val decrypted = KrcDecryptor.decrypt(decoded) ?: return@withContext null
                        val (tags, lyricsMap) = KrcParser.parse(decrypted)
                        Lyrics(
                            tags = tags,
                            orig = lyricsMap["orig"] ?: emptyList(),
                            ts = lyricsMap["ts"] ?: emptyList(),
                            roma = lyricsMap["roma"] ?: emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "获取歌词失败", e)
                null
            }
        }
    }
    
    suspend fun getMusicDetail(songInfo: SongInfo): SongInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取酷狗音乐详情: ${songInfo.title}, id=${songInfo.id}")
                
                val request = Request.Builder()
                    .url("https://www.kuwo.cn/api/www/music/musicInfo?mid=${songInfo.id}")
                    .header("Referer", "https://www.kuwo.cn/")
                    .header("Cookie", "kw_token=NONE")
                    .header("csrf", "NONE")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: return@withContext null
                    
                    val json = JSONObject(responseText)
                    if (json.optInt("code", -1) != 200) {
                        Log.d(TAG, "获取详情API失败，code=${json.optInt("code")}")
                        return@withContext null
                    }
                    
                    val data = json.optJSONObject("data") ?: return@withContext null
                    
                    val artists = data.optJSONArray("artistList")?.let { arr ->
                        (0 until arr.length()).mapNotNull { j ->
                            arr.optJSONObject(j)?.optString("name")
                        }
                    } ?: songInfo.artist
                    
                    val coverUrl = data.optString("pic")?.let { 
                        if (it.isNotEmpty()) it else null
                    } ?: songInfo.coverUrl
                    
                    val year = data.optString("releaseDate", "").take(4).let { 
                        if (it.isNotEmpty()) it else null
                    }
                    
                    val trackNumber = data.optInt("track", 0).let { if (it > 0) it.toString() else null }
                    val album = data.optString("album") ?: songInfo.album
                    
                    SongInfo(
                        source = songInfo.source,
                        id = songInfo.id,
                        mid = songInfo.mid,
                        hash = songInfo.hash,
                        title = data.optString("name") ?: songInfo.title,
                        subtitle = songInfo.subtitle,
                        artist = artists,
                        album = album,
                        duration = data.optLong("duration", songInfo.duration ?: 0L) * 1000,
                        year = year,
                        trackNumber = trackNumber,
                        discNumber = null,
                        genre = null,
                        albumArtist = null,
                        composer = null,
                        lyricist = null,
                        comment = null,
                        copyright = null,
                        coverUrl = coverUrl
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取酷狗音乐详情失败", e)
                null
            }
        }
    }
}

private fun String.toRequestBody() = okhttp3.RequestBody.create(null, this.toByteArray())
