package com.example.LyricBox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.Arrays

class AppleMusicLyricsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    AppleMusicLyricsScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicLyricsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var useCloudflareProxy by remember { mutableStateOf(false) }
    var cloudflareWorkerUrl by remember { mutableStateOf("") }
    var mediaUserToken by remember { mutableStateOf("") }
    var bearerToken by remember { mutableStateOf("") }
    var songId by remember { mutableStateOf("") }
    var storefront by remember { mutableStateOf("cn") }
    var language by remember { mutableStateOf("zh-CN") }
    var lyricsType by remember { mutableStateOf("syllable-lyrics") }
    
    var isLoading by remember { mutableStateOf(false) }
    var isFetchingToken by remember { mutableStateOf(false) }
    var lyricsResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCopiedDialog by remember { mutableStateOf(false) }
    
    // 从设置读取配置
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val tokenSource = prefs.getString("amTokenSource", "cloudflare") ?: "cloudflare"
        val userToken = prefs.getString("amUserToken", "") ?: ""
        val workerUrl = prefs.getString("amCloudflareUrl", "") ?: ""
        
        useCloudflareProxy = tokenSource == "cloudflare"
        mediaUserToken = userToken
        cloudflareWorkerUrl = workerUrl
    }
    
    fun fetchBearerToken() {
        isFetchingToken = true
        errorMessage = null
        
        scope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    getAppleMusicToken()
                }
                
                if (token.isNotEmpty()) {
                    bearerToken = token
                } else {
                    errorMessage = "获取 Bearer Token 失败"
                }
            } catch (e: Exception) {
                errorMessage = "获取 Bearer Token 失败: ${e.message}"
            } finally {
                isFetchingToken = false
            }
        }
    }
    
    fun fetchLyrics() {
        if (songId.isBlank() || storefront.isBlank() || language.isBlank()) {
            errorMessage = "请填写所有必填字段"
            return
        }
        
        // 只有不使用 Cloudflare Worker 时才需要 Media-User-Token
        if (!useCloudflareProxy && mediaUserToken.isBlank()) {
            errorMessage = "请填写 Media-User-Token"
            return
        }
        
        if (useCloudflareProxy && cloudflareWorkerUrl.isBlank()) {
            errorMessage = "请输入 Worker URL"
            return
        }
        
        if (!useCloudflareProxy && bearerToken.isBlank()) {
            errorMessage = "请先获取 Bearer Token"
            return
        }
        
        isLoading = true
        errorMessage = null
        lyricsResult = null
        
        scope.launch {
            try {
                // 如果使用 Cloudflare Worker 且没有 Bearer Token，先自动获取
                val finalBearerToken = if (useCloudflareProxy && bearerToken.isBlank()) {
                    Log.d("AppleMusic", "使用 Worker，先获取 Bearer Token")
                    val token = withContext(Dispatchers.IO) {
                        getAppleMusicToken()
                    }
                    if (token.isNotEmpty()) {
                        bearerToken = token
                        token
                    } else {
                        errorMessage = "获取 Bearer Token 失败"
                        isLoading = false
                        return@launch
                    }
                } else {
                    bearerToken
                }
                
                val result = withContext(Dispatchers.IO) {
                    if (useCloudflareProxy) {
                        getSongLyricsViaCloudflare(
                            workerUrl = cloudflareWorkerUrl,
                            songId = songId,
                            storefront = storefront,
                            language = language,
                            lyricsType = lyricsType,
                            bearerToken = finalBearerToken,
                            mediaUserToken = mediaUserToken
                        )
                    } else {
                        getSongLyrics(
                            songId = songId,
                            storefront = storefront,
                            token = finalBearerToken,
                            userToken = mediaUserToken,
                            lrcType = lyricsType,
                            language = language
                        )
                    }
                }
                
                result.fold(
                    onSuccess = { lyrics ->
                        lyricsResult = lyrics
                    },
                    onFailure = { e ->
                        errorMessage = "获取失败: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                errorMessage = "获取失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("歌词", text)
        clipboard.setPrimaryClip(clip)
        showCopiedDialog = true
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "Apple Music 歌词获取",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "参数设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = useCloudflareProxy,
                    onCheckedChange = { useCloudflareProxy = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "使用 Worker 代理",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            AnimatedVisibility(visible = useCloudflareProxy) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    InputField(
                        label = "Worker URL",
                        value = cloudflareWorkerUrl,
                        onValueChange = { cloudflareWorkerUrl = it },
                        placeholder = "https://",
                        isMultiline = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    InputField(
                        label = "Bearer Token",
                        value = bearerToken,
                        onValueChange = { bearerToken = it },
                        placeholder = if (useCloudflareProxy) "点击获取或自动获取" else "点击右侧按钮自动获取",
                        isMultiline = true,
                        enabled = false
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = { fetchBearerToken() },
                    enabled = !isFetchingToken
                ) {
                    if (isFetchingToken) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "获取",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InputField(
                label = if (useCloudflareProxy) "Media-User-Token (可选)" else "Media-User-Token",
                value = mediaUserToken,
                onValueChange = { mediaUserToken = it },
                placeholder = if (useCloudflareProxy) "留空使用 Worker 中配置的 Token" else "请输入 Media-User-Token",
                isMultiline = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InputField(
                label = "歌曲 ID",
                value = songId,
                onValueChange = { songId = it },
                placeholder = "请输入歌曲 ID"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InputField(
                    label = "Storefront",
                    value = storefront,
                    onValueChange = { storefront = it },
                    placeholder = "cn",
                    modifier = Modifier.weight(1f)
                )
                
                InputField(
                    label = "语言",
                    value = language,
                    onValueChange = { language = it },
                    placeholder = "zh-CN",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "歌词类型",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OptionButton(
                    label = "逐字歌词",
                    selected = lyricsType == "syllable-lyrics",
                    onClick = { lyricsType = "syllable-lyrics" },
                    modifier = Modifier.weight(1f)
                )
                
                OptionButton(
                    label = "普通歌词",
                    selected = lyricsType == "lyrics",
                    onClick = { lyricsType = "lyrics" },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { fetchLyrics() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("获取中...")
                } else {
                    Text("获取歌词")
                }
            }
            
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            
            lyricsResult?.let { lyrics ->
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "歌词结果",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    IconButton(onClick = { copyToClipboard(lyrics) }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Text(
                        text = lyrics,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    
    if (showCopiedDialog) {
        AlertDialog(
            onDismissRequest = { showCopiedDialog = false },
            title = { Text("提示") },
            text = { Text("已复制到剪贴板") },
            confirmButton = {
                Button(onClick = { showCopiedDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isMultiline: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (enabled) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 12.dp, vertical = if (isMultiline) 10.dp else 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = !isMultiline,
                maxLines = if (isMultiline) 4 else 1,
                enabled = enabled,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = if (enabled) 
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                ),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun OptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

suspend fun getAppleMusicToken(): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        try {
            val mainPageRequest = Request.Builder()
                .url("https://music.apple.com")
                .build()
            
            client.newCall(mainPageRequest).execute().use { response ->
                val responseText = response.body?.string() ?: return@withContext ""
                
                val jsPattern = Pattern.compile("/assets/index~[^/]+\\.js")
                val jsMatcher = jsPattern.matcher(responseText)
                
                if (!jsMatcher.find()) {
                    Log.e("AppleMusic", "未找到 index.js 路径")
                    return@withContext ""
                }
                
                val indexJsUri = jsMatcher.group()
                Log.d("AppleMusic", "找到 index.js: $indexJsUri")
                
                val jsRequest = Request.Builder()
                    .url("https://music.apple.com$indexJsUri")
                    .build()
                
                client.newCall(jsRequest).execute().use { jsResponse ->
                    val jsText = jsResponse.body?.string() ?: return@withContext ""
                    
                    val tokenPattern = Pattern.compile("eyJh([^\"]*)")
                    val tokenMatcher = tokenPattern.matcher(jsText)
                    
                    if (tokenMatcher.find()) {
                        val token = tokenMatcher.group()
                        Log.d("AppleMusic", "成功获取 Bearer Token")
                        return@withContext token
                    } else {
                        Log.e("AppleMusic", "未找到 Bearer Token")
                        return@withContext ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppleMusic", "获取 Bearer Token 失败", e)
            throw e
        }
    }
}

suspend fun getSongLyrics(
    songId: String,
    storefront: String,
    token: String,
    userToken: String,
    lrcType: String,
    language: String
): Result<String> {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        try {
            val url = "https://amp-api.music.apple.com/v1/catalog/$storefront/songs/$songId/$lrcType?l=$language&extend=ttmlLocalizations"
            
            Log.d("AppleMusic", "请求 URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("Authorization", "Bearer $token")
                .header("Cookie", "media-user-token=$userToken")
                .build()
            
            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string() ?: ""
                
                Log.d("AppleMusic", "响应码: ${response.code}")
                Log.d("AppleMusic", "响应内容: $responseText")
                
                if (response.isSuccessful) {
                    val json = org.json.JSONObject(responseText)
                    val dataArray = json.optJSONArray("data")
                    
                    if (dataArray != null && dataArray.length() > 0) {
                        val attributes = dataArray.optJSONObject(0)?.optJSONObject("attributes")
                        
                        if (attributes != null) {
                            val ttml = attributes.optString("ttml", "")
                            val ttmlLocalizations = attributes.optString("ttmlLocalizations", "")
                            
                            val result = if (ttml.isNotEmpty()) ttml else ttmlLocalizations
                            if (result.isNotEmpty()) {
                                return@withContext Result.success(result)
                            }
                        }
                    }
                    
                    return@withContext Result.failure(Exception("未找到歌词数据"))
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $responseText"))
                }
            }
        } catch (e: Exception) {
            Log.e("AppleMusic", "获取歌词失败", e)
            return@withContext Result.failure(e)
        }
    }
}

suspend fun getSongLyricsViaCloudflare(
    workerUrl: String,
    songId: String,
    storefront: String,
    language: String,
    lyricsType: String,
    bearerToken: String,
    mediaUserToken: String
): Result<String> {
    return withContext(Dispatchers.IO) {
        Log.d("AppleMusic", "=== 开始 Cloudflare 请求 ===")
        Log.d("AppleMusic", "Worker URL: $workerUrl")
        
        val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .build()
        
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followSslRedirects(true)
            .followRedirects(true)
            .connectionSpecs(Arrays.asList(modernTlsSpec, ConnectionSpec.CLEARTEXT))
            .build()
        
        try {
            val jsonMediaType = "application/json".toMediaType()
            
            val jsonBody = org.json.JSONObject().apply {
                put("songId", songId)
                put("storefront", storefront)
                put("language", language)
                put("lyricsType", lyricsType)
                put("bearerToken", bearerToken)
                if (mediaUserToken.isNotEmpty()) {
                    put("mediaUserToken", mediaUserToken)
                }
            }
            
            Log.d("AppleMusic", "请求 JSON: $jsonBody")
            
            val requestBody = jsonBody.toString().toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url(workerUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "LyricBox/1.0")
                .build()
            
            Log.d("AppleMusic", "正在发送请求...")
            
            client.newCall(request).execute().use { response ->
                Log.d("AppleMusic", "收到响应，状态码: ${response.code}")
                
                val responseText = response.body?.string() ?: ""
                Log.d("AppleMusic", "响应内容长度: ${responseText.length}")
                Log.d("AppleMusic", "响应内容: $responseText")
                
                if (response.isSuccessful) {
                    try {
                        val json = org.json.JSONObject(responseText)
                        
                        // 检查是否是错误响应
                        if (json.has("error")) {
                            val errorMsg = json.optString("error", "未知错误")
                            return@withContext Result.failure(Exception("Worker 错误: $errorMsg"))
                        }
                        
                        val dataArray = json.optJSONArray("data")
                        
                        if (dataArray != null && dataArray.length() > 0) {
                            val attributes = dataArray.optJSONObject(0)?.optJSONObject("attributes")
                            
                            if (attributes != null) {
                                val ttml = attributes.optString("ttml", "")
                                val ttmlLocalizations = attributes.optString("ttmlLocalizations", "")
                                
                                val result = if (ttml.isNotEmpty()) ttml else ttmlLocalizations
                                if (result.isNotEmpty()) {
                                    Log.d("AppleMusic", "成功获取歌词，长度: ${result.length}")
                                    return@withContext Result.success(result)
                                }
                            }
                        }
                        
                        return@withContext Result.failure(Exception("未找到歌词数据"))
                    } catch (e: Exception) {
                        Log.e("AppleMusic", "解析 JSON 失败", e)
                        // 如果不是 JSON，可能直接是 TTML
                        if (responseText.contains("<tt") || responseText.contains("<?xml")) {
                            Log.d("AppleMusic", "响应是 TTML/XML 格式，直接返回")
                            return@withContext Result.success(responseText)
                        }
                        return@withContext Result.failure(Exception("解析响应失败: ${e.message}"))
                    }
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $responseText"))
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("AppleMusic", "连接超时", e)
            return@withContext Result.failure(Exception("连接超时，请检查网络或稍后重试"))
        } catch (e: java.net.ConnectException) {
            Log.e("AppleMusic", "连接失败", e)
            return@withContext Result.failure(Exception("连接失败，请检查 Worker URL 是否正确"))
        } catch (e: Exception) {
            Log.e("AppleMusic", "通过 Cloudflare 获取歌词失败", e)
            return@withContext Result.failure(e)
        }
    }
}
