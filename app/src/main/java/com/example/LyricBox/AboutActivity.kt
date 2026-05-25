package com.example.LyricBox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme
import com.example.LyricBox.utils.PiracyChecker
import com.example.LyricBox.utils.PiracyCheckResult
import com.example.LyricBox.utils.UpdateChecker
import com.example.LyricBox.utils.UpdateInfo
import com.example.LyricBox.utils.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.SystemClock

class AboutActivity : ComponentActivity() {
    
    private var piracyCheckResult by mutableStateOf<PiracyCheckResult?>(null)
    private var showPiracyWarning by mutableStateOf(false)
    private var isDebugDevice by mutableStateOf(false)
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val checkResult = PiracyChecker.checkAll(this)
        isDebugDevice = PiracyChecker.isDebugDevice(this)
        if (checkResult.isPirated) {
            piracyCheckResult = checkResult
            showPiracyWarning = true
        }
        
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    AboutScreen(
                        onBack = { finish() },
                        isDebugDevice = isDebugDevice
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

enum class UpdateCheckState {
    IDLE,
    CHECKING,
    NO_UPDATE,
    ERROR,
    UPDATE_AVAILABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    isDebugDevice: Boolean = false,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQQGroupDialog by remember { mutableStateOf(false) }
    var showSpecialThanks by remember { mutableStateOf(false) }
    var updateCheckState by remember { mutableStateOf(UpdateCheckState.IDLE) }
    var latestVersionName by remember { mutableStateOf<String?>(null) }
    var pendingUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val specialThanksSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showDeveloperTest by remember { mutableStateOf(isDebugDevice) }
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(updateCheckState) {
        if (updateCheckState == UpdateCheckState.ERROR) {
            delay(2000)
            updateCheckState = UpdateCheckState.IDLE
        }
    }
    
    val versionName = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "未知"
    } catch (e: PackageManager.NameNotFoundException) {
        "未知"
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "关于",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Icon(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "应用图标",
                    modifier = Modifier
                        .size(130.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val currentTime = SystemClock.elapsedRealtime()
                            if (currentTime - lastClickTime < 1000) {
                                clickCount++
                                if (clickCount >= 10) {
                                    showDeveloperTest = true
                                    clickCount = 0
                                }
                            } else {
                                clickCount = 1
                            }
                            lastClickTime = currentTime
                        },
                    tint = Color.Unspecified
                )
            }
            
           // Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "LunaBeat",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "版本：$versionName",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AboutItem(
                    title = "加入QQ群",
                    onClick = {
                        if (!joinQQGroup(context, "YAFXsv4yNW9GssJXeZVetN34UYpP6E86")) {
                            showQQGroupDialog = true
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AboutItem(
                    title = "访问GitHub",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/2755337087/LunaBeat"))
                        context.startActivity(intent)
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AboutItem(
                    title = "使用说明",
                    onClick = {
                        context.startActivity(Intent(context, InstructionsActivity::class.java))
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val highlightTitle = when (updateCheckState) {
                    UpdateCheckState.CHECKING -> "正在检测更新..."
                    UpdateCheckState.NO_UPDATE -> "已是最新版，最新版本 ${latestVersionName ?: versionName}"
                    UpdateCheckState.ERROR -> "检测失败"
                    UpdateCheckState.UPDATE_AVAILABLE -> "检测到新版本"
                    UpdateCheckState.IDLE -> null
                }
                
                AboutItem(
                    title = "检测新版本",
                    onClick = {
                        when (updateCheckState) {
                            UpdateCheckState.CHECKING -> {}
                            else -> {
                                checkUpdateManually(context) { result ->
                                    when (result) {
                                        is UpdateResult.UpdateAvailable -> {
                                            val intent = Intent(context, UpdateActivity::class.java).apply {
                                                putExtra("updateInfo", result.updateInfo)
                                            }
                                            context.startActivity(intent)
                                            updateCheckState = UpdateCheckState.IDLE
                                        }
                                        is UpdateResult.NoUpdate -> {
                                            latestVersionName = result.latestVersion
                                            updateCheckState = UpdateCheckState.NO_UPDATE
                                        }
                                        is UpdateResult.Timeout -> {
                                            updateCheckState = UpdateCheckState.ERROR
                                        }
                                        is UpdateResult.Error -> {
                                            updateCheckState = UpdateCheckState.ERROR
                                        }
                                    }
                                }
                                updateCheckState = UpdateCheckState.CHECKING
                            }
                        }
                    },
                    highlightTitle = highlightTitle,
                    isHighlighted = updateCheckState != UpdateCheckState.IDLE
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AboutItem(
                    title = "更新日志",
                    onClick = {
                        context.startActivity(Intent(context, ChangelogActivity::class.java))
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AboutItem(
                    title = "特别感谢",
                    onClick = {
                        showSpecialThanks = true
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AboutItem(
                    title = "捐赠支持",
                    onClick = {
                        context.startActivity(Intent(context, DonateActivity::class.java))
                    }
                )
                
                if (showDeveloperTest) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AboutItem(
                        title = "开发者测试",
                        onClick = {
                            context.startActivity(Intent(context, DeveloperTestActivity::class.java))
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp + bottomContentPadding))
            }
        }
    }
    
    if (showSpecialThanks) {
        SpecialThanksBottomSheet(
            sheetState = specialThanksSheetState,
            onDismiss = { showSpecialThanks = false }
        )
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
fun AboutItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightTitle: String? = null,
    isHighlighted: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    val displayTitle = if (isHighlighted && highlightTitle != null) highlightTitle else title
    val textColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isHighlighted || highlightTitle == "检测到新版本"
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = displayTitle,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith 
                fadeOut(animationSpec = tween(300))
            },
            label = "titleAnimation",
            modifier = Modifier.weight(1f)
        ) { text ->
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
        if (!isHighlighted) {
            Icon(
                painter = painterResource(id = R.drawable.back__left),
                contentDescription = "箭头",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun QQGroupDialog(
    onDismiss: () -> Unit,
    onCopyGroupNumber: () -> Unit,
    onOpenQQ: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入QQ群") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "群号：964680520",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请在QQ中搜索群号加入",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onCopyGroupNumber) {
                    Text("复制群号")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onOpenQQ) {
                    Text("打开QQ")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
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

private fun checkUpdateManually(context: Context, onResult: (UpdateResult) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = withContext(Dispatchers.IO) {
            UpdateChecker.checkForUpdate(context)
        }
        
        onResult(result)
    }
}

data class OpenSourceProject(
    val name: String,
    val url: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialThanksBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val technicalProjects = listOf(
        // OpenSourceProject(
        //     name = "Trae CN",
        //     url = "https://www.trae.cn/",
        //     description = "AI编程助手，提供代码编写支持"
        // ),
        // OpenSourceProject(
        //     name = "ChatGPT",
        //     url = "https://chatgpt.com/",
        //     description = "AI助手，提供技术问答支持"
        // ),
        OpenSourceProject(
            name = "TagLib",
            url = "https://github.com/taglib/taglib",
            description = "音频元数据读写库"
        ),
        OpenSourceProject(
            name = "Lyricon",
            url = "https://github.com/tomakino/lyricon",
            description = "歌词同步播放器，提供技术参考"
        ),
        OpenSourceProject(
            name = "SongSync",
            url = "https://github.com/Lambada10/SongSync",
            description = "Android歌词下载应用，提供功能参考"
        ),
        OpenSourceProject(
            name = "LDDC",
            url = "https://github.com/chenmozhijin/LDDC",
            description = "精准歌词下载匹配工具，提供功能参考"
        ),
        OpenSourceProject(
            name = "OpenCC",
            url = "https://github.com/BYVoid/OpenCC",
            description = ""
        ),
        OpenSourceProject(
            name = "any-listen-extension-online-metadata",
            url = "https://github.com/any-listen/any-listen-extension-online-metadata",
            description = "音频元数据搜索插件，提供功能参考"
        ),
        OpenSourceProject(
            name = "163Music2Tag",
            url = "https://gitee.com/Wangs-official/163Music2Tag",
            description = ""
        ),
        OpenSourceProject(
            name = "TagLib(Kyant0)",
            url = "https://github.com/Kyant0/taglib",
            description = ""
        ),
        OpenSourceProject(
            name = "uCrop",
            url = "https://github.com/Yalantis/uCrop",
            description = ""
        ),
        OpenSourceProject(
            name = "accompanist-lyrics-ui",
            url = "https://github.com/6xingyv/accompanist-lyrics-ui",
            description = ""
        ),
        OpenSourceProject(
            name = "EdgeTranslucent",
            url = "https://github.com/qinci/EdgeTranslucent",
            description = ""
        ),
        OpenSourceProject(
            name = "lottie-android",
            url = "https://github.com/airbnb/lottie-android",
            description = ""
        ),
        OpenSourceProject(
            name = "ICU Transliterator",
            url = "https://unicode-org.github.io/icu/userguide/transforms/general/",
            description = ""
        ),
        OpenSourceProject(
            name = "AndroidLiquidGlass",
            url = "https://github.com/Kyant0/AndroidLiquidGlass",
            description = ""
        ),
    )
    
    val inspirationProjects = listOf(
        OpenSourceProject(
            name = "amll-ttml-tool",
            url = "https://github.com/amll-dev/amll-ttml-tool",
            description = ""
        ),
        OpenSourceProject(
            name = "音乐标签",
            url = "https://www.cnblogs.com/vinlxc/p/11932130.html",
            description = ""
        )
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "特别感谢",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "感谢以下项目为本应用提供帮助：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            technicalProjects.forEach { project ->
                OpenSourceProjectItem(
                    project = project,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(project.url))
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "以下项目提供灵感支持：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            inspirationProjects.forEach { project ->
                OpenSourceProjectItem(
                    project = project,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(project.url))
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun OpenSourceProjectItem(
    project: OpenSourceProject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = project.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.back__left),
            contentDescription = "访问",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
