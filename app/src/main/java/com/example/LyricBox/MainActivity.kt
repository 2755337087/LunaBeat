package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.PiracyChecker
import com.example.LyricBox.utils.PiracyCheckResult
import com.example.LyricBox.utils.SecureStorage
import com.example.LyricBox.utils.UpdateChecker
import com.example.LyricBox.utils.UpdateInfo
import com.example.LyricBox.utils.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    
    private var piracyCheckResult by mutableStateOf<PiracyCheckResult?>(null)
    private var showPiracyWarning by mutableStateOf(false)
    
    companion object {
        private var hasCheckedUpdate = false
        private var cachedNotice: String? = null
        private var cachedShowNotice: Boolean = false
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化安全存储
        SecureStorage.initializeIfNeeded(this)
        
        val checkResult = PiracyChecker.checkAll(this)
        if (checkResult.isPirated) {
            piracyCheckResult = checkResult
            showPiracyWarning = true
        }
        
        setContent {
            歌词转换Theme {
                val context = LocalContext.current
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                var noticeText by remember { mutableStateOf<String?>(null) }
                var showNotice by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    if (!hasCheckedUpdate) {
                        hasCheckedUpdate = true
                        delay(800)
                        withContext(Dispatchers.IO) {
                            val result = UpdateChecker.checkForUpdate(context)
                            if (result is UpdateResult.UpdateAvailable) {
                                updateInfo = result.updateInfo
                            }
                            if (result is UpdateResult.UpdateAvailable || result is UpdateResult.NoUpdate) {
                                val info = when (result) {
                                    is UpdateResult.UpdateAvailable -> result.updateInfo
                                    is UpdateResult.NoUpdate -> {
                                        try {
                                            val url = java.net.URL(UpdateChecker.UPDATE_URL)
                                            val connection = url.openConnection() as java.net.HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.connectTimeout = 3000
                                            connection.readTimeout = 3000
                                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                                            connection.disconnect()
                                            val json = org.json.JSONObject(response)
                                            UpdateInfo(
                                                versionCode = json.optInt("versionCode", 0),
                                                versionName = json.optString("versionName", ""),
                                                changelog = json.optString("changelog", ""),
                                                updateTime = json.optString("updateTime", ""),
                                                downloadName1 = json.optString("downloadName1", null).takeIf { it.isNotEmpty() },
                                                downloadName2 = json.optString("downloadName2", null).takeIf { it.isNotEmpty() },
                                                downloadUrl1 = json.optString("downloadUrl1", null).takeIf { it.isNotEmpty() },
                                                downloadUrl2 = json.optString("downloadUrl2", null).takeIf { it.isNotEmpty() },
                                                notice = json.optString("notice", null).takeIf { it.isNotEmpty() },
                                                showNotice = json.optBoolean("showNotice", false),
                                                amUrl = json.optString("AMURL", null).takeIf { it.isNotEmpty() },
                                                amUrlName = json.optString("AMURLname", null).takeIf { it.isNotEmpty() },
                                                amUrlCountry = json.optString("AMURLcountry", null).takeIf { it.isNotEmpty() },
                                                amUrlNameContributor = json.optString("AMURLname_contributor", null).takeIf { it.isNotEmpty() },
                                                amUrlCountryContributor = json.optString("AMURLcountry_contributor", null).takeIf { it.isNotEmpty() },
                                                noticeContributor = json.optString("Notice_contributor", null).takeIf { it.isNotEmpty() }
                                            )
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    else -> null
                                }
                                if (info != null) {
                                    cachedNotice = info.notice
                                    cachedShowNotice = info.showNotice
                                    noticeText = info.notice
                                    if (info.showNotice) {
                                        delay(100)
                                        showNotice = true
                                    }
                                    val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                                    prefs.edit().apply {
                                        info.amUrl?.let { putString("amUrl", it) }
                                        info.amUrlName?.let { putString("amUrlName", it) }
                                        info.amUrlCountry?.let { putString("amUrlCountry", it) }
                                        info.amUrlNameContributor?.let { putString("amUrlNameContributor", it) }
                                        info.amUrlCountryContributor?.let { putString("amUrlCountryContributor", it) }
                                        info.noticeContributor?.let { putString("noticeContributor", it) }
                                        apply()
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (updateInfo != null) {
                    val intent = Intent(context, UpdateActivity::class.java).apply {
                        putExtra("updateInfo", updateInfo)
                    }
                    context.startActivity(intent)
                    updateInfo = null
                }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    MainScreen(
                        onNavigateToLyricTiming = { 
                            startActivity(Intent(this, LyricTimingActivity::class.java))
                        },
                        onNavigateToMusicLibrary = {
                            startActivity(Intent(this, MusicLibraryActivity::class.java))
                        },
                        onNavigateToSettings = {
                            startActivity(Intent(this, SettingsActivity::class.java))
                        },
                        onNavigateToAbout = {
                            startActivity(Intent(this, AboutActivity::class.java))
                        },
                        noticeText = noticeText,
                        showNotice = showNotice,
                        onDismissNotice = {
                            showNotice = false
                            cachedShowNotice = false
                        }
                    )
                }
                
                if (showPiracyWarning && piracyCheckResult != null) {
                    com.example.LyricBox.ui.components.PiracyWarningDialog(
                        onExit = {
                            finish()
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToLyricTiming: () -> Unit,
    onNavigateToMusicLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    noticeText: String? = null,
    showNotice: Boolean = false,
    onDismissNotice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density)
    val headBarHeight = 56.dp
    val noticeTopPadding = with(density) { 
        statusBarHeight.toDp() + headBarHeight + 8.dp 
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommonHeadBar(
                title = "LyricBox",
                showBack = false,
                showMenu = false
            )
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "欢迎使用",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedButton(
                    onClick = onNavigateToLyricTiming,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(text = "歌词打轴")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onNavigateToMusicLibrary,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(text = "音乐库")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(text = "设置")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onNavigateToAbout,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(text = "关于")
                }
            }
        }
        
        if (!noticeText.isNullOrEmpty()) {
            AnimatedVisibility(
                visible = showNotice,
                enter = fadeIn(animationSpec = tween(400)) + 
                         scaleIn(
                             initialScale = 0.3f,
                             animationSpec = spring(
                                 dampingRatio = Spring.DampingRatioMediumBouncy,
                                 stiffness = Spring.StiffnessLow
                             )
                         ) + slideInVertically(
                             initialOffsetY = { -it },
                             animationSpec = spring(
                                 dampingRatio = Spring.DampingRatioMediumBouncy,
                                 stiffness = Spring.StiffnessLow
                             )
                         ),
                exit = fadeOut(animationSpec = tween(200)) + 
                       scaleOut(
                           targetScale = 0.5f,
                           animationSpec = tween(200)
                       ) + slideOutVertically(
                           targetOffsetY = { -it },
                           animationSpec = tween(200)
                       ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = noticeTopPadding, start = 16.dp, end = 16.dp)
            ) {
                NoticeBanner(
                    text = noticeText,
                    onDismiss = onDismissNotice
                )
            }
        }
    }
}

@Composable
fun NoticeBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp)
        )
        
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭公告",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    歌词转换Theme {
        MainScreen(
            onNavigateToLyricTiming = {},
            onNavigateToMusicLibrary = {},
            onNavigateToSettings = {},
            onNavigateToAbout = {}
        )
    }
}
