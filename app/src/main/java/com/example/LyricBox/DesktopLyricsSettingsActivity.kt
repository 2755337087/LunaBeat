package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlin.math.roundToInt

class DesktopLyricsSettingsActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }
                            Text(
                                text = "桌面歌词",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                ) { padding ->
                    DesktopLyricsSettingsContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopLyricsSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember {
        context.getSharedPreferences(LyricPreviewActivity.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val loaded = remember { DesktopLyricsSettingsStore.load(prefs) }
    var enabled by remember { mutableStateOf(loaded.enabled) }
    var widthPercent by remember { mutableIntStateOf(loaded.widthPercent) }
    var fontSize by remember { mutableFloatStateOf(loaded.fontSizeSp) }
    var fontWeight by remember { mutableIntStateOf(loaded.fontWeight) }
    var strokeEnabled by remember { mutableStateOf(loaded.strokeEnabled) }
    var showTranslation by remember { mutableStateOf(loaded.showTranslation) }
    var useCustomFont by remember { mutableStateOf(loaded.useCustomFont) }
    var align by remember { mutableIntStateOf(loaded.align) }
    var xPercent by remember { mutableIntStateOf(loaded.xPercent) }
    var yPercent by remember { mutableIntStateOf(loaded.yPercent) }
    var colorKey by remember { mutableStateOf(loaded.colorKey) }
    var pendingOverlayPermissionEnable by remember { mutableStateOf(false) }

    fun persistCurrent() {
        DesktopLyricsSettingsStore.save(
            context = context,
            settings = DesktopLyricsSettings(
                enabled = enabled,
                widthPercent = widthPercent,
                fontSizeSp = fontSize,
                fontWeight = fontWeight,
                strokeEnabled = strokeEnabled,
                showTranslation = showTranslation,
                useCustomFont = useCustomFont,
                align = align,
                xPercent = xPercent,
                yPercent = yPercent,
                colorKey = colorKey
            )
        )
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (DesktopLyricsPermission.canDrawOverlays(context)) {
            enabled = true
            pendingOverlayPermissionEnable = false
            persistCurrent()
        }
    }

    DisposableEffect(lifecycleOwner, pendingOverlayPermissionEnable) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingOverlayPermissionEnable) {
                if (DesktopLyricsPermission.canDrawOverlays(context)) {
                    enabled = true
                    pendingOverlayPermissionEnable = false
                    persistCurrent()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DesktopSwitchRow(
                title = "桌面歌词总开关",
                checked = enabled,
                onCheckedChange = { checked ->
                    if (!checked) {
                        enabled = false
                        pendingOverlayPermissionEnable = false
                        persistCurrent()
                        return@DesktopSwitchRow
                    }
                    if (!DesktopLyricsPermission.canDrawOverlays(context)) {
                        pendingOverlayPermissionEnable = true
                        overlayPermissionLauncher.launch(DesktopLyricsPermission.overlaySettingsIntent(context))
                        Toast.makeText(context, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                    } else {
                        enabled = true
                        pendingOverlayPermissionEnable = false
                        persistCurrent()
                    }
                }
            )
        }
        item {
            DesktopSliderRow(
                title = "歌词组件宽度",
                valueText = "$widthPercent%",
                value = widthPercent.toFloat(),
                valueRange = 50f..100f,
                steps = 9,
                onValueChange = {
                    widthPercent = it.roundToInt().coerceIn(50, 100)
                    persistCurrent()
                }
            )
        }
        item {
            DesktopSliderRow(
                title = "字体大小",
                valueText = "${fontSize.roundToInt()}sp",
                value = fontSize,
                valueRange = 16f..56f,
                steps = 19,
                onValueChange = {
                    fontSize = it.coerceIn(16f, 56f)
                    persistCurrent()
                }
            )
        }
        item {
            DesktopSliderRow(
                title = "字体粗细",
                valueText = "$fontWeight",
                value = fontWeight.toFloat(),
                valueRange = 400f..900f,
                steps = 9,
                onValueChange = {
                    fontWeight = (it.roundToInt() / 50 * 50).coerceIn(400, 900)
                    persistCurrent()
                }
            )
        }
        item {
            DesktopSwitchRow(
                title = "字体描边",
                checked = strokeEnabled,
                onCheckedChange = {
                    strokeEnabled = it
                    persistCurrent()
                }
            )
        }
        item {
            DesktopSwitchRow(
                title = "显示翻译",
                checked = showTranslation,
                onCheckedChange = {
                    showTranslation = it
                    persistCurrent()
                }
            )
        }
        item {
            DesktopSwitchRow(
                title = "使用自定义字体",
                checked = useCustomFont,
                onCheckedChange = {
                    useCustomFont = it
                    persistCurrent()
                }
            )
        }
        item {
            DesktopOptionGroup(
                title = "对齐方式",
                options = listOf(
                    LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_LEFT to "居左",
                    LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_CENTER to "居中",
                    LyricPreviewActivity.DESKTOP_LYRIC_ALIGN_RIGHT to "居右"
                ),
                selected = align,
                onSelected = {
                    align = it
                    persistCurrent()
                }
            )
        }
        item {
            DesktopSliderRow(
                title = "歌词显示 X 轴位置",
                valueText = "$xPercent%",
                value = xPercent.toFloat(),
                valueRange = 0f..100f,
                steps = 9,
                onValueChange = {
                    xPercent = it.roundToInt().coerceIn(0, 100)
                    persistCurrent()
                }
            )
        }
        item {
            DesktopSliderRow(
                title = "歌词显示 Y 轴位置",
                valueText = "$yPercent%",
                value = yPercent.toFloat(),
                valueRange = 0f..100f,
                steps = 9,
                onValueChange = {
                    yPercent = it.roundToInt().coerceIn(0, 100)
                    persistCurrent()
                }
            )
        }
        item {
            Text(
                text = "歌词颜色",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DesktopLyricsSettingsStore.colorPresets.forEach { (key, color) ->
                    val selected = colorKey == key
                    Box(
                        modifier = Modifier
                            .size(if (selected) 40.dp else 34.dp)
                            .background(color, CircleShape)
                            .clickable {
                                colorKey = key
                                persistCurrent()
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DesktopSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DesktopSliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun DesktopOptionGroup(
    title: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(value) }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.material3.RadioButton(
                    selected = selected == value,
                    onClick = { onSelected(value) }
                )
            }
        }
    }
}

private object DesktopLyricsPermission {
    fun canDrawOverlays(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
}
