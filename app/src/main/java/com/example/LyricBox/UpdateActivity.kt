package com.example.LyricBox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.UpdateChecker
import com.example.LyricBox.utils.UpdateInfo
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val updateInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("updateInfo", UpdateInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("updateInfo")
        }
        
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    UpdateScreen(
                        updateInfo = updateInfo,
                        onBack = { finish() },
                        activity = this
                    )
                }
            }
        }
    }
}

sealed class UpdateLoadState {
    data object Loading : UpdateLoadState()
    data class Success(val content: String) : UpdateLoadState()
    data class Error(val message: String) : UpdateLoadState()
}

@Composable
fun UpdateScreen(
    updateInfo: UpdateInfo?,
    onBack: () -> Unit,
    activity: UpdateActivity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQQGroupDialog by remember { mutableStateOf(false) }
    var loadState by remember { mutableStateOf<UpdateLoadState>(UpdateLoadState.Loading) }
    
    val currentVersionName = UpdateChecker.getCurrentVersionName(context)
    
    LaunchedEffect(Unit) {
        loadChangelog(activity) { result ->
            loadState = result
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "发现新版本",
            showBack = true,
            onBackClick = onBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "当前版本：$currentVersionName",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "新版本：${updateInfo?.versionName ?: "未知"}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (!updateInfo?.updateTime.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "更新日期：${updateInfo?.updateTime}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                when (loadState) {
                    is UpdateLoadState.Loading -> {
                    UpdateLoadingView()
                    }
                    is UpdateLoadState.Success -> {
                    UpdateChangelogContentView(content = (loadState as UpdateLoadState.Success).content)
                    }
                    is UpdateLoadState.Error -> {
                    UpdateErrorView(
                        message = (loadState as UpdateLoadState.Error).message,
                        onRetry = {
                        loadState = UpdateLoadState.Loading
                        loadChangelog(activity) { result ->
                            loadState = result
                        }
                        }
                    )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (updateInfo != null) {
                    if (updateInfo.hasDownloadUrl1) {
                        Button(
                            onClick = {
                                openDownloadUrl(context, updateInfo.downloadUrl1!!)
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = updateInfo.downloadName1 ?: "立即下载",
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    if (updateInfo.hasDownloadUrl2) {
                        Button(
                            onClick = {
                                openDownloadUrl(context, updateInfo.downloadUrl2!!)
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = updateInfo.downloadName2 ?: "立即下载2",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                OutlinedButton(
                    onClick = {
                        if (!joinQQGroup(context, "YAFXsv4yNW9GssJXeZVetN34UYpP6E86")) {
                            showQQGroupDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(text = "加入QQ群获取新版本", fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showQQGroupDialog) {
        QQGroupDialog(
            onDismiss = { showQQGroupDialog = false },
            onCopyGroupNumber = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("QQ群号", "964680520")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "群号已复制", Toast.LENGTH_SHORT).show()
            },
            onOpenQQ = {
                openQQApp(context)
            }
        )
    }
}

@Composable
fun UpdateLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingIndicator(modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "正在加载更新日志...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun UpdateErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "重试",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateChangelogContentView(content: String) {
    val versionSections = parseVersionSections(content)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        
        versionSections.forEach { section ->
            UpdateVersionSectionBox(section = section)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun UpdateVersionSectionBox(section: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
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
                    fontSize = 24.sp
                ),
                h2 = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 20.sp
                ),
                h3 = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 17.sp
                ),
                h4 = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp
                ),
                h5 = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp
                ),
                h6 = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp
                ),
                text = MaterialTheme.typography.bodyMedium,
                paragraph = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 19.sp,
                    fontSize = 13.sp
                ),
                list = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 19.sp,
                    fontSize = 13.sp
                ),
                code = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                quote = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
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

private fun loadChangelog(activity: UpdateActivity, onResult: (UpdateLoadState) -> Unit) {
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
            onResult(UpdateLoadState.Success(content))
        } catch (e: Exception) {
            onResult(UpdateLoadState.Error(e.message ?: "未知错误"))
        }
    }
}

private fun joinQQGroup(context: Context, key: String): Boolean {
    val intent = Intent()
    intent.data = Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

private fun openDownloadUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}

private fun openQQApp(context: Context) {
    val packageManager = context.packageManager
    
    val qqPackages = listOf(
        "com.tencent.mobileqq",
        "com.tencent.tim",
        "com.tencent.minihd.qq"
    )
    
    for (packageName in qqPackages) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "正在打开QQ...", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            continue
        }
    }
    
    try {
        val uri = Uri.parse("mqqwpa://")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "正在打开QQ...", Toast.LENGTH_SHORT).show()
        return
    } catch (e: Exception) {
    }
    
    try {
        val uri = Uri.parse("mqq://")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Toast.makeText(context, "正在打开QQ...", Toast.LENGTH_SHORT).show()
        return
    } catch (e: Exception) {
    }
    
    Toast.makeText(context, "未安装QQ，请先下载安装", Toast.LENGTH_SHORT).show()
}
