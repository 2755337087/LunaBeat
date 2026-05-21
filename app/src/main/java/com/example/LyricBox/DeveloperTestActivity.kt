package com.example.LyricBox

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.LyricBox.lyrics.api.Netease163KeyCodec
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme
import org.json.JSONObject

class DeveloperTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    DeveloperTestScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun DeveloperTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }
    var decodeOutput by remember { mutableStateOf("") }
    
    val menuItems = listOf(
        MenuItem(
            title = "菜单1",
            subItems = listOf(
                MenuItem(title = "二级菜单1-1", onClick = {
                    Toast.makeText(context, "点击了二级菜单1-1", Toast.LENGTH_SHORT).show()
                }),
                MenuItem(title = "二级菜单1-2", onClick = {
                    Toast.makeText(context, "点击了二级菜单1-2", Toast.LENGTH_SHORT).show()
                }),
                MenuItem(title = "二级菜单1-3", onClick = {
                    Toast.makeText(context, "点击了二级菜单1-3", Toast.LENGTH_SHORT).show()
                })
            )
        ),
        MenuItem(
            title = "菜单2",
            onClick = {
                Toast.makeText(context, "点击了菜单2", Toast.LENGTH_SHORT).show()
            }
        ),
        MenuItem(
            title = "菜单3",
            subItems = listOf(
                MenuItem(title = "二级菜单3-1", onClick = {
                    Toast.makeText(context, "点击了二级菜单3-1", Toast.LENGTH_SHORT).show()
                }),
                MenuItem(title = "二级菜单3-2", onClick = {
                    Toast.makeText(context, "点击了二级菜单3-2", Toast.LENGTH_SHORT).show()
                })
            )
        ),
        MenuItem(
            title = "菜单4",
            onClick = {
                Toast.makeText(context, "点击了菜单4", Toast.LENGTH_SHORT).show()
            }
        )
    )
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "开发者测试",
            showBack = true,
            showMenu = true,
            onBackClick = onBack,
            onMenuClick = { showMenu = true },
            menuContent = { menuButtonPosition ->
                CustomDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = menuItems,
                    anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f),
                    menuWidth = 200f
                )
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "自定义菜单栏测试",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "点击右上角菜单按钮查看效果",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "功能测试",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, VerbatimLyricsActivity::class.java))
                }
            ) {
                Text("逐字歌词获取")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, AppleMusicLyricsActivity::class.java))
                }
            ) {
                Text("Apple Music 歌词获取")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, BottomSheetLongDemoActivity::class.java))
                }
            ) {
                Text("BottomSheet 长列表测试")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, BottomSheetShortDemoActivity::class.java))
                }
            ) {
                Text("BottomSheet 短内容测试")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "163key 解密测试",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "支持输入完整 comment（163 key(Don't modify):...）或仅 Base64 密文",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("163key 密文") },
                minLines = 4,
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        decodeOutput = runCatching {
                            val decoded = Netease163KeyCodec.decode(keyInput)
                            val prettyJson = decoded.musicJson?.let { json ->
                                runCatching { JSONObject(json).toString(2) }.getOrDefault(json)
                            }
                            buildString {
                                appendLine("Base64:")
                                appendLine(decoded.normalizedBase64)
                                appendLine()
                                appendLine("解密明文:")
                                appendLine(decoded.plaintext)
                                if (prettyJson != null) {
                                    appendLine()
                                    appendLine("music JSON:")
                                    append(prettyJson)
                                }
                            }
                        }.getOrElse { error ->
                            "解密失败：${error.message ?: "未知错误"}"
                        }
                    }
                ) {
                    Text("解密")
                }

                Spacer(modifier = Modifier.height(0.dp).weight(1f, fill = true))

                OutlinedButton(
                    onClick = {
                        keyInput = ""
                        decodeOutput = ""
                    }
                ) {
                    Text("清空")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = decodeOutput,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                label = { Text("解密结果") },
                readOnly = true
            )
        }
    }
}
