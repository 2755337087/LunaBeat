package com.example.LyricBox

import android.content.Context
import android.os.SystemClock

private const val SLEEP_TIMER_PREFS = "sleep_timer_state"
private const val KEY_SLEEP_TIMER_ACTIVE = "sleep_timer_active"
private const val KEY_SLEEP_TIMER_END_ELAPSED_MS = "sleep_timer_end_elapsed_ms"
private const val KEY_SLEEP_TIMER_FINISH_CURRENT = "sleep_timer_finish_current_song"
private const val KEY_SLEEP_TIMER_WAITING_SONG_END = "sleep_timer_waiting_song_end"
private const val KEY_SLEEP_TIMER_ANCHOR_PATH = "sleep_timer_anchor_audio_path"
private const val KEY_SLEEP_TIMER_LAST_CUSTOM_MINUTES = "sleep_timer_last_custom_minutes"
private const val KEY_SLEEP_TIMER_LAST_FINISH_CURRENT = "sleep_timer_last_finish_current"

data class SleepTimerSnapshot(
    val isActive: Boolean = false,
    val endElapsedRealtimeMs: Long = 0L,
    val finishCurrentSong: Boolean = false,
    val waitingForSongEnd: Boolean = false,
    val anchorAudioPath: String? = null
) {
    fun remainingMs(nowElapsedRealtimeMs: Long = SystemClock.elapsedRealtime()): Long {
        if (!isActive || waitingForSongEnd) return 0L
        return (endElapsedRealtimeMs - nowElapsedRealtimeMs).coerceAtLeast(0L)
    }
}

data class SleepTimerUiPreference(
    val lastCustomMinutes: Int = 30,
    val lastFinishCurrentSong: Boolean = true
)

object SleepTimerStore {

    fun read(context: Context): SleepTimerSnapshot {
        val prefs = context.getSharedPreferences(SLEEP_TIMER_PREFS, Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean(KEY_SLEEP_TIMER_ACTIVE, false)
        if (!isActive) return SleepTimerSnapshot()
        return SleepTimerSnapshot(
            isActive = true,
            endElapsedRealtimeMs = prefs.getLong(KEY_SLEEP_TIMER_END_ELAPSED_MS, 0L),
            finishCurrentSong = prefs.getBoolean(KEY_SLEEP_TIMER_FINISH_CURRENT, false),
            waitingForSongEnd = prefs.getBoolean(KEY_SLEEP_TIMER_WAITING_SONG_END, false),
            anchorAudioPath = prefs.getString(KEY_SLEEP_TIMER_ANCHOR_PATH, null)
        )
    }

    fun start(context: Context, durationMs: Long, finishCurrentSong: Boolean, anchorPath: String?) {
        val safeDurationMs = durationMs.coerceAtLeast(60_000L)
        val endElapsedRealtimeMs = SystemClock.elapsedRealtime() + safeDurationMs
        write(
            context,
            SleepTimerSnapshot(
                isActive = true,
                endElapsedRealtimeMs = endElapsedRealtimeMs,
                finishCurrentSong = finishCurrentSong,
                waitingForSongEnd = false,
                anchorAudioPath = anchorPath
            )
        )
        saveLastFinishCurrentSong(context, finishCurrentSong)
    }

    fun markWaitingForSongEnd(context: Context, anchorPath: String?) {
        val current = read(context)
        if (!current.isActive) return
        write(
            context,
            current.copy(
                waitingForSongEnd = true,
                anchorAudioPath = anchorPath
            )
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(SLEEP_TIMER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun readUiPreference(context: Context): SleepTimerUiPreference {
        val prefs = context.getSharedPreferences(SLEEP_TIMER_PREFS, Context.MODE_PRIVATE)
        val lastCustom = prefs.getInt(KEY_SLEEP_TIMER_LAST_CUSTOM_MINUTES, 30).coerceIn(1, 120)
        val normalizedCustom = if (lastCustom == 1) {
            1
        } else {
            (lastCustom / 5) * 5
        }.coerceIn(1, 120)
        return SleepTimerUiPreference(
            lastCustomMinutes = normalizedCustom,
            lastFinishCurrentSong = prefs.getBoolean(KEY_SLEEP_TIMER_LAST_FINISH_CURRENT, true)
        )
    }

    fun saveLastCustomMinutes(context: Context, minutes: Int) {
        val normalized = if (minutes <= 1) {
            1
        } else {
            (minutes / 5) * 5
        }.coerceIn(1, 120)
        context.getSharedPreferences(SLEEP_TIMER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SLEEP_TIMER_LAST_CUSTOM_MINUTES, normalized)
            .apply()
    }

    fun saveLastFinishCurrentSong(context: Context, checked: Boolean) {
        context.getSharedPreferences(SLEEP_TIMER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SLEEP_TIMER_LAST_FINISH_CURRENT, checked)
            .apply()
    }

    private fun write(context: Context, snapshot: SleepTimerSnapshot) {
        val prefs = context.getSharedPreferences(SLEEP_TIMER_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SLEEP_TIMER_ACTIVE, snapshot.isActive)
            .putLong(KEY_SLEEP_TIMER_END_ELAPSED_MS, snapshot.endElapsedRealtimeMs)
            .putBoolean(KEY_SLEEP_TIMER_FINISH_CURRENT, snapshot.finishCurrentSong)
            .putBoolean(KEY_SLEEP_TIMER_WAITING_SONG_END, snapshot.waitingForSongEnd)
            .putString(KEY_SLEEP_TIMER_ANCHOR_PATH, snapshot.anchorAudioPath)
            .apply()
    }
}
