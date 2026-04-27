package com.example.LyricBox

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.LyricBox.ui.theme.歌词转换Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomMetadataFieldsSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            歌词转换Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
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

private fun Pair<Float, Float>.containsY(y: Float): Boolean = y in first..second

private enum class MetadataFieldSection {
    VISIBLE,
    HIDDEN
}

@Composable
fun CustomMetadataFieldsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("MusicLibrarySettings", Context.MODE_PRIVATE) }

    val customFields = remember { mutableStateListOf<String>() }
    val visibleFieldKeys = remember { mutableStateListOf<String>() }
    val hiddenFieldKeys = remember { mutableStateListOf<String>() }

    var hasInitialized by remember { mutableStateOf(false) }
    var showAddCustomFieldDialog by remember { mutableStateOf(false) }
    var showEditCustomFieldDialog by remember { mutableStateOf(false) }
    var customFieldToEdit by remember { mutableStateOf<String?>(null) }
    var customFieldToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteCustomFieldDialog by remember { mutableStateOf(false) }

    val itemBounds = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
    val itemPlacementOffsets = remember { mutableStateMapOf<String, Float>() }
    var visibleSectionBounds by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var hiddenSectionBounds by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var draggingFieldKey by remember { mutableStateOf<String?>(null) }
    var draggingYInWindow by remember { mutableFloatStateOf(0f) }
    var dragPreviewSection by remember { mutableStateOf<MetadataFieldSection?>(null) }
    var dragPreviewInsertIndex by remember { mutableStateOf(-1) }
    val itemSpacingPx = with(density) { 8.dp.toPx() }

    fun saveSettings() {
        MetadataFieldConfigStore.save(
            prefs = prefs,
            visibleFieldKeys = visibleFieldKeys.toList(),
            hiddenFieldKeys = hiddenFieldKeys.toList(),
            customFieldNames = customFields.toList()
        )
    }

    fun sectionBounds(keys: List<String>, fallback: Pair<Float, Float>?): Pair<Float, Float>? {
        val bounds = keys.mapNotNull { itemBounds[it] }
        if (bounds.isEmpty()) return fallback
        return bounds.minOf { it.first } to bounds.maxOf { it.second }
    }

    fun queuePlacementAnimation(keys: List<String>, offsetY: Float) {
        if (keys.isEmpty() || offsetY == 0f) return
        keys.forEach { key ->
            itemPlacementOffsets[key] = (itemPlacementOffsets[key] ?: 0f) + offsetY
        }
        coroutineScope.launch {
            delay(16)
            keys.forEach { key -> itemPlacementOffsets[key] = 0f }
        }
    }

    fun calcInsertIndex(target: List<String>, dropY: Float, movingKey: String): Int {
        val candidates = target.filter { it != movingKey }
        if (candidates.isEmpty()) return 0
        for ((index, key) in candidates.withIndex()) {
            val bounds = itemBounds[key] ?: continue
            val centerY = (bounds.first + bounds.second) / 2f
            if (dropY < centerY) return index
        }
        return candidates.size
    }

    fun sectionOfField(fieldKey: String): MetadataFieldSection? {
        return when {
            visibleFieldKeys.contains(fieldKey) -> MetadataFieldSection.VISIBLE
            hiddenFieldKeys.contains(fieldKey) -> MetadataFieldSection.HIDDEN
            else -> null
        }
    }

    fun listOfSection(section: MetadataFieldSection): MutableList<String> {
        return when (section) {
            MetadataFieldSection.VISIBLE -> visibleFieldKeys
            MetadataFieldSection.HIDDEN -> hiddenFieldKeys
        }
    }

    fun resolveTargetSection(fieldKey: String, dropY: Float?): MetadataFieldSection {
        val current = when {
            visibleFieldKeys.contains(fieldKey) -> visibleFieldKeys
            hiddenFieldKeys.contains(fieldKey) -> hiddenFieldKeys
            else -> visibleFieldKeys
        }
        val currentSection = if (current === visibleFieldKeys) {
            MetadataFieldSection.VISIBLE
        } else {
            MetadataFieldSection.HIDDEN
        }
        if (dropY == null) return currentSection

        val visibleBounds = sectionBounds(visibleFieldKeys, visibleSectionBounds)
        val hiddenBounds = sectionBounds(hiddenFieldKeys, hiddenSectionBounds)
        return when {
            visibleBounds?.containsY(dropY) == true -> MetadataFieldSection.VISIBLE
            hiddenBounds?.containsY(dropY) == true -> MetadataFieldSection.HIDDEN
            else -> currentSection
        }
    }

    fun moveField(
        fieldKey: String,
        dropY: Float?,
        allowCrossSection: Boolean
    ): Boolean {
        if (dropY == null) return false
        val sourceSection = sectionOfField(fieldKey) ?: return false
        val source = listOfSection(sourceSection)
        val sourceIndex = source.indexOf(fieldKey)
        if (sourceIndex < 0) return false

        val resolvedTargetSection = resolveTargetSection(fieldKey, dropY)
        if (!allowCrossSection && resolvedTargetSection != sourceSection) return false
        val targetSection = if (allowCrossSection) resolvedTargetSection else sourceSection
        val target = listOfSection(targetSection)
        val maxInsertIndex = if (target.contains(fieldKey)) {
            (target.size - 1).coerceAtLeast(0)
        } else {
            target.size
        }
        val insertIndex = calcInsertIndex(target, dropY, fieldKey).coerceIn(0, maxInsertIndex)
        if (source === target && insertIndex == sourceIndex) return false

        val moveUnit = ((itemBounds[fieldKey]?.second ?: 0f) - (itemBounds[fieldKey]?.first ?: 0f))
            .takeIf { it > 0f }
            ?.plus(itemSpacingPx)
            ?: with(density) { 56.dp.toPx() }

        if (sourceSection == targetSection) {
            if (insertIndex > sourceIndex) {
                val affectedKeys = source.subList(sourceIndex + 1, insertIndex + 1).toList()
                queuePlacementAnimation(affectedKeys, moveUnit)
                queuePlacementAnimation(listOf(fieldKey), -moveUnit * (insertIndex - sourceIndex))
            } else if (insertIndex < sourceIndex) {
                val affectedKeys = source.subList(insertIndex, sourceIndex).toList()
                queuePlacementAnimation(affectedKeys, -moveUnit)
                queuePlacementAnimation(listOf(fieldKey), moveUnit * (sourceIndex - insertIndex))
            }
        } else {
            val sourceAffected = if (sourceIndex + 1 < source.size) {
                source.subList(sourceIndex + 1, source.size).toList()
            } else {
                emptyList()
            }
            val targetAffected = target.drop(insertIndex)
            queuePlacementAnimation(sourceAffected, moveUnit)
            queuePlacementAnimation(targetAffected, -moveUnit)
        }

        source.removeAt(sourceIndex)
        target.add(insertIndex.coerceIn(0, target.size), fieldKey)
        saveSettings()
        return true
    }

    fun updateDragPreview(fieldKey: String, dragY: Float) {
        val targetSection = resolveTargetSection(fieldKey, dragY)
        val targetList = listOfSection(targetSection)
        val maxInsertIndex = if (targetList.contains(fieldKey)) {
            (targetList.size - 1).coerceAtLeast(0)
        } else {
            targetList.size
        }
        dragPreviewSection = targetSection
        dragPreviewInsertIndex = calcInsertIndex(targetList, dragY, fieldKey).coerceIn(0, maxInsertIndex)
    }

    if (!hasInitialized) {
        val config = MetadataFieldConfigStore.load(prefs)
        customFields.addAll(config.customFieldNames)
        visibleFieldKeys.addAll(config.visibleFieldKeys)
        hiddenFieldKeys.addAll(config.hiddenFieldKeys)
        hasInitialized = true
    }

    val reservedFieldNames = remember {
        BUILT_IN_METADATA_FIELDS.flatMap { listOf(it.key, it.label) }.toSet()
    }
    val visibleContainerColor by animateColorAsState(
        targetValue = if (draggingFieldKey != null && dragPreviewSection == MetadataFieldSection.VISIBLE) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 160),
        label = "visibleContainerColor"
    )
    val hiddenContainerColor by animateColorAsState(
        targetValue = if (draggingFieldKey != null && dragPreviewSection == MetadataFieldSection.HIDDEN) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        },
        animationSpec = tween(durationMillis = 160),
        label = "hiddenContainerColor"
    )

    Column(modifier = modifier.fillMaxSize()) {
        CommonHeadBar(
            title = "自定义元数据字段",
            showBack = true,
            showMenu = false,
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
            ,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "visible-title") {
                Text(
                    text = "显示字段",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (visibleFieldKeys.isEmpty()) {
                item(key = "visible-empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(visibleContainerColor)
                            .onGloballyPositioned { coordinates ->
                                val top = coordinates.positionInWindow().y
                                visibleSectionBounds = top to (top + coordinates.size.height)
                            }
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "拖动字段到这里以显示",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (draggingFieldKey != null && dragPreviewSection == MetadataFieldSection.VISIBLE) {
                                DragInsertIndicator()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = visibleFieldKeys,
                    key = { _, fieldKey -> fieldKey }
                ) { index, fieldKey ->
                    val label = metadataFieldLabel(fieldKey)
                    val isCustom = isCustomMetadataFieldKey(fieldKey)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (draggingFieldKey != null &&
                            dragPreviewSection == MetadataFieldSection.VISIBLE &&
                            dragPreviewInsertIndex == index
                        ) {
                            DragInsertIndicator()
                        }

                        MetadataDragFieldItem(
                            fieldKey = fieldKey,
                            label = label,
                            isCustom = isCustom,
                            isDragging = draggingFieldKey == fieldKey,
                            onEditCustom = {
                                customFieldToEdit = customMetadataFieldNameFromKey(fieldKey)
                                showEditCustomFieldDialog = true
                            },
                            onDeleteCustom = {
                                customFieldToDelete = customMetadataFieldNameFromKey(fieldKey)
                                showDeleteCustomFieldDialog = true
                            },
                            onBoundsChanged = { bounds -> itemBounds[fieldKey] = bounds },
                            onDragStart = { y ->
                                draggingFieldKey = fieldKey
                                draggingYInWindow = y
                                updateDragPreview(fieldKey, y)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragYChanged = { y ->
                                draggingYInWindow = y
                                val movedInCurrentSection = moveField(fieldKey, y, allowCrossSection = false)
                                val oldPreviewSection = dragPreviewSection
                                val oldPreviewIndex = dragPreviewInsertIndex
                                updateDragPreview(fieldKey, y)
                                val previewChanged = oldPreviewSection != dragPreviewSection || oldPreviewIndex != dragPreviewInsertIndex
                                if (movedInCurrentSection || previewChanged) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                moveField(fieldKey, draggingYInWindow, allowCrossSection = true)
                                draggingFieldKey = null
                                draggingYInWindow = 0f
                                dragPreviewSection = null
                                dragPreviewInsertIndex = -1
                            },
                            onDragCancel = {
                                draggingFieldKey = null
                                draggingYInWindow = 0f
                                dragPreviewSection = null
                                dragPreviewInsertIndex = -1
                            },
                            normalBackgroundColor = if (draggingFieldKey != null && dragPreviewSection == MetadataFieldSection.VISIBLE) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.74f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
                            },
                            placementOffsetY = itemPlacementOffsets[fieldKey] ?: 0f
                        )
                    }
                }

                if (draggingFieldKey != null &&
                    dragPreviewSection == MetadataFieldSection.VISIBLE &&
                    dragPreviewInsertIndex == visibleFieldKeys.size
                ) {
                    item(key = "visible-drop-end") {
                        DragInsertIndicator()
                    }
                }
            }

            item(key = "hidden-title-spacer") {
                Spacer(modifier = Modifier.height(10.dp))
            }

            item(key = "hidden-title") {
                Text(
                    text = "隐藏字段",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (hiddenFieldKeys.isEmpty()) {
                item(key = "hidden-empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(hiddenContainerColor)
                            .onGloballyPositioned { coordinates ->
                                val top = coordinates.positionInWindow().y
                                hiddenSectionBounds = top to (top + coordinates.size.height)
                            }
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "拖动字段到这里以隐藏",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (draggingFieldKey != null && dragPreviewSection == MetadataFieldSection.HIDDEN) {
                                DragInsertIndicator()
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = hiddenFieldKeys,
                    key = { _, fieldKey -> fieldKey }
                ) { index, fieldKey ->
                    val label = metadataFieldLabel(fieldKey)
                    val isCustom = isCustomMetadataFieldKey(fieldKey)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (draggingFieldKey != null &&
                            dragPreviewSection == MetadataFieldSection.HIDDEN &&
                            dragPreviewInsertIndex == index
                        ) {
                            DragInsertIndicator()
                        }

                        MetadataDragFieldItem(
                            fieldKey = fieldKey,
                            label = label,
                            isCustom = isCustom,
                            isDragging = draggingFieldKey == fieldKey,
                            onEditCustom = {
                                customFieldToEdit = customMetadataFieldNameFromKey(fieldKey)
                                showEditCustomFieldDialog = true
                            },
                            onDeleteCustom = {
                                customFieldToDelete = customMetadataFieldNameFromKey(fieldKey)
                                showDeleteCustomFieldDialog = true
                            },
                            onBoundsChanged = { bounds -> itemBounds[fieldKey] = bounds },
                            onDragStart = { y ->
                                draggingFieldKey = fieldKey
                                draggingYInWindow = y
                                updateDragPreview(fieldKey, y)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragYChanged = { y ->
                                draggingYInWindow = y
                                val movedInCurrentSection = moveField(fieldKey, y, allowCrossSection = false)
                                val oldPreviewSection = dragPreviewSection
                                val oldPreviewIndex = dragPreviewInsertIndex
                                updateDragPreview(fieldKey, y)
                                val previewChanged = oldPreviewSection != dragPreviewSection || oldPreviewIndex != dragPreviewInsertIndex
                                if (movedInCurrentSection || previewChanged) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                moveField(fieldKey, draggingYInWindow, allowCrossSection = true)
                                draggingFieldKey = null
                                draggingYInWindow = 0f
                                dragPreviewSection = null
                                dragPreviewInsertIndex = -1
                            },
                            onDragCancel = {
                                draggingFieldKey = null
                                draggingYInWindow = 0f
                                dragPreviewSection = null
                                dragPreviewInsertIndex = -1
                            },
                            normalBackgroundColor = if (draggingFieldKey != null && dragPreviewSection == MetadataFieldSection.HIDDEN) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                            },
                            placementOffsetY = itemPlacementOffsets[fieldKey] ?: 0f
                        )
                    }
                }

                if (draggingFieldKey != null &&
                    dragPreviewSection == MetadataFieldSection.HIDDEN &&
                    dragPreviewInsertIndex == hiddenFieldKeys.size
                ) {
                    item(key = "hidden-drop-end") {
                        DragInsertIndicator()
                    }
                }
            }

            item(key = "add-button-spacer") {
                Spacer(modifier = Modifier.height(14.dp))
            }

            item(key = "add-button") {
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
            }

            item(key = "nav-bar-spacer") {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }

    if (showAddCustomFieldDialog) {
        AddCustomFieldDialog(
            existingCustomFields = customFields.toList(),
            reservedFieldNames = reservedFieldNames,
            onDismiss = { showAddCustomFieldDialog = false },
            onAdd = { fieldName ->
                val key = customMetadataFieldKey(fieldName)
                if (!customFields.contains(fieldName)) {
                    customFields.add(fieldName)
                    visibleFieldKeys.add(key)
                    hiddenFieldKeys.remove(key)
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
            reservedFieldNames = reservedFieldNames,
            onDismiss = {
                showEditCustomFieldDialog = false
                customFieldToEdit = null
            },
            onEdit = { oldName, newName ->
                val index = customFields.indexOf(oldName)
                if (index >= 0) {
                    customFields[index] = newName
                    val oldKey = customMetadataFieldKey(oldName)
                    val newKey = customMetadataFieldKey(newName)
                    val visibleIndex = visibleFieldKeys.indexOf(oldKey)
                    if (visibleIndex >= 0) visibleFieldKeys[visibleIndex] = newKey
                    val hiddenIndex = hiddenFieldKeys.indexOf(oldKey)
                    if (hiddenIndex >= 0) hiddenFieldKeys[hiddenIndex] = newKey
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
                        if (!toDelete.isNullOrEmpty()) {
                            val key = customMetadataFieldKey(toDelete)
                            customFields.remove(toDelete)
                            visibleFieldKeys.remove(key)
                            hiddenFieldKeys.remove(key)
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

@Composable
private fun DragInsertIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    )
}

@Composable
private fun MetadataDragFieldItem(
    fieldKey: String,
    label: String,
    isCustom: Boolean,
    isDragging: Boolean,
    onEditCustom: () -> Unit,
    onDeleteCustom: () -> Unit,
    onBoundsChanged: (Pair<Float, Float>) -> Unit,
    onDragStart: (Float) -> Unit,
    onDragYChanged: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    normalBackgroundColor: Color,
    placementOffsetY: Float = 0f,
    modifier: Modifier = Modifier
) {
    var itemTopInWindow by remember { mutableFloatStateOf(0f) }
    val backgroundColor by animateColorAsState(
        targetValue = if (isDragging) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            normalBackgroundColor
        },
        animationSpec = tween(durationMillis = 140),
        label = "dragItemBackground"
    )
    val animatedPlacementOffsetY by animateFloatAsState(
        targetValue = placementOffsetY,
        animationSpec = tween(durationMillis = 180),
        label = "placementOffsetY"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "dragScale"
    )
    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "dragElevation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
                translationY = animatedPlacementOffsetY
            }
            .shadow(elevation = dragElevation, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .onGloballyPositioned { coordinates ->
                val top = coordinates.positionInWindow().y
                itemTopInWindow = top
                onBoundsChanged(top to (top + coordinates.size.height))
            }
            .pointerInput(fieldKey) {
                var currentDragY = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        currentDragY = itemTopInWindow + offset.y
                        onDragStart(currentDragY)
                    },
                    onDrag = { _, dragAmount ->
                        currentDragY += dragAmount.y
                        onDragYChanged(currentDragY)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "长按拖动",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isCustom) {
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onEditCustom),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pencil),
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onDeleteCustom),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFieldDialog(
    existingCustomFields: List<String>,
    reservedFieldNames: Set<String>,
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
                        if (showError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
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
                        color = if (showError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (inputFieldName.isEmpty()) {
                            Text(
                                text = "请输入字段名称",
                                color = if (showError) {
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                },
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

                        if (existingCustomFields.contains(trimmedFieldName) || reservedFieldNames.contains(trimmedFieldName)) {
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
    reservedFieldNames: Set<String>,
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
                        if (showError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
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
                        color = if (showError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (inputFieldName.isEmpty()) {
                            Text(
                                text = "请输入字段名称",
                                color = if (showError) {
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                },
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

                        if (
                            trimmedFieldName != oldFieldName &&
                            (existingCustomFields.contains(trimmedFieldName) || reservedFieldNames.contains(trimmedFieldName))
                        ) {
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
