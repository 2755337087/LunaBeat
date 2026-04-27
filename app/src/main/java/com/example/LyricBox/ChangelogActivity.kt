package com.example.LyricBox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChangelogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    ChangelogScreen(
                        onBack = { finish() },
                        activity = this
                    )
                }
            }
        }
    }
}

sealed class ChangelogLoadState {
    data object Loading : ChangelogLoadState()
    data class Success(val content: String) : ChangelogLoadState()
    data class Error(val message: String) : ChangelogLoadState()
}

@Composable
fun ChangelogScreen(
    onBack: () -> Unit,
    activity: ChangelogActivity,
    modifier: Modifier = Modifier
) {
    var loadState by remember { mutableStateOf<ChangelogLoadState>(ChangelogLoadState.Loading) }
    
    LaunchedEffect(Unit) {
        loadChangelog(activity) { result ->
            loadState = result
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "更新日志",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (loadState) {
                is ChangelogLoadState.Loading -> {
                    ChangelogLoadingView()
                }
                is ChangelogLoadState.Success -> {
                    ChangelogMarkdownContentView(content = (loadState as ChangelogLoadState.Success).content)
                }
                is ChangelogLoadState.Error -> {
                    ChangelogErrorView(
                        message = (loadState as ChangelogLoadState.Error).message,
                        onRetry = {
                            loadState = ChangelogLoadState.Loading
                            loadChangelog(activity) { result ->
                                loadState = result
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChangelogLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(40.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "正在加载更新日志...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ChangelogErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "加载失败",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "重试",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChangelogMarkdownContentView(content: String) {
    val versionSections = parseVersionSections(content)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            versionSections.forEach { section ->
                VersionSectionBox(section = section)
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VersionSectionBox(section: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Markdown(
            content = section,
            colors = markdownColor(
                text = MaterialTheme.colorScheme.onSurface,
                codeText = MaterialTheme.colorScheme.onPrimaryContainer,
                codeBackground = MaterialTheme.colorScheme.primaryContainer,
                inlineCodeText = MaterialTheme.colorScheme.onPrimaryContainer,
                inlineCodeBackground = MaterialTheme.colorScheme.primaryContainer,
                linkText = MaterialTheme.colorScheme.primary
            ),
            typography = markdownTypography(
                h1 = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp
                ),
                h2 = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 22.sp
                ),
                h3 = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 18.sp
                ),
                h4 = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                ),
                h5 = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 15.sp
                ),
                h6 = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 14.sp
                ),
                text = MaterialTheme.typography.bodyMedium,
                paragraph = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                ),
                list = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                ),
                code = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                ),
                quote = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun parseVersionSections(content: String): List<String> {
    val lines = content.lines()
    val sections = mutableListOf<String>()
    val currentSection = mutableListOf<String>()
    
    for (line in lines) {
        if (line.startsWith("## ")) {
            if (currentSection.isNotEmpty()) {
                sections.add(currentSection.joinToString("\n"))
                currentSection.clear()
            }
        }
        currentSection.add(line)
    }
    
    if (currentSection.isNotEmpty()) {
        sections.add(currentSection.joinToString("\n"))
    }
    
    return sections
}

private fun loadChangelog(activity: ChangelogActivity, onResult: (ChangelogLoadState) -> Unit) {
    activity.lifecycleScope.launch {
        try {
            val content = withContext(Dispatchers.IO) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url("https://gitee.com/lb244394/lyric-box/raw/master/CHANGELOG.md")
                    .build()
                
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP错误: ${response.code}")
                }
                
                response.body?.string() ?: throw Exception("响应内容为空")
            }
            onResult(ChangelogLoadState.Success(content))
        } catch (e: Exception) {
            onResult(ChangelogLoadState.Error(e.message ?: "未知错误"))
        }
    }
}
