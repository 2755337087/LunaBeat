package com.example.LyricBox

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme

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
        }
    }
}
