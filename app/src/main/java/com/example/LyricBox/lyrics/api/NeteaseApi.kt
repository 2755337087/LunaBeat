package com.example.LyricBox.lyrics.api

import android.util.Log
import com.example.LyricBox.lyrics.models.*
import com.example.LyricBox.lyrics.parser.LrcParser
import com.example.LyricBox.lyrics.parser.YrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class NeteaseApi {
    companion object {
        private const val TAG = "NeteaseApi"
        private const val EAPI_KEY = "e82ckenh8dichen8"
        private const val DEVICEID_XOR_KEY = "3go8&\$8*3*3h0k(2)2"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private var cookies: MutableMap<String, String> = mutableMapOf()
    private var inited = false
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    private fun aesEncrypt(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
    
    private fun aesDecrypt(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
    
    private fun eapiEncrypt(path: String, params: Map<String, Any?>): String {
        val paramsJson = buildJson(params)
        val signSrc = "nobody${path}use${paramsJson}md5forencrypt"
        val sign = md5(signSrc)
        val aesSrc = "$path-36cd479b6b5-$paramsJson-36cd479b6b5-$sign"
        val encrypted = aesEncrypt(aesSrc.toByteArray(), EAPI_KEY.toByteArray())
        return "params=${encrypted.joinToString("") { "%02X".format(it) }}"
    }
    
    private fun eapiDecrypt(data: ByteArray): String {
        val decrypted = aesDecrypt(data, EAPI_KEY.toByteArray())
        val padLen = decrypted.last().toInt()
        return if (padLen in 1..16) {
            String(decrypted.copyOfRange(0, decrypted.size - padLen), Charsets.UTF_8)
        } else {
            String(decrypted, Charsets.UTF_8)
        }
    }
    
    private fun buildJson(params: Map<String, Any?>): String {
        val sb = StringBuilder()
        sb.append("{")
        val entries = params.entries.toList()
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
            is Map<*, *> -> buildJson(value.mapKeys { it.key.toString() })
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
    
    private fun getAnonymousUsername(deviceId: String): String {
        val xoredChars = mutableListOf<Char>()
        for (i in deviceId.indices) {
            xoredChars.add((deviceId[i].code xor DEVICEID_XOR_KEY[i % DEVICEID_XOR_KEY.length].code).toChar())
        }
        val xoredString = xoredChars.joinToString("")
        val md5Digest = MessageDigest.getInstance("MD5").digest(xoredString.toByteArray())
        val combinedStr = "$deviceId ${Base64.getEncoder().encodeToString(md5Digest)}"
        return Base64.getEncoder().encodeToString(combinedStr.toByteArray())
    }
    
    private fun generateDeviceId(): String {
        return (1..32).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
    }
    
    private fun generateClientSign(): String {
        val mac = (1..6).map { "%02X".format(Random.nextInt(256)) }.joinToString(":")
        val randomStr = (1..8).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ"[Random.nextInt(26)] }.joinToString("")
        val hashPart = (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
        return "$mac@@@$randomStr@@@@@@$hashPart"
    }
    
    suspend fun init() {
        if (inited) return
        
        withContext(Dispatchers.IO) {
            try {
                val deviceId = generateDeviceId()
                val clientSign = generateClientSign()
                
                val preCookies = mapOf(
                    "os" to "pc",
                    "deviceId" to deviceId,
                    "osver" to "Microsoft-Windows-10--build-${Random.nextInt(200, 300)}00-64bit",
                    "clientSign" to clientSign,
                    "channel" to "netease",
                    "mode" to listOf("MS-iCraft B760M WIFI", "ASUS ROG STRIX Z790", "MSI MAG B550 TOMAHAWK").random(),
                    "appver" to "3.1.3.203419"
                )
                
                val header = buildJson(mapOf(
                    "clientSign" to clientSign,
                    "os" to "pc",
                    "appver" to "3.1.3.203419",
                    "deviceId" to deviceId,
                    "requestId" to 0,
                    "osver" to preCookies["osver"]!!
                ))
                
                val params = mapOf(
                    "username" to getAnonymousUsername(deviceId),
                    "e_r" to true,
                    "header" to header
                )
                
                val encryptedParams = eapiEncrypt("/api/register/anonimous", params)
                
                val request = Request.Builder()
                    .url("https://interface.music.163.com/eapi/register/anonimous")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36")
                    .header("appver", "3.1.3.203419")
                    .post(encryptedParams.toRequestBody())
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.bytes() ?: return@withContext
                    
                    val decrypted = try {
                        eapiDecrypt(responseBody)
                    } catch (e: Exception) {
                        String(responseBody, Charsets.UTF_8)
                    }
                    
                    Log.d(TAG, "登录响应: ${decrypted.take(500)}")
                    
                    val responseJson = JSONObject(decrypted)
                    if (responseJson.optInt("code", -1) == 200) {
                        cookies["WEVNSM"] = "1.0.0"
                        cookies["os"] = "pc"
                        cookies["deviceId"] = deviceId
                        cookies["osver"] = preCookies["osver"]!!
                        cookies["clientSign"] = clientSign
                        cookies["channel"] = "netease"
                        cookies["mode"] = preCookies["mode"]!!
                        cookies["appver"] = "3.1.3.203419"
                        
                        response.headers("Set-Cookie").forEach { cookie ->
                            val parts = cookie.split(";")[0].split("=", limit = 2)
                            if (parts.size == 2) {
                                cookies[parts[0]] = parts[1]
                            }
                        }
                        
                        inited = true
                        Log.d(TAG, "网易云游客登录成功")
                    } else {
                        Log.e(TAG, "网易云游客登录失败: code=${responseJson.optInt("code", -1)}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "网易云初始化失败", e)
            }
        }
    }
    
    private fun buildCookieHeader(): String {
        return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }
    
    suspend fun search(keyword: String, page: Int = 1, coverSize: Int = 1000): List<SongInfo> {
        if (!inited) init()
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "搜索关键词: $keyword, 封面尺寸: $coverSize")
                val path = "/api/search/song/list/page"
                val header = buildJson(mapOf(
                    "clientSign" to (cookies["clientSign"] ?: ""),
                    "os" to "pc",
                    "appver" to "3.1.3.203419",
                    "deviceId" to (cookies["deviceId"] ?: ""),
                    "requestId" to 0,
                    "osver" to (cookies["osver"] ?: "")
                ))
                
                val params = mapOf(
                    "keyword" to keyword,
                    "scene" to "NORMAL",
                    "needCorrect" to "true",
                    "limit" to "5",
                    "offset" to ((page - 1) * 20).toString(),
                    "e_r" to true,
                    "header" to header
                )
                
                val encryptedData = eapiEncrypt(path, params)
                
                val request = Request.Builder()
                    .url("https://interface.music.163.com/eapi/search/song/list/page")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36")
                    .header("appver", "3.1.3.203419")
                    .header("cookie", buildCookieHeader())
                    .post(encryptedData.toRequestBody())
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.bytes() ?: return@withContext emptyList()
                    
                    val decrypted = try {
                        eapiDecrypt(responseBody)
                    } catch (e: Exception) {
                        Log.e(TAG, "解密失败: ${e.message}")
                        String(responseBody, Charsets.UTF_8)
                    }
                    
                    val responseJson = JSONObject(decrypted)
                    
                    if (responseJson.optInt("code", -1) != 200) {
                        Log.e(TAG, "API错误: code=${responseJson.optInt("code", -1)}")
                        return@withContext emptyList()
                    }
                    
                    val resources = responseJson.optJSONObject("data")?.optJSONArray("resources") ?: return@withContext emptyList()
                    
                    (0 until resources.length()).mapNotNull { i ->
                        val resource = resources.optJSONObject(i) ?: return@mapNotNull null
                        val songData = resource.optJSONObject("baseInfo")
                        val simpleSongData = songData?.optJSONObject("simpleSongData") ?: return@mapNotNull null
                        
                        val singers = simpleSongData.optJSONArray("ar")?.let { arr ->
                            (0 until arr.length()).mapNotNull { j ->
                                arr.optJSONObject(j)?.optString("name")
                            }
                        } ?: emptyList()
                        
                        val album = simpleSongData.optJSONObject("al")
                        val coverUrl = album?.optString("picUrl")?.takeIf { it.isNotEmpty() }?.let {
                            "$it?param=${coverSize}y${coverSize}"
                        }
                        
                        val trackNumber = simpleSongData.optInt("no", 0).let { if (it > 0) it.toString() else null }
                        val cd = simpleSongData.optString("cd", "")
                        val discNumber = if (cd.isNotEmpty()) cd else null
                        
                        // 尝试从搜索结果中获取年份 - 检查多个对象和字段
                        var year: String? = null
                        var publishTime = simpleSongData.optLong("publishTime", 0L)
                        if (publishTime <= 0) {
                            publishTime = simpleSongData.optLong("pTime", 0L)
                        }
                        if (publishTime <= 0 && album != null) {
                            publishTime = album.optLong("publishTime", 0L)
                        }
                        if (publishTime <= 0 && album != null) {
                            publishTime = album.optLong("pTime", 0L)
                        }
                        if (publishTime > 0) {
                            year = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(publishTime))
                        }
                        
                        SongInfo(
                            source = Source.NE,
                            id = simpleSongData.opt("id")?.toString() ?: "",
                            title = simpleSongData.optString("name"),
                            subtitle = simpleSongData.optJSONArray("alia")?.optString(0),
                            artist = singers,
                            album = album?.optString("name"),
                            duration = simpleSongData.optLong("dt"),
                            year = year,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            coverUrl = coverUrl
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索失败", e)
                emptyList()
            }
        }
    }
    
    suspend fun getMusicDetail(songInfo: SongInfo, coverSize: Int = 1000): SongInfo? {
        if (!inited) init()
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取歌曲详情 - id=${songInfo.id}, 封面尺寸: $coverSize")
                val path = "/api/v3/song/detail"
                val header = buildJson(mapOf(
                    "clientSign" to (cookies["clientSign"] ?: ""),
                    "os" to "pc",
                    "appver" to "3.1.3.203419",
                    "deviceId" to (cookies["deviceId"] ?: ""),
                    "requestId" to 0,
                    "osver" to (cookies["osver"] ?: "")
                ))
                
                val params = mapOf(
                    "c" to """[{"id":${songInfo.id}}]""",
                    "ids" to "[${songInfo.id}]",
                    "e_r" to true,
                    "header" to header
                )
                
                val encryptedData = eapiEncrypt(path, params)
                
                val request = Request.Builder()
                    .url("https://interface.music.163.com/eapi/v3/song/detail")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36")
                    .header("appver", "3.1.3.203419")
                    .header("cookie", buildCookieHeader())
                    .post(encryptedData.toRequestBody())
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.bytes() ?: return@withContext null
                    
                    val decrypted = try {
                        eapiDecrypt(responseBody)
                    } catch (e: Exception) {
                        String(responseBody, Charsets.UTF_8)
                    }
                    
                    val responseJson = JSONObject(decrypted)
                    
                    if (responseJson.optInt("code", -1) != 200) {
                        Log.e(TAG, "获取详情API错误: code=${responseJson.optInt("code", -1)}")
                        return@withContext null
                    }
                    
                    val songs = responseJson.optJSONArray("songs")
                    if (songs == null || songs.length() == 0) {
                        return@withContext null
                    }
                    
                    val song = songs.optJSONObject(0) ?: return@withContext null
                    
                    val singers = song.optJSONArray("ar")?.let { arr ->
                        (0 until arr.length()).mapNotNull { j ->
                            arr.optJSONObject(j)?.optString("name")
                        }
                    } ?: songInfo.artist
                    
                    val album = song.optJSONObject("al")
                    val albumName = album?.optString("name") ?: songInfo.album
                    val coverUrl = album?.optString("picUrl")?.let {
                        "$it?param=${coverSize}y${coverSize}"
                    }
                    
                    // 打印所有可用的键以便调试
                    Log.d(TAG, "song 所有键: ${song.keys().asSequence().toList()}")
                    if (album != null) {
                        Log.d(TAG, "album 所有键: ${album.keys().asSequence().toList()}")
                    }
                    
                    // 尝试获取年份信息 - 尝试多个可能的字段和对象
                    var publishTime = song.optLong("publishTime", 0L)
                    if (publishTime <= 0) {
                        publishTime = song.optLong("pTime", 0L)
                    }
                    if (publishTime <= 0) {
                        publishTime = song.optLong("time", 0L)
                    }
                    // 尝试从专辑对象获取
                    if (publishTime <= 0 && album != null) {
                        publishTime = album.optLong("publishTime", 0L)
                    }
                    if (publishTime <= 0 && album != null) {
                        publishTime = album.optLong("pTime", 0L)
                    }
                    val year = if (publishTime > 0) {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(publishTime))
                    } else null
                    
                    Log.d(TAG, "详情 - 歌曲: ${song.optString("name")}, publishTime: $publishTime, year: $year")
                    
                    val trackNumber = song.optInt("no", 0).let { if (it > 0) it.toString() else null }
                    val discNumber = song.optString("cd")
                    
                    val copyright = song.optLong("copyright", 0L).toString()
                    
                    val real163Key = generate163Key(song, album, singers)
                    
                    Log.d(TAG, "生成的163key: ${real163Key?.take(100)}...")
                    
                    SongInfo(
                        source = songInfo.source,
                        id = songInfo.id,
                        mid = songInfo.mid,
                        hash = songInfo.hash,
                        title = song.optString("name") ?: songInfo.title,
                        subtitle = songInfo.subtitle,
                        artist = singers,
                        album = albumName,
                        duration = song.optLong("dt", songInfo.duration ?: 0L),
                        year = year,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        genre = null,
                        albumArtist = null,
                        composer = null,
                        lyricist = null,
                        comment = real163Key,
                        copyright = if (copyright != "0") copyright else null,
                        coverUrl = coverUrl
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取歌曲详情失败", e)
                null
            }
        }
    }
    
    private fun generate163Key(song: JSONObject, album: JSONObject?, singers: List<String>): String? {
        try {
            val safeAlbum = album ?: JSONObject()
            val musicId = song.optLong("id", 0L)
            val musicName = song.optString("name", "")
            val albumName = safeAlbum.optString("name", "")
            val albumId = safeAlbum.optLong("id", 0L)
            val albumPic = safeAlbum.optString("picUrl", "")
            val duration = song.optLong("dt", 0L)
            val bitrate = song.optJSONObject("h")?.optLong("br", 128000L) 
                ?: song.optJSONObject("m")?.optLong("br", 128000L)
                ?: 128000L
            
            val artistArray = org.json.JSONArray()
            val arArray = song.optJSONArray("ar")
            if (arArray != null) {
                for (i in 0 until arArray.length()) {
                    val artist = arArray.optJSONObject(i)
                    val artistItem = org.json.JSONArray()
                    artistItem.put(artist?.optString("name", ""))
                    artistItem.put(artist?.optLong("id", 0L))
                    artistArray.put(artistItem)
                }
            }
            
            val musicJson = JSONObject()
            musicJson.put("format", "mp3")
            musicJson.put("musicId", musicId)
            musicJson.put("musicName", musicName)
            musicJson.put("artist", artistArray)
            musicJson.put("album", albumName)
            musicJson.put("albumId", albumId)
            musicJson.put("albumPicDocId", 0L)
            musicJson.put("albumPic", albumPic)
            musicJson.put("mvId", 0L)
            musicJson.put("flag", 0)
            musicJson.put("bitrate", bitrate)
            musicJson.put("duration", duration)
            musicJson.put("alias", org.json.JSONArray())
            musicJson.put("transNames", org.json.JSONArray())

            return Netease163KeyCodec.encodeMusicJson(musicJson.toString())
        } catch (e: Exception) {
            Log.e(TAG, "生成163key失败", e)
            return null
        }
    }
    
    suspend fun getLyrics(songInfo: SongInfo): Lyrics? {
        if (!inited) init()
        
        return withContext(Dispatchers.IO) {
            try {
                val path = "/api/song/lyric/v1"
                val header = buildJson(mapOf(
                    "clientSign" to (cookies["clientSign"] ?: ""),
                    "os" to "pc",
                    "appver" to "3.1.3.203419",
                    "deviceId" to (cookies["deviceId"] ?: ""),
                    "requestId" to 0,
                    "osver" to (cookies["osver"] ?: "")
                ))
                
                val params = mapOf(
                    "id" to songInfo.id,
                    "lv" to "-1",
                    "tv" to "-1",
                    "rv" to "-1",
                    "yv" to "-1",
                    "e_r" to true,
                    "header" to header
                )
                
                val encryptedData = eapiEncrypt(path, params)
                
                val request = Request.Builder()
                    .url("https://interface.music.163.com/eapi/song/lyric/v1")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36")
                    .header("appver", "3.1.3.203419")
                    .header("cookie", buildCookieHeader())
                    .post(encryptedData.toRequestBody())
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.bytes() ?: return@withContext null
                    
                    val decrypted = try {
                        eapiDecrypt(responseBody)
                    } catch (e: Exception) {
                        String(responseBody, Charsets.UTF_8)
                    }
                    
                    val responseJson = JSONObject(decrypted)
                    
                    if (responseJson.optInt("code", -1) != 200) {
                        return@withContext null
                    }
                    
                    val tags = mutableMapOf<String, String>()
                    songInfo.artist.let { tags["ar"] = it.joinToString("/") }
                    songInfo.album?.let { tags["al"] = it }
                    songInfo.title?.let { tags["ti"] = it }
                    
                    var origData: LyricsData = emptyList()
                    var tsData: LyricsData = emptyList()
                    var romaData: LyricsData = emptyList()
                    
                    val yrcObj = responseJson.optJSONObject("yrc")
                    val hasYrc = yrcObj != null && yrcObj.optString("lyric", "").isNotEmpty()
                    
                    if (hasYrc) {
                        val yrcLyric = yrcObj?.optString("lyric", "") ?: ""
                        origData = YrcParser.parse(yrcLyric)
                        
                        val tlyric = responseJson.optJSONObject("tlyric")?.optString("lyric", "") ?: ""
                        if (tlyric.isNotEmpty()) {
                            val (_, parsed) = LrcParser.parse(tlyric)
                            tsData = parsed
                        }
                        
                        val romalrc = responseJson.optJSONObject("romalrc")?.optString("lyric", "") ?: ""
                        if (romalrc.isNotEmpty()) {
                            val (_, parsed) = LrcParser.parse(romalrc)
                            romaData = parsed
                        }
                    } else {
                        val lrcLyric = responseJson.optJSONObject("lrc")?.optString("lyric", "") ?: ""
                        if (lrcLyric.isNotEmpty()) {
                            val (parsedTags, parsed) = LrcParser.parse(lrcLyric)
                            tags.putAll(parsedTags)
                            origData = parsed
                        }
                        
                        val tlyric = responseJson.optJSONObject("tlyric")?.optString("lyric", "") ?: ""
                        if (tlyric.isNotEmpty()) {
                            val (_, parsed) = LrcParser.parse(tlyric)
                            tsData = parsed
                        }
                        
                        val romalrc = responseJson.optJSONObject("romalrc")?.optString("lyric", "") ?: ""
                        if (romalrc.isNotEmpty()) {
                            val (_, parsed) = LrcParser.parse(romalrc)
                            romaData = parsed
                        }
                    }
                    
                    Lyrics(tags = tags, orig = origData, ts = tsData, roma = romaData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取歌词失败", e)
                null
            }
        }
    }
}

private fun String.toRequestBody() = okhttp3.RequestBody.create(null, this.toByteArray())
