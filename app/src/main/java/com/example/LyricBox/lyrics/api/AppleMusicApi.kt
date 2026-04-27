package com.example.LyricBox.lyrics.api

import android.content.Context
import android.util.Log
import com.example.LyricBox.lyrics.models.Lyrics
import com.example.LyricBox.lyrics.models.LyricsData
import com.example.LyricBox.lyrics.models.LyricsLine
import com.example.LyricBox.lyrics.models.LyricsWord
import com.example.LyricBox.lyrics.models.Source
import com.example.LyricBox.lyrics.models.SongInfo
import com.example.LyricBox.utils.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.TlsVersion
import org.json.JSONObject
import java.util.Arrays
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class AppleMusicApi {
    companion object {
        private const val TAG = "AppleMusicApi"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followSslRedirects(true)
        .followRedirects(true)
        .connectionSpecs(
            Arrays.asList(
                ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                    .build(),
                ConnectionSpec.CLEARTEXT
            )
        )
        .build()
    
    private val iTunesApi = ITunesApi()

    fun getAppleMusicToken(): String {
        return try {
            val request = Request.Builder()
                .url("https://music.apple.com")
                .build()

            client.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: ""

                val indexJsPattern = Pattern.compile("/assets/index~[^/]+\\.js")
                val indexJsMatcher = indexJsPattern.matcher(html)
                if (!indexJsMatcher.find()) {
                    Log.e(TAG, "未找到 index.js 路径")
                    return ""
                }

                val indexJsUri = indexJsMatcher.group()
                Log.d(TAG, "找到 index.js: $indexJsUri")

                val indexJsRequest = Request.Builder()
                    .url("https://music.apple.com$indexJsUri")
                    .build()

                client.newCall(indexJsRequest).execute().use { indexJsResponse ->
                    val indexJs = indexJsResponse.body?.string() ?: ""

                    val tokenPattern = Pattern.compile("eyJh([^\"]*)")
                    val tokenMatcher = tokenPattern.matcher(indexJs)
                    if (tokenMatcher.find()) {
                        val token = tokenMatcher.group()
                        Log.d(TAG, "成功获取 Bearer Token")
                        token
                    } else {
                        Log.e(TAG, "未找到 Bearer Token")
                        ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取 Apple Music token 失败", e)
            throw e
        }
    }

    suspend fun search(
        keyword: String,
        storefront: String,
        limit: Int = 5,
        coverSize: Int = 3000
    ): List<SongInfo> {
        // 使用 ITunesApi 来搜索，不需要 Apple Music token
        val country = when (storefront.lowercase()) {
            "cn" -> "CN"
            "hk" -> "HK"
            "tw" -> "TW"
            "jp" -> "JP"
            "kr" -> "KR"
            "us" -> "US"
            else -> storefront.uppercase()
        }
        
        return iTunesApi.search(keyword, country, false, limit, coverSize)
    }

    suspend fun getLyrics(
        songInfo: SongInfo,
        context: Context,
        language: String = "zh-CN"
    ): Triple<Lyrics?, String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取 Apple Music 歌词: ${songInfo.title}, id=${songInfo.id}")

                val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val tokenSource = prefs.getString("amTokenSource", "cloudflare") ?: "cloudflare"
                val userToken = prefs.getString("amUserToken", "") ?: ""
                val cloudflareUrl = prefs.getString("amUrl", "") ?: ""
                val amUrlCountry = prefs.getString("amUrlCountry", "cn") ?: "cn"
                val amUrlCountryContributor = prefs.getString("amUrlCountryContributor", "tr") ?: "tr"
                
                Log.d(TAG, "tokenSource: $tokenSource")
                Log.d(TAG, "amUrlCountryContributor: $amUrlCountryContributor")
                
                val storefront = when (tokenSource) {
                    "contributor" -> amUrlCountryContributor
                    "custom" -> prefs.getString("amCountry", "cn") ?: "cn"
                    else -> amUrlCountry
                }
                
                val actualUserToken = userToken
                
                Log.d(TAG, "actualUserToken: $actualUserToken")
                Log.d(TAG, "storefront: $storefront")

                val bearerToken = getAppleMusicToken()
                if (bearerToken.isEmpty()) {
                    Log.e(TAG, "无法获取 Bearer token")
                    return@withContext Triple(null, null, "无法获取 Apple Music token，请检查网络连接")
                }

                val lyricsType = "syllable-lyrics"

                val (lyricsJson, errorMsg) = if ((tokenSource == "cloudflare" || tokenSource == "contributor") && cloudflareUrl.isNotEmpty()) {
                    getLyricsViaCloudflare(
                        context,
                        cloudflareUrl,
                        songInfo.id,
                        storefront,
                        language,
                        lyricsType,
                        bearerToken,
                        tokenSource == "contributor"
                    )
                } else if (tokenSource == "custom" && actualUserToken.isNotEmpty()) {
                    getLyricsDirectly(
                        songInfo.id,
                        storefront,
                        language,
                        lyricsType,
                        bearerToken,
                        actualUserToken
                    )
                } else {
                    Log.e(TAG, "未配置 Apple Music token")
                    return@withContext Triple(null, null, "未配置 Apple Music token，请在设置中配置")
                }
                
                if (errorMsg != null) {
                    return@withContext Triple(null, null, errorMsg)
                }
                
                if (lyricsJson == null) {
                    return@withContext Triple(null, null, "获取歌词失败")
                }
                
                // 检查是否是无歌词的错误响应
                val errors = lyricsJson.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val firstError = errors.optJSONObject(0)
                    val detail = firstError?.optString("detail", "")
                    if (detail?.contains("No related resources") == true) {
                        return@withContext Triple(null, null, "该歌曲暂无歌词")
                    }
                    val title = firstError?.optString("title", "")
                    return@withContext Triple(null, null, "获取歌词失败: $title")
                }

                val (lyrics, rawTtml) = parseLyricsFromJson(lyricsJson)
                if (lyrics == null && rawTtml == null) {
                    return@withContext Triple(null, null, "歌词解析失败，响应格式可能不正确")
                }
                Triple(lyrics, rawTtml, null)
            } catch (e: Exception) {
                Log.e(TAG, "获取 Apple Music 歌词失败", e)
                Triple(null, null, "获取歌词时出错: ${e.message}")
            }
        }
    }

    private fun decodeSecretKey(context: Context): String {
        // 使用 EncryptedSharedPreferences 安全存储
        return SecureStorage.getCloudflareSecretKey(context)
    }
    
    private fun generateSignature(timestamp: Long, secretKey: String): String {
        val data = timestamp.toString() + secretKey
        var hash = 0
        for (i in data.indices) {
            val char = data[i].code
            hash = ((hash shl 5) - hash) + char
            hash = hash and hash // 转换为 32 位整数
        }
        return kotlin.math.abs(hash).toString(16)
    }
    
    private suspend fun getLyricsViaCloudflare(
        context: Context,
        workerUrl: String,
        songId: String,
        storefront: String,
        language: String,
        lyricsType: String,
        bearerToken: String,
        isContributor: Boolean = false
    ): Pair<JSONObject?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== 通过 Cloudflare 获取歌词 ===")
                Log.d(TAG, "Worker URL: $workerUrl")
                Log.d(TAG, "isContributor: $isContributor")

                val jsonMediaType = "application/json".toMediaType()

                val jsonBody = JSONObject().apply {
                    put("songId", songId)
                    put("storefront", storefront)
                    put("language", language)
                    put("lyricsType", lyricsType)
                    put("bearerToken", bearerToken)
                    if (isContributor) {
                        put("useToken2", true)
                    }
                }

                Log.d(TAG, "请求 JSON: $jsonBody")

                val timestamp = System.currentTimeMillis()
                val secretKey = decodeSecretKey(context) // 解码密钥
                val signature = generateSignature(timestamp, secretKey)

                val request = Request.Builder()
                    .url(workerUrl)
                    .post(jsonBody.toString().toRequestBody(jsonMediaType))
                    .header("Content-Type", "application/json")
                    .header("X-Request-Timestamp", timestamp.toString())
                    .header("X-Request-Signature", signature)
                    .header("User-Agent", "LyricBox/1.0 (com.example.LyricBox; Android)")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: ""
                    Log.d(TAG, "响应: $responseText")

                    if (!response.isSuccessful) {
                        Log.e(TAG, "请求失败: ${response.code}")
                        // 对于 404 响应，我们仍然尝试解析响应内容，可能包含错误信息
                        try {
                            Pair(JSONObject(responseText), null)
                        } catch (e: Exception) {
                            Pair(null, "默认配置获取失败")
                        }
                    } else {
                        Pair(JSONObject(responseText), null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取歌词失败", e)
                val errorMsg = when (e) {
                    is java.net.SocketTimeoutException -> "服务器连接超时，请检查网络后重试"
                    is java.net.UnknownHostException -> "无法连接到服务器，请检查网络后重试"
                    is java.net.ConnectException -> "服务器连接失败，请检查网络后重试"
                    else -> "获取歌词时出错: ${e.message}"
                }
                Pair(null, errorMsg)
            }
        }
    }

    private suspend fun getLyricsDirectly(
        songId: String,
        storefront: String,
        language: String,
        lyricsType: String,
        bearerToken: String,
        mediaUserToken: String
    ): Pair<JSONObject?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== 直接获取 Apple Music 歌词 ===")

                val url = "https://amp-api.music.apple.com/v1/catalog/$storefront/songs/$songId/$lyricsType?l=$language&extend=ttmlLocalizations"
                Log.d(TAG, "Apple Music API URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .header("Origin", "https://music.apple.com")
                    .header("Referer", "https://music.apple.com/")
                    .header("Authorization", "Bearer $bearerToken")
                    .header("Cookie", "media-user-token=$mediaUserToken")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: ""
                    Log.d(TAG, "Apple Music 响应: $responseText")

                    if (!response.isSuccessful) {
                        Log.e(TAG, "Apple Music 请求失败: ${response.code}")
                        // 对于 404 响应，我们仍然尝试解析响应内容，可能包含错误信息
                        try {
                            Pair(JSONObject(responseText), null)
                        } catch (e: Exception) {
                            Pair(null, "Apple Music API 请求失败，请检查 Media-User-Token 是否有效")
                        }
                    } else {
                        Pair(JSONObject(responseText), null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "直接获取 Apple Music 歌词失败", e)
                val errorMsg = when (e) {
                    is java.net.SocketTimeoutException -> "服务器连接超时，请检查网络后重试"
                    is java.net.UnknownHostException -> "无法连接到服务器，请检查网络后重试"
                    is java.net.ConnectException -> "服务器连接失败，请检查网络后重试"
                    else -> "获取歌词时出错: ${e.message}"
                }
                Pair(null, errorMsg)
            }
        }
    }

    private fun parseLyricsFromJson(json: JSONObject): Pair<Lyrics?, String?> {
        return try {
            val data = json.optJSONArray("data") ?: return Pair(null, null)
            if (data.length() == 0) return Pair(null, null)

            val songData = data.optJSONObject(0) ?: return Pair(null, null)
            val attributes = songData.optJSONObject("attributes") ?: return Pair(null, null)

            var ttml = attributes.optString("ttml", "")
            if (ttml.isEmpty()) {
                ttml = attributes.optString("ttmlLocalizations", "")
            }

            if (ttml.isEmpty()) {
                Log.e(TAG, "未找到 TTML 歌词")
                return Pair(null, null)
            }

            val lyrics = parseTtml(ttml)
            Pair(lyrics, ttml)
        } catch (e: Exception) {
            Log.e(TAG, "解析歌词失败", e)
            Pair(null, null)
        }
    }

    private fun parseTtml(ttml: String): Lyrics {
        val lyricsData = mutableListOf<LyricsLine>()
        
        // 简化的 TTML 解析，用于提取逐字歌词
        // TTML 格式类似于：<p begin="00:00:01.000" end="00:00:02.000">...</p>
        
        val pPattern = Regex("""<p\s+[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
        val spanPattern = Regex("""<span\s+[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        
        val pMatches = pPattern.findAll(ttml)
        for (pMatch in pMatches) {
            val beginStr = pMatch.groupValues[1]
            val endStr = pMatch.groupValues[2]
            val content = pMatch.groupValues[3]
            
            val lineBegin = parseTime(beginStr)
            val lineEnd = parseTime(endStr)
            
            val words = mutableListOf<LyricsWord>()
            val spanMatches = spanPattern.findAll(content)
            for (spanMatch in spanMatches) {
                val spanBeginStr = spanMatch.groupValues[1]
                val spanEndStr = spanMatch.groupValues[2]
                val spanText = spanMatch.groupValues[3]
                    .replace(Regex("""<[^>]+>"""), "")
                    .trim()
                
                if (spanText.isNotEmpty()) {
                    val wordBegin = parseTime(spanBeginStr)
                    val wordEnd = parseTime(spanEndStr)
                    words.add(LyricsWord(wordBegin, wordEnd, spanText))
                }
            }
            
            // 如果没有 span 标签，把整行作为一个词
            if (words.isEmpty()) {
                val text = content.replace(Regex("""<[^>]+>"""), "").trim()
                if (text.isNotEmpty()) {
                    words.add(LyricsWord(lineBegin, lineEnd, text))
                }
            }
            
            if (words.isNotEmpty()) {
                lyricsData.add(LyricsLine(lineBegin, lineEnd, words))
            }
        }
        
        return Lyrics(orig = lyricsData)
    }

    private fun parseTime(timeStr: String): Long {
        // 格式: 00:00:01.000 或 00:00:01,000
        val normalized = timeStr.replace(',', '.')
        val parts = normalized.split(':')
        if (parts.size != 3) return 0L
        
        val hours = parts[0].toLongOrNull() ?: 0L
        val minutes = parts[1].toLongOrNull() ?: 0L
        val secondsPart = parts[2].split('.')
        val seconds = secondsPart[0].toLongOrNull() ?: 0L
        val milliseconds = if (secondsPart.size > 1) {
            secondsPart[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        } else 0L
        
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds
    }
}
