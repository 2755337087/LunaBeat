package com.example.LyricBox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.LyricBox.ui.components.CustomDropdownMenu
import com.example.LyricBox.ui.components.MenuAnchorPosition
import com.example.LyricBox.ui.components.MenuItem
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.launch

class BottomSheetLongDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    BottomSheetDemoScreen(
                        title = "滚动BottomSheet",
                        sheetType = SheetType.Long,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

class BottomSheetShortDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    BottomSheetDemoScreen(
                        title = "普通BottomSheet",
                        sheetType = SheetType.Short,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSheetDemoScreen(
    title: String,
    sheetType: SheetType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    extraMenuItems: List<MenuItem> = emptyList()
) {
    var showSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = title,
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = { showSheet = true }) {
                Text("打开 BottomSheet")
            }
        }
    }

    if (!showSheet) return

    when (sheetType) {
        SheetType.Long -> BottomSheetWithList(
            title = title,
            onCancel = { showSheet = false },
            extraMenuItems = extraMenuItems
        )

        SheetType.Short -> BottomSheetShort(
            title = title,
            onCancel = { showSheet = false },
            extraMenuItems = extraMenuItems
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetWithList(
    title: String,
    onCancel: () -> Unit,
    extraMenuItems: List<MenuItem>
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )
    val listState = rememberLazyListState()
    val blockSheetDragFromList = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(x = 0f, y = available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(x = 0f, y = available.y)
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var menuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    val density = LocalDensity.current
    val closeAction: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onCancel()
        }
    }
    val menuItems = listOf(MenuItem(title = "关闭页面", onClick = closeAction)) + extraMenuItems

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        onDismissRequest = onCancel,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BottomSheetHeader(
                title = title,
                onMenuClick = { showMenu = true },
                onMenuPosition = { menuButtonPosition = it },
                density = density
            )

            CustomDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                items = menuItems,
                anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f),
                menuWidth = 200f
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            LoopingList(
                state = listState,
                nestedScrollConnection = blockSheetDragFromList
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetShort(
    title: String,
    onCancel: () -> Unit,
    extraMenuItems: List<MenuItem>
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMenu by remember { mutableStateOf(false) }
    var menuButtonPosition by remember { mutableStateOf<MenuAnchorPosition?>(null) }
    val density = LocalDensity.current
    val closeAction: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onCancel()
        }
    }
    val menuItems = listOf(MenuItem(title = "关闭页面", onClick = closeAction)) + extraMenuItems

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        onDismissRequest = onCancel,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(180.dp)
        ) {
            BottomSheetHeader(
                title = title,
                onMenuClick = { showMenu = true },
                onMenuPosition = { menuButtonPosition = it },
                density = density
            )

            CustomDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                items = menuItems,
                anchorPosition = menuButtonPosition ?: MenuAnchorPosition(0f, 0f),
                menuWidth = 200f
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("这是一个较低高度、无滚动内容的 BottomSheet。")
        }
    }
}

@Composable
private fun BottomSheetHeader(
    title: String,
    onMenuClick: () -> Unit,
    onMenuPosition: (MenuAnchorPosition?) -> Unit,
    density: Density
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                val centerX = bounds.center.x
                val centerY = bounds.center.y
                onMenuPosition(
                    MenuAnchorPosition(
                        x = with(density) { centerX.toDp().value },
                        y = with(density) { centerY.toDp().value }
                    )
                )
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.more),
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LoopingList(
    state: LazyListState,
    nestedScrollConnection: NestedScrollConnection
) {
    val listItems = remember { List(30) { "第 ${it + 1} 条" } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .nestedScroll(nestedScrollConnection)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = state
        ) {
            items(listItems) { item ->
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
                HorizontalDivider()
            }
        }
    }
}

private enum class SheetType {
    Long,
    Short
}

