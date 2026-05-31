package com.example.LyricBox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    isRunning: Boolean,
    remainingMs: Long,
    onDismiss: () -> Unit,
    onStartTimer: (minutes: Int, finishCurrentSong: Boolean) -> Unit,
    onCancelTimer: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        AnimatedContent(
            targetState = isRunning,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) togetherWith
                    fadeOut(animationSpec = tween(140)))
                    .using(SizeTransform(clip = false))
            },
            label = "sleepTimerSheetMode"
        ) { running ->
            if (running) {
                SleepTimerRunningContent(
                    remainingMs = remainingMs,
                    onCancel = { onCancelTimer() }
                )
            } else {
                SleepTimerSetupContent(
                    onStartQuick = { minutes, finishCurrentSong ->
                        onStartTimer(minutes, finishCurrentSong)
                    },
                    onStartCustom = { minutes, finishCurrentSong ->
                        onStartTimer(minutes, finishCurrentSong)
                    }
                )
            }
        }
    }
}

@Composable
private fun SleepTimerSetupContent(
    onStartQuick: (minutes: Int, finishCurrentSong: Boolean) -> Unit,
    onStartCustom: (minutes: Int, finishCurrentSong: Boolean) -> Unit
) {
    val context = LocalContext.current
    val persisted = remember { SleepTimerStore.readUiPreference(context) }
    val customMinuteOptions = remember { listOf(1) + (5..120 step 5).toList() }
    var customIndex by remember {
        mutableIntStateOf(customMinuteOptions.indexOf(persisted.lastCustomMinutes).coerceAtLeast(0))
    }
    var selectedQuickMinute by remember { mutableStateOf<Int?>(null) }
    var finishCurrentSong by remember { mutableStateOf(persisted.lastFinishCurrentSong) }
    val customMinutes = customMinuteOptions.getOrElse(customIndex) { 30 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "定时播放",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "快捷时间选择",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 30, 45, 60, 90, 120).chunked(3).forEach { rowMinutes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowMinutes.forEach { minutes ->
                        val selected = selectedQuickMinute == minutes
                        BoxButton(
                            text = "${minutes}分钟",
                            selected = selected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedQuickMinute = minutes
                                onStartQuick(minutes, finishCurrentSong)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "自定义时长",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前选择：${customMinutes}分钟",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = customIndex.toFloat(),
            onValueChange = { value ->
                customIndex = value.roundToInt().coerceIn(0, customMinuteOptions.lastIndex)
            },
            valueRange = 0f..customMinuteOptions.lastIndex.toFloat(),
            steps = (customMinuteOptions.size - 2).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        )

        FinishCurrentSongOption(
            checked = finishCurrentSong,
            onCheckedChange = { finishCurrentSong = it },
            label = "播放完当前歌曲后结束",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { onStartCustom(customMinutes, finishCurrentSong) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "开始计时：${customMinutes}分钟",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun SleepTimerRunningContent(
    remainingMs: Long,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "定时播放中",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = formatSleepTimerRemaining(remainingMs),
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(18.dp))
        TextButton(onClick = onCancel) {
            Text(
                text = "取消定时播放",
                color = MaterialTheme.colorScheme.error,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun FinishCurrentSongOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (checked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            )
            .then(
                if (checked) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun BoxButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatSleepTimerRemaining(remainingMs: Long): String {
    val safeSeconds = (remainingMs.coerceAtLeast(0L) / 1000L)
    val minutes = safeSeconds / 60L
    val seconds = safeSeconds % 60L
    return String.format("%02d:%02d", minutes, seconds)
}
