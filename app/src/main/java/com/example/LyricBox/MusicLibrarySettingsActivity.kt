package com.example.LyricBox

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme

class MusicLibrarySettingsActivity : ComponentActivity() {
    private var isFirstSetup = false
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "需要存储权限才能扫描音频文件", Toast.LENGTH_LONG).show()
        }
    }
    
    private var onFolderSelected: ((String) -> Unit)? = null
    private var onExcludeFolderSelected: ((String) -> Unit)? = null
    
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
            
            val path = convertUriToPath(it)
            if (path != null) {
                onFolderSelected?.invoke(path)
                onExcludeFolderSelected?.invoke(path)
            } else {
                Toast.makeText(this, "无法获取文件夹路径", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun convertUriToPath(uri: Uri): String? {
        var path: String? = null
        
        try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            
            if (split.size >= 2) {
                val type = split[0]
                val relativePath = split[1]
                
                if ("primary".equals(type, ignoreCase = true)) {
                    path = "/storage/emulated/0/$relativePath"
                } else {
                    path = "/storage/$type/$relativePath"
                }
            } else if (split.size == 1) {
                path = "/storage/emulated/0/${split[0]}"
            }
        } catch (e: Exception) {
            Log.e("MusicLibrarySettings", "Error converting URI to path", e)
        }
        
        if (path == null) {
            try {
                path = uri.path
                path?.let {
                    if (it.startsWith("/tree/")) {
                        val treePath = it.substringAfter("/tree/")
                        if (treePath.contains(":")) {
                            val typePart = treePath.substringBefore(":")
                            val pathPart = treePath.substringAfter(":")
                            
                            if ("primary".equals(typePart, ignoreCase = true)) {
                                path = "/storage/emulated/0/$pathPart"
                            } else {
                                path = "/storage/$typePart/$pathPart"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicLibrarySettings", "Fallback path conversion failed", e)
            }
        }
        
        if (path != null) {
            val file = File(path)
            if (file.exists() && file.isDirectory) {
                return path
            }
        }
        
        return path
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstSetup = intent.getBooleanExtra("isFirstSetup", false)
        enableEdgeToEdge()
        
        requestStoragePermission()
        
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
                ) { paddingValues ->
                    MusicLibrarySettingsScreen(
                        onBack = { finish() },
                        onConfirm = { 
                            if (isFirstSetup) {
                                startActivity(Intent(this, MusicLibraryActivity::class.java))
                            }
                            finish()
                        },
                        onLaunchFolderPicker = { callback ->
                            onFolderSelected = callback
                            folderPickerLauncher.launch(null)
                        },
                        onLaunchExcludeFolderPicker = { callback ->
                            onExcludeFolderSelected = callback
                            folderPickerLauncher.launch(null)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }
}

@Composable
fun MusicLibrarySettingsScreen(
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onLaunchFolderPicker: ((String) -> Unit) -> Unit,
    onLaunchExcludeFolderPicker: ((String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddExcludeFolderDialog by remember { mutableStateOf(false) }
    var excludeShortAudio by remember { mutableStateOf(prefs.getBoolean("excludeShortAudio", true)) }
    val folderList = remember { mutableStateListOf<String>() }
    val excludeFolderList = remember { mutableStateListOf<String>() }
    
    var folderToDelete by remember { mutableStateOf<String?>(null) }
    var excludeFolderToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteExcludeConfirmDialog by remember { mutableStateOf(false) }
    
    val sortType = remember {
        mutableStateOf(
            try {
                SortType.valueOf(prefs.getString("sortType", SortType.FILE_NAME.name) ?: SortType.FILE_NAME.name)
            } catch (e: Exception) { SortType.FILE_NAME }
        )
    }
    val sortOrder = remember {
        mutableStateOf(
            try {
                SortOrder.valueOf(prefs.getString("sortOrder", SortOrder.ASC.name) ?: SortOrder.ASC.name)
            } catch (e: Exception) { SortOrder.ASC }
        )
    }
    
    var hasInitialized by remember { mutableStateOf(false) }
    if (!hasInitialized) {
        val savedFolders = prefs.getStringSet("musicFolders", emptySet()) ?: emptySet()
        if (folderList.isEmpty() && savedFolders.isNotEmpty()) {
            folderList.addAll(savedFolders)
        }
        
        val savedExcludeFolders = prefs.getStringSet("excludeFolders", emptySet()) ?: emptySet()
        if (excludeFolderList.isEmpty() && savedExcludeFolders.isNotEmpty()) {
            excludeFolderList.addAll(savedExcludeFolders)
        }
        hasInitialized = true
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "目录设置",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "音频目录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "设置读取音频文件的目录",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            folderList.forEach { folder ->
                FolderItem(
                    path = folder,
                    showDelete = folderList.size > 1,
                    onDelete = {
                        folderToDelete = folder
                        showDeleteConfirmDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            OutlinedButton(
                onClick = { showAddFolderDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = com.example.LyricBox.R.drawable.add),
                    contentDescription = "添加目录",
                    modifier = Modifier.padding(end = 8.dp).size(16.dp)
                )
                Text("添加目录")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "排序设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "选择音频列表的排序方式",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "排序依据",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SortTypeGrid(
                sortTypes = SortType.values().toList(),
                selectedSortType = sortType.value,
                onSortTypeSelected = { sortType.value = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "排序顺序",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SortOrderGrid(
                sortOrders = SortOrder.values().toList(),
                selectedSortOrder = sortOrder.value,
                onSortOrderSelected = { sortOrder.value = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "过滤设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FilterCheckboxItem(
                text = "过滤60秒以下的音频文件",
                checked = excludeShortAudio,
                onCheckedChange = { excludeShortAudio = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "过滤目录",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "音乐库列表不会显示这些目录中的歌曲",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            excludeFolderList.forEachIndexed { index, folder ->
                androidx.compose.runtime.key(folder) {
                    FolderItem(
                        path = folder,
                        showDelete = true,
                        onDelete = {
                            excludeFolderToDelete = folder
                            showDeleteExcludeConfirmDialog = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            OutlinedButton(
                onClick = { showAddExcludeFolderDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = com.example.LyricBox.R.drawable.add),
                    contentDescription = "添加目录",
                    modifier = Modifier.padding(end = 8.dp).size(16.dp)
                )
                Text("添加目录")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    prefs.edit()
                        .putStringSet("musicFolders", folderList.toSet())
                        .putStringSet("excludeFolders", excludeFolderList.toSet())
                        .putBoolean("excludeShortAudio", excludeShortAudio)
                        .putString("sortType", sortType.value.name)
                        .putString("sortOrder", sortOrder.value.name)
                        .putBoolean("hasSetup", true)
                        .apply()
                    onConfirm()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = folderList.isNotEmpty()
            ) {
                Text("确认")
            }
        }
    }
    
    if (showAddFolderDialog) {
        AddFolderDialog(
            existingFolders = folderList.toList(),
            onDismiss = { showAddFolderDialog = false },
            onAdd = { path ->
                if (!folderList.contains(path)) {
                    folderList.add(path)
                }
                showAddFolderDialog = false
            },
            onLaunchFolderPicker = onLaunchFolderPicker
        )
    }
    
    if (showAddExcludeFolderDialog) {
        AddExcludeFolderDialog(
            existingFolders = folderList.toList(),
            existingExcludeFolders = excludeFolderList.toList(),
            onDismiss = { showAddExcludeFolderDialog = false },
            onAdd = { path ->
                if (!excludeFolderList.contains(path)) {
                    excludeFolderList.add(path)
                }
                showAddExcludeFolderDialog = false
            },
            onLaunchFolderPicker = onLaunchExcludeFolderPicker
        )
    }
    
    if (showDeleteConfirmDialog && folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmDialog = false
                folderToDelete = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除此目录吗？\n$folderToDelete") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = folderToDelete
                        if (toDelete != null) {
                            folderList.remove(toDelete)
                        }
                        showDeleteConfirmDialog = false
                        folderToDelete = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirmDialog = false
                    folderToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showDeleteExcludeConfirmDialog && excludeFolderToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteExcludeConfirmDialog = false
                excludeFolderToDelete = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除此过滤目录吗？\n$excludeFolderToDelete") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = excludeFolderToDelete
                        if (toDelete != null) {
                            val index = excludeFolderList.indexOfFirst { it == toDelete }
                            if (index >= 0) {
                                excludeFolderList.removeAt(index)
                            }
                        }
                        showDeleteExcludeConfirmDialog = false
                        excludeFolderToDelete = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteExcludeConfirmDialog = false
                    excludeFolderToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SortTypeGrid(
    sortTypes: List<SortType>,
    selectedSortType: SortType,
    onSortTypeSelected: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortTypes.chunked(2).forEach { chunk ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.forEach { type ->
                    val isSelected = selectedSortType == type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onSortTypeSelected(type) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            fontSize = 14.sp,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortOrderGrid(
    sortOrders: List<SortOrder>,
    selectedSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortOrders.forEach { order ->
            val isSelected = selectedSortOrder == order
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSortOrderSelected(order) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = order.displayName,
                    fontSize = 14.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun FilterCheckboxItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (checked) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (checked) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FolderItem(
    path: String,
    showDelete: Boolean = true,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_folder_open_24),
            contentDescription = "目录",
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = path,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (showDelete) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onDelete)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFolderDialog(
    existingFolders: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onLaunchFolderPicker: ((String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputPath by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val suggestedPaths = remember {
        listOf(
            "/sdcard/Music",
            "/sdcard/Download",
            "/sdcard/Movies",
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download"
        ).distinct()
    }
    
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
                text = "添加音频目录",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "选择或输入音频文件目录路径：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = {
                    onLaunchFolderPicker { path ->
                        inputPath = path
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_folder_open_24),
                    contentDescription = "选择文件夹",
                    modifier = Modifier.padding(end = 8.dp).size(18.dp)
                )
                Text("使用系统文件选择器")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (showError) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = inputPath,
                    onValueChange = { 
                        inputPath = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (showError) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (inputPath.isEmpty()) {
                            Text(
                                text = "请输入目录",
                                color = if (showError) 
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f) 
                                else 
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
            if (showError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "常用目录：",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            suggestedPaths.forEach { path ->
                if (!existingFolders.contains(path)) {
                    Text(
                        text = path,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inputPath = path }
                            .padding(vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val trimmedPath = inputPath.trim()
                        if (trimmedPath.isEmpty()) {
                            errorMessage = "路径不能为空"
                            showError = true
                            return@Button
                        }
                        
                        if (existingFolders.contains(trimmedPath)) {
                            errorMessage = "该目录已添加"
                            showError = true
                            return@Button
                        }
                        
                        onAdd(trimmedPath)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("添加")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExcludeFolderDialog(
    existingFolders: List<String>,
    existingExcludeFolders: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onLaunchFolderPicker: ((String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputPath by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val suggestedPaths = remember {
        val subfolders = mutableListOf<String>()
        existingFolders.forEach { folder ->
            val dir = File(folder)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.filter { it.isDirectory }?.take(5)?.forEach { subdir ->
                    subfolders.add(subdir.absolutePath)
                }
            }
        }
        subfolders.distinct().take(5)
    }
    
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
                text = "添加过滤目录",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "选择或输入要过滤的目录路径：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = {
                    onLaunchFolderPicker { path ->
                        inputPath = path
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_folder_open_24),
                    contentDescription = "选择文件夹",
                    modifier = Modifier.padding(end = 8.dp).size(18.dp)
                )
                Text("使用系统文件选择器")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (showError) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = inputPath,
                    onValueChange = { 
                        inputPath = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (showError) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (inputPath.isEmpty()) {
                            Text(
                                text = "请输入目录",
                                color = if (showError) 
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f) 
                                else 
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
            if (showError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (suggestedPaths.isNotEmpty()) {
                Text(
                    text = "建议目录：",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                suggestedPaths.forEach { path ->
                    if (!existingExcludeFolders.contains(path)) {
                        Text(
                            text = path,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputPath = path }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val trimmedPath = inputPath.trim()
                        if (trimmedPath.isEmpty()) {
                            errorMessage = "路径不能为空"
                            showError = true
                            return@Button
                        }
                        
                        if (existingExcludeFolders.contains(trimmedPath)) {
                            errorMessage = "该目录已添加"
                            showError = true
                            return@Button
                        }
                        
                        onAdd(trimmedPath)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("添加")
                }
            }
        }
    }
}
