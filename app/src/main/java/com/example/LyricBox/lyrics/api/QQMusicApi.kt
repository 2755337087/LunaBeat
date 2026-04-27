package com.example.LyricBox.lyrics.api

import android.util.Base64
import android.util.Log
import com.example.LyricBox.lyrics.decryptor.QrcDecryptor
import com.example.LyricBox.lyrics.models.*
import com.example.LyricBox.lyrics.parser.QrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class QQMusicApi {
    companion object {
        private const val TAG = "QQMusicApi"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    

    
    private var comm: Map<String, Any> = mapOf(
        "ct" to 11,
        "cv" to "1003006",
        "v" to "1003006",
        "os_ver" to "15",
        "phonetype" to "24122RKC7C",
        "tmeAppID" to "qqmusiclight",
        "nettype" to "NETWORK_WIFI",
        "udid" to "0"
    )
    
    private var inited = false
    private var uid: String = "0"
    private var sid: String = ""
    private var userip: String = ""
    
    suspend fun init() {
        if (inited) return
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始初始化QQ音乐API")
                val param = mapOf("caller" to 0, "uid" to "0", "vkey" to 0)
                val data = request("GetSession", "music.getSession.session", param)
                val session = data.optJSONObject("session")
                if (session != null) {
                    uid = session.optString("uid", "0")
                    sid = session.optString("sid", "")
                    userip = session.optString("userip", "")
                    comm = comm + mapOf("uid" to uid, "sid" to sid, "userip" to userip)
                }
                inited = true
                Log.d(TAG, "QQ音乐API初始化成功: uid=$uid, sid=$sid")
            } catch (e: Exception) {
                Log.e(TAG, "QQ音乐API初始化失败，使用默认参数", e)
                inited = true
            }
        }
    }
    
    private fun request(method: String, module: String, param: Map<String, Any?>): JSONObject {
        val jsonStr = buildJsonRequest(method, module, param)
        Log.d(TAG, "请求: method=$method, module=$module")
        Log.d(TAG, "请求数据: $jsonStr")
        
        val request = Request.Builder()
            .url("https://u.y.qq.com/cgi-bin/musicu.fcg")
            .header("cookie", "tmeLoginType=-1;")
            .header("content-type", "application/json")
            .header("user-agent", "okhttp/3.14.9")
            .post(jsonStr.toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: return JSONObject()
            Log.d(TAG, "响应状态: ${response.code}")
            Log.d(TAG, "响应长度: ${responseBody.length}")
            
            val responseJson = JSONObject(responseBody)
            
            val code = responseJson.optInt("code", -1)
            val requestCode = responseJson.optJSONObject("request")?.optInt("code", -1) ?: -1
            
            if (code != 0 && requestCode != 0) {
                Log.e(TAG, "API错误: code=$code, requestCode=$requestCode")
                throw Exception("QQ Music API error: code=$code, requestCode=$requestCode")
            }
            
            val result = responseJson.optJSONObject("request")?.optJSONObject("data") ?: JSONObject()
            Log.d(TAG, "响应数据keys: ${result.keys().asSequence().toList()}")
            return result
        }
    }
    
    private fun buildJsonRequest(method: String, module: String, param: Map<String, Any?>): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"comm\":{")
        sb.append("\"ct\":${comm["ct"]},")
        sb.append("\"cv\":\"${comm["cv"]}\",")
        sb.append("\"v\":\"${comm["v"]}\",")
        sb.append("\"os_ver\":\"${comm["os_ver"]}\",")
        sb.append("\"phonetype\":\"${comm["phonetype"]}\",")
        sb.append("\"tmeAppID\":\"${comm["tmeAppID"]}\",")
        sb.append("\"nettype\":\"${comm["nettype"]}\",")
        sb.append("\"udid\":\"${comm["udid"]}\"")
        if (comm.containsKey("uid")) {
            sb.append(",\"uid\":\"${comm["uid"]}\"")
        }
        if (comm.containsKey("sid")) {
            sb.append(",\"sid\":\"${comm["sid"]}\"")
        }
        if (comm.containsKey("userip")) {
            sb.append(",\"userip\":\"${comm["userip"]}\"")
        }
        sb.append("},")
        sb.append("\"request\":{")
        sb.append("\"method\":\"$method\",")
        sb.append("\"module\":\"$module\",")
        sb.append("\"param\":")
        sb.append(buildJsonObject(param))
        sb.append("}")
        sb.append("}")
        return sb.toString()
    }
    
    private fun buildJsonObject(map: Map<String, Any?>): String {
        val sb = StringBuilder()
        sb.append("{")
        val entries = map.entries.toList()
        for ((index, entry) in entries.withIndex()) {
            sb.append("\"${entry.key}\":")
            sb.append(valueToJson(entry.value))
            if (index < entries.size - 1) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }
    
    private fun valueToJson(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> buildJsonObject(value.mapKeys { it.key.toString() })
            is List<*> -> {
                val sb = StringBuilder()
                sb.append("[")
                value.forEachIndexed { index, item ->
                    sb.append(valueToJson(item))
                    if (index < value.size - 1) sb.append(",")
                }
                sb.append("]")
                sb.toString()
            }
            else -> "\"${value.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
    }
    
    suspend fun search(keyword: String, page: Int = 1, coverSize: Int = 1000): List<SongInfo> {
        if (!inited) init()
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "搜索关键词: $keyword, 封面尺寸: $coverSize")
                val param = mapOf(
                    "search_id" to abs(Random().nextLong()).toString(),
                    "remoteplace" to "search.android.keyboard",
                    "query" to keyword,
                    "search_type" to 0,
                    "num_per_page" to 5,
                    "page_num" to page,
                    "highlight" to 0,
                    "nqc_flag" to 0,
                    "page_id" to 1,
                    "grp" to 1
                )
                
                val data = request("DoSearchForQQMusicLite", "music.search.SearchCgiService", param)
                Log.d(TAG, "搜索响应: $data")
                val body = data.optJSONObject("body") ?: return@withContext emptyList()
                val itemSong = body.optJSONArray("item_song") ?: return@withContext emptyList()
                Log.d(TAG, "找到歌曲数: ${itemSong.length()}")
                
                (0 until itemSong.length()).mapNotNull { i ->
                    val songInfo = itemSong.optJSONObject(i) ?: return@mapNotNull null
                    
                    val singers = songInfo.optJSONArray("singer")?.let { arr ->
                        (0 until arr.length()).mapNotNull { j ->
                            arr.optJSONObject(j)?.optString("name")
                        }
                    } ?: emptyList()
                    
                    val albumMid = songInfo.optJSONObject("album")?.optString("mid")
                    val singerMid = songInfo.optJSONArray("singer")?.optJSONObject(0)?.optString("mid")
                    val coverUrl = if (albumMid != null && albumMid.isNotEmpty() && albumMid != "空") {
                        "https://y.gtimg.cn/music/photo_new/T002R${coverSize}x${coverSize}M000${albumMid}.jpg"
                    } else if (singerMid != null && singerMid.isNotEmpty()) {
                        "https://y.gtimg.cn/music/photo_new/T001R${coverSize}x${coverSize}M000${singerMid}.jpg"
                    } else {
                        null
                    }
                    
                    val trackNumber = songInfo.optInt("index_album", 0).let { if (it > 0) it.toString() else null }
                    val timePublic = songInfo.optString("time_public", "")
                    val year = if (timePublic.isNotEmpty()) {
                        timePublic
                    } else null
                    
                    SongInfo(
                        source = Source.QM,
                        id = songInfo.opt("id")?.toString() ?: "",
                        mid = songInfo.optString("mid"),
                        title = songInfo.optString("title"),
                        subtitle = songInfo.optString("subtitle"),
                        artist = singers,
                        album = songInfo.optJSONObject("album")?.optString("name"),
                        duration = songInfo.optLong("interval") * 1000,
                        year = year,
                        trackNumber = trackNumber,
                        coverUrl = coverUrl
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索失败", e)
                emptyList()
            }
        }
    }
    
    suspend fun getLyrics(songInfo: SongInfo): Lyrics? {
        if (!inited) init()
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取歌词: ${songInfo.title} - ${songInfo.artist}, id=${songInfo.id}, mid=${songInfo.mid}, album=${songInfo.album}, duration=${songInfo.duration}")
                
                val param = mutableMapOf<String, Any?>(
                    "crypt" to 1,
                    "ct" to 19,
                    "cv" to 2111,
                    "interval" to (songInfo.duration?.div(1000) ?: 0),
                    "lrc_t" to 0,
                    "qrc" to 1,
                    "qrc_t" to 0,
                    "roma" to 1,
                    "roma_t" to 0,
                    "songID" to (songInfo.id.toIntOrNull() ?: 0),
                    "trans" to 1,
                    "trans_t" to 0,
                    "type" to 0
                )
                
                songInfo.album?.let { param["albumName"] = Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP) }
                songInfo.title?.let { param["songName"] = Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP) }
                if (songInfo.artist.isNotEmpty()) {
                    param["singerName"] = Base64.encodeToString(songInfo.artist.joinToString(",").toByteArray(), Base64.NO_WRAP)
                }
                
                val data = request("GetPlayLyricInfo", "music.musichallSong.PlayLyricInfo", param)
                Log.d(TAG, "歌词响应数据: lyric=${data.has("lyric")}, trans=${data.has("trans")}, roma=${data.has("roma")}")
                
                var tags: Map<String, String> = emptyMap()
                var origData: LyricsData = emptyList()
                var tsData: LyricsData = emptyList()
                var romaData: LyricsData = emptyList()
                
                val lyric = data.optString("lyric", "")
                val trans = data.optString("trans", "")
                val roma = data.optString("roma", "")
                
                if (lyric.isNotEmpty()) {
                    Log.d(TAG, "解密原文歌词, 长度: ${lyric.length}")
                    val decrypted = QrcDecryptor.decrypt(lyric)
                    if (decrypted != null) {
                        Log.d(TAG, "解密成功, 长度: ${decrypted.length}")
                        val (parsedTags, parsedData) = QrcParser.parse(decrypted)
                        tags = parsedTags
                        origData = parsedData
                        Log.d(TAG, "解析结果: ${origData.size}行")
                    } else {
                        Log.e(TAG, "解密失败")
                    }
                }
                
                if (trans.isNotEmpty()) {
                    Log.d(TAG, "解密翻译歌词, 长度: ${trans.length}")
                    val decrypted = QrcDecryptor.decrypt(trans)
                    if (decrypted != null) {
                        Log.d(TAG, "翻译解密成功, 长度: ${decrypted.length}")
                        Log.d(TAG, "翻译内容前200字符: ${decrypted.take(200)}")
                        val (parsedTags, parsedData) = QrcParser.parse(decrypted)
                        tsData = parsedData
                        Log.d(TAG, "翻译解析结果: ${tsData.size}行")
                    } else {
                        Log.e(TAG, "翻译解密失败")
                    }
                } else {
                    Log.d(TAG, "无翻译数据")
                }
                
                if (roma.isNotEmpty()) {
                    val decrypted = QrcDecryptor.decrypt(roma)
                    if (decrypted != null) {
                        val (_, parsedData) = QrcParser.parse(decrypted)
                        romaData = parsedData
                    }
                }
                
                Lyrics(tags = tags, orig = origData, ts = tsData, roma = romaData)
            } catch (e: Exception) {
                Log.e(TAG, "获取歌词失败", e)
                null
            }
        }
    }
    
    suspend fun getMusicDetail(songInfo: SongInfo, coverSize: Int = 1000): SongInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取QQ音乐详情: ${songInfo.title}, mid=${songInfo.mid}, 封面尺寸: $coverSize")
                
                val request = Request.Builder()
                    .url("https://c.y.qq.com/v8/fcg-bin/fcg_play_single_song.fcg?songmid=${songInfo.mid}&format=json")
                    .header("Referer", "https://y.qq.com/")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: return@withContext null
                    
                    val json = JSONObject(responseText)
                    val data = json.optJSONArray("data")?.optJSONObject(0) ?: return@withContext null
                    
                    val album = data.optJSONObject("album")
                    val albumName = album?.optString("name") ?: songInfo.album
                    val albumMid = album?.optString("mid")
                    
                    val singers = data.optJSONArray("singer")?.let { arr ->
                        (0 until arr.length()).mapNotNull { j ->
                            arr.optJSONObject(j)?.optString("name")
                        }
                    } ?: songInfo.artist
                    
                    val singerMid = data.optJSONArray("singer")?.optJSONObject(0)?.optString("mid")
                    val coverUrl = if (albumMid != null && albumMid.isNotEmpty() && albumMid != "空") {
                        "https://y.gtimg.cn/music/photo_new/T002R${coverSize}x${coverSize}M000${albumMid}.jpg"
                    } else if (singerMid != null && singerMid.isNotEmpty()) {
                        "https://y.gtimg.cn/music/photo_new/T001R${coverSize}x${coverSize}M000${singerMid}.jpg"
                    } else {
                        songInfo.coverUrl
                    }
                    
                    val trackNumber = data.optInt("index_album", 0).let { if (it > 0) it.toString() else null }
                    val year = data.optString("time_public", "").let { if (it.isNotEmpty()) it else null }
                    
                    SongInfo(
                        source = songInfo.source,
                        id = songInfo.id,
                        mid = songInfo.mid,
                        hash = songInfo.hash,
                        title = songInfo.title,
                        subtitle = songInfo.subtitle,
                        artist = singers,
                        album = albumName,
                        duration = data.optLong("interval", songInfo.duration ?: 0L) * 1000,
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
                Log.e(TAG, "获取QQ音乐详情失败", e)
                null
            }
        }
    }
}
