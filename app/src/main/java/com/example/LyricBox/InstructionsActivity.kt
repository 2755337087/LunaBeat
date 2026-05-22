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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LoadingIndicator
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

class InstructionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    InstructionsScreen(
                        onBack = { finish() },
                        activity = this
                    )
                }
            }
        }
    }
}

sealed class LoadState {
    data object Loading : LoadState()
    data class Success(val content: String) : LoadState()
    data class Error(val message: String) : LoadState()
}

@Composable
fun InstructionsScreen(
    onBack: () -> Unit,
    activity: InstructionsActivity,
    modifier: Modifier = Modifier
) {
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    
    LaunchedEffect(Unit) {
        loadReadme(activity) { result ->
            loadState = result
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "使用说明",
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
                is LoadState.Loading -> {
                    LoadingView()
                }
                is LoadState.Success -> {
                    val filteredContent = filterMarkdownContent((loadState as LoadState.Success).content)
                    MarkdownContentView(content = filteredContent)
                }
                is LoadState.Error -> {
                    ErrorView(
                        message = (loadState as LoadState.Error).message,
                        onRetry = {
                            loadState = LoadState.Loading
                            loadReadme(activity) { result ->
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
fun LoadingView() {
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
                LoadingIndicator(modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "正在加载使用说明...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
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
fun MarkdownContentView(content: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Markdown(
                content = content,
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
                        fontSize = 24.sp
                    ),
                    h3 = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 20.sp
                    ),
                    h4 = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    ),
                    h5 = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp
                    ),
                    h6 = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 15.sp
                    ),
                    text = MaterialTheme.typography.bodyMedium,
                    paragraph = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        fontSize = 15.sp
                    ),
                    list = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        fontSize = 15.sp
                    ),
                    code = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp
                    ),
                    quote = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun filterMarkdownContent(content: String): String {
    val lines = content.lines()
    val filteredLines = mutableListOf<String>()
    var skipSection = false
    val skipKeywords = listOf("截图预览", "特别感谢", "关于")
    
    for (line in lines) {
        val trimmedLine = line.trim()
        val isH1 = trimmedLine.startsWith("# ")
        
        if (isH1) {
            val h1Title = trimmedLine.removePrefix("# ").trim()
            skipSection = skipKeywords.any { h1Title.contains(it) }
        }
        
        if (!skipSection) {
            filteredLines.add(line)
        }
    }
    
    return filteredLines.joinToString("\n")
}

private fun loadReadme(activity: InstructionsActivity, onResult: (LoadState) -> Unit) {
    activity.lifecycleScope.launch {
        try {
            val content = withContext(Dispatchers.IO) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url("https://gitee.com/lb244394/lyric-box/raw/master/README.md")
                    .build()
                
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP错误: ${response.code}")
                }
                
                response.body?.string() ?: throw Exception("响应内容为空")
            }
            onResult(LoadState.Success(content))
        } catch (e: Exception) {
            onResult(LoadState.Error(e.message ?: "未知错误"))
        }
    }
}
