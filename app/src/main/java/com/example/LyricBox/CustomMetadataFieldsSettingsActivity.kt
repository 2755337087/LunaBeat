package com.example.LyricBox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LyricBox.ui.theme.歌词转换Theme

class CustomMetadataFieldsSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime)
                ) { paddingValues ->
                    CustomMetadataFieldsSettingsScreen(
                        onBack = { finish() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomMetadataFieldsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }
    
    var showAddCustomFieldDialog by remember { mutableStateOf(false) }
    var showEditCustomFieldDialog by remember { mutableStateOf(false) }
    var customFieldToEdit by remember { mutableStateOf<String?>(null) }
    
    // 预设字段和自定义字段 - 移除Comment，只保留Language和Rate
    val presetFields = remember { listOf("Language", "Rate") }
    val enabledFields = remember { mutableStateListOf<String>() }
    val customFields = remember { mutableStateListOf<String>() }
    
    var customFieldToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteCustomFieldDialog by remember { mutableStateOf(false) }
    
    // 实时保存函数
    fun saveSettings() {
        prefs.edit()
            .putStringSet("enabledMetadataFields", enabledFields.toSet())
            .putStringSet("customMetadataFields", customFields.toSet())
            .apply()
    }
    
    var hasInitialized by remember { mutableStateOf(false) }
    if (!hasInitialized) {
        val savedEnabledFields = prefs.getStringSet("enabledMetadataFields", emptySet()) ?: emptySet()
        if (enabledFields.isEmpty() && savedEnabledFields.isNotEmpty()) {
            enabledFields.addAll(savedEnabledFields)
        }
        
        val savedCustomFields = prefs.getStringSet("customMetadataFields", emptySet()) ?: emptySet()
        if (customFields.isEmpty() && savedCustomFields.isNotEmpty()) {
            customFields.addAll(savedCustomFields)
        }
        hasInitialized = true
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "自定义元数据字段",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "选择要在编辑页面显示的元数据字段",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 预设字段
            Text(
                text = "预设字段",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            presetFields.forEach { field ->
                val isEnabled = enabledFields.contains(field)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isEnabled) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { 
                            if (isEnabled) {
                                enabledFields.remove(field)
                            } else {
                                enabledFields.add(field)
                            }
                            saveSettings()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = field,
                        fontSize = 14.sp,
                        color = if (isEnabled) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 自定义字段
            if (customFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "自定义字段",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                customFields.forEach { field ->
                    val isEnabled = enabledFields.contains(field)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isEnabled) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    if (isEnabled) {
                                        enabledFields.remove(field)
                                    } else {
                                        enabledFields.add(field)
                                    }
                                    saveSettings()
                                }
                        ) {
                            Text(
                                text = field,
                                fontSize = 14.sp,
                                color = if (isEnabled) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // 编辑按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    customFieldToEdit = field
                                    showEditCustomFieldDialog = true
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.pencil),
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // 删除按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    customFieldToDelete = field
                                    showDeleteCustomFieldDialog = true
                                }
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
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            OutlinedButton(
                onClick = { showAddCustomFieldDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "添加字段",
                    modifier = Modifier.padding(end = 8.dp).size(16.dp)
                )
                Text("添加自定义字段")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showAddCustomFieldDialog) {
        AddCustomFieldDialog(
            existingCustomFields = customFields.toList(),
            presetFields = presetFields,
            onDismiss = { showAddCustomFieldDialog = false },
            onAdd = { fieldName ->
                if (!customFields.contains(fieldName)) {
                    customFields.add(fieldName)
                    enabledFields.add(fieldName) // 新增后默认选中
                    saveSettings()
                }
                showAddCustomFieldDialog = false
            }
        )
    }
    
    if (showEditCustomFieldDialog && customFieldToEdit != null) {
        EditCustomFieldDialog(
            oldFieldName = customFieldToEdit!!,
            existingCustomFields = customFields.toList(),
            presetFields = presetFields,
            onDismiss = { 
                showEditCustomFieldDialog = false 
                customFieldToEdit = null
            },
            onEdit = { oldName, newName ->
                val index = customFields.indexOf(oldName)
                if (index >= 0) {
                    customFields[index] = newName
                    // 如果旧字段是启用的，新字段也启用
                    if (enabledFields.contains(oldName)) {
                        enabledFields.remove(oldName)
                        enabledFields.add(newName)
                    }
                    saveSettings()
                }
                showEditCustomFieldDialog = false
                customFieldToEdit = null
            }
        )
    }
    
    if (showDeleteCustomFieldDialog && customFieldToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteCustomFieldDialog = false
                customFieldToDelete = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除此自定义字段吗？\n$customFieldToDelete") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = customFieldToDelete
                        if (toDelete != null) {
                            customFields.remove(toDelete)
                            enabledFields.remove(toDelete)
                            saveSettings()
                        }
                        showDeleteCustomFieldDialog = false
                        customFieldToDelete = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteCustomFieldDialog = false
                    customFieldToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFieldDialog(
    existingCustomFields: List<String>,
    presetFields: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputFieldName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
        ) {
            Text(
                text = "添加自定义字段",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "输入字段名称：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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
                    value = inputFieldName,
                    onValueChange = { 
                        inputFieldName = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 1,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (showError) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (inputFieldName.isEmpty()) {
                            Text(
                                text = "请输入字段名称",
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
                        val trimmedFieldName = inputFieldName.trim()
                        if (trimmedFieldName.isEmpty()) {
                            errorMessage = "字段名称不能为空"
                            showError = true
                            return@Button
                        }
                        
                        if (existingCustomFields.contains(trimmedFieldName) || 
                            presetFields.contains(trimmedFieldName)) {
                            errorMessage = "该字段已存在"
                            showError = true
                            return@Button
                        }
                        
                        onAdd(trimmedFieldName)
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
fun EditCustomFieldDialog(
    oldFieldName: String,
    existingCustomFields: List<String>,
    presetFields: List<String>,
    onDismiss: () -> Unit,
    onEdit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputFieldName by remember { mutableStateOf(oldFieldName) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
        ) {
            Text(
                text = "编辑自定义字段",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "输入新的字段名称：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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
                    value = inputFieldName,
                    onValueChange = { 
                        inputFieldName = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 1,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (showError) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (inputFieldName.isEmpty()) {
                            Text(
                                text = "请输入字段名称",
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
                        val trimmedFieldName = inputFieldName.trim()
                        if (trimmedFieldName.isEmpty()) {
                            errorMessage = "字段名称不能为空"
                            showError = true
                            return@Button
                        }
                        
                        if (trimmedFieldName != oldFieldName && 
                            (existingCustomFields.contains(trimmedFieldName) || 
                             presetFields.contains(trimmedFieldName))) {
                            errorMessage = "该字段已存在"
                            showError = true
                            return@Button
                        }
                        
                        onEdit(oldFieldName, trimmedFieldName)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }
        }
    }
}
