package com.example.LyricBox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

private const val FLYME_LYRIC_LOG_TAG = "FlymeStatusBarLyric"

class FlymeStatusBarLyricBridge(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private var started = false
    private var hideNotification = true
    private var currentSongId: String? = null
    private var currentLines: List<NewPreviewLyricLine> = emptyList()
    private var lastPushedLyricText: String? = null

    private var alwaysShowTickerFlag: Int = 0
    private var onlyUpdateTickerFlag: Int = 0

    fun start() {
        if (started) return
        resolveFlymeTickerFlags()
        createNotificationChannelIfNeeded()
        started = true
        if (hideNotification) {
            cancelStandaloneTickerNotifications()
        }
    }

    fun stop(clearRemoteState: Boolean) {
        if (clearRemoteState) {
            clearLyric()
        }
        started = false
        currentSongId = null
        currentLines = emptyList()
        lastPushedLyricText = null
    }

    fun release() {
        stop(clearRemoteState = true)
    }

    fun isActive(): Boolean {
        return started
    }

    fun setHideNotification(enabled: Boolean) {
        hideNotification = enabled
        if (!started) return
        if (enabled) {
            cancelStandaloneTickerNotifications()
        } else {
            PlaybackTickerState.clear()
        }
        lastPushedLyricText = null
    }

    fun updateSong(songId: String?, lyricLines: List<NewPreviewLyricLine>) {
        currentSongId = songId
        currentLines = lyricLines
        lastPushedLyricText = null
    }

    fun updatePlayback(positionMs: Long, isSeek: Boolean) {
        if (!started) return
        if (currentSongId.isNullOrBlank() || currentLines.isEmpty()) return
        val lyricText = resolveLyricText(positionMs.coerceAtLeast(0L)) ?: return
        if (!isSeek && lyricText == lastPushedLyricText) return
        sendLyric(lyricText)
        lastPushedLyricText = lyricText
    }

    private fun resolveLyricText(positionMs: Long): String? {
        val matched = currentLines.lastOrNull { line ->
            !line.isInterlude && line.begin <= positionMs
        } ?: return null
        val rawText = matched.words.joinToString(separator = "") { it.text }.ifBlank { matched.translation }
        return rawText.trim().takeIf { it.isNotEmpty() }
    }

    private fun showLyricTicker(text: String) {
        if (text.isEmpty()) return
        val flymeTickerSupported = isFlymeTickerSupported()
        val manager = notificationManager ?: return
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.lyricon_status_icon)
            .setTicker(if (flymeTickerSupported) text else null)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
            .apply {
                flags = flags or Notification.FLAG_NO_CLEAR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && flymeTickerSupported) {
                    extras.putBoolean("ticker_icon_switch", false)
                    extras.putInt("ticker_icon", R.drawable.lyricon_status_icon)
                    extras.putString("ticker_text", text)
                    extras.putString("lyric", text)
                }
                if (flymeTickerSupported) {
                    flags = flags or alwaysShowTickerFlag
                    flags = flags or onlyUpdateTickerFlag
                }
            }
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun clearLyric() {
        runCatching {
            val clearIntent = Intent(ACTION_CLEAR_LYRIC).apply {
                putExtra("ticker_package", appContext.packageName)
                putExtra("package", appContext.packageName)
            }
            sendFlymeBroadcast(clearIntent)
        }
        PlaybackTickerState.clear()
        cancelStandaloneTickerNotifications()
    }

    private fun sendLyric(text: String) {
        if (text.isBlank()) {
            clearLyric()
            return
        }
        runCatching {
            val sendIntent = Intent(ACTION_SEND_LYRIC).apply {
                putExtra("ticker_text", text)
                putExtra("lyric", text)
                putExtra("text", text)
                putExtra("content", text)
                putExtra("ticker_package", appContext.packageName)
                putExtra("package", appContext.packageName)
                putExtra("ticker_app_name", "LunaBeat")
                putExtra("app_name", "LunaBeat")
            }
            sendFlymeBroadcast(sendIntent)
        }
        if (hideNotification) {
            PlaybackTickerState.update(text, null)
            cancelStandaloneTickerNotifications()
        } else {
            PlaybackTickerState.clear()
            showLyricTicker(text)
        }
    }

    private fun cancelStandaloneTickerNotifications() {
        val manager = notificationManager ?: return
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(null, NOTIFICATION_ID)
        manager.cancel(LEGACY_HIDDEN_NOTIFICATION_ID)
        runCatching {
            createNotificationChannelIfNeeded()
            val disposable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(appContext, CHANNEL_ID)
            } else {
                Notification.Builder(appContext)
            }
                .setSmallIcon(R.drawable.lyricon_status_icon)
                .setContentTitle("")
                .setContentText("")
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MIN)
                .build()
            manager.notify(NOTIFICATION_ID, disposable)
            manager.cancel(NOTIFICATION_ID)
            manager.cancel(null, NOTIFICATION_ID)
        }
    }

    private fun sendFlymeBroadcast(intent: Intent) {
        appContext.sendBroadcast(intent)
        appContext.sendBroadcast(Intent(intent).setPackage(SYSTEM_UI_PACKAGE))
    }

    private fun resolveFlymeTickerFlags() {
        if (alwaysShowTickerFlag != 0 && onlyUpdateTickerFlag != 0) return
        try {
            val notificationClass = Notification::class.java
            alwaysShowTickerFlag = getFieldStepwise(
                notificationClass,
                null,
                "FLAG_ALWAYS_SHOW_TICKER"
            ) as? Int ?: 0
            onlyUpdateTickerFlag = getFieldStepwise(
                notificationClass,
                null,
                "FLAG_ONLY_UPDATE_TICKER"
            ) as? Int ?: 0
        } catch (e: Exception) {
            alwaysShowTickerFlag = FLAG_ALWAYS_SHOW_TICKER_FALLBACK
            onlyUpdateTickerFlag = FLAG_ONLY_UPDATE_TICKER_FALLBACK
        }
        if (alwaysShowTickerFlag <= 0) {
            alwaysShowTickerFlag = FLAG_ALWAYS_SHOW_TICKER_FALLBACK
        }
        if (onlyUpdateTickerFlag <= 0) {
            onlyUpdateTickerFlag = FLAG_ONLY_UPDATE_TICKER_FALLBACK
        }
    }

    private fun isFlymeTickerSupported(): Boolean {
        return alwaysShowTickerFlag > 0 && onlyUpdateTickerFlag > 0
    }

    @Throws(NoSuchFieldException::class)
    private fun getFieldStepwise(desClass: Class<*>, desObj: Any?, fieldName: String): Any {
        var targetClass: Class<*>? = desClass
        while (targetClass != null) {
            try {
                val field = targetClass.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(desObj) ?: throw NoSuchFieldException(fieldName)
            } catch (_: NoSuchFieldException) {
                targetClass = targetClass.superclass
            }
        }
        throw NoSuchFieldException(fieldName)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = notificationManager ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Flyme Status Bar Lyric",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "flyme_status_bar_lyric"
        private const val NOTIFICATION_ID = 77831
        private const val LEGACY_HIDDEN_NOTIFICATION_ID = 1001
        private const val FLAG_ALWAYS_SHOW_TICKER_FALLBACK = 0x1000000
        private const val FLAG_ONLY_UPDATE_TICKER_FALLBACK = 0x2000000
        private const val ACTION_SEND_LYRIC = "com.meizu.flyme.ticker.ACTION_SEND"
        private const val ACTION_CLEAR_LYRIC = "com.meizu.flyme.ticker.ACTION_CLEAR"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}

internal object PlaybackTickerState {
    data class Payload(
        val text: String,
        val translation: String?
    )

    @Volatile
    private var payload: Payload? = null
    private var refreshNotification: (() -> Unit)? = null

    @Synchronized
    fun setRefreshCallback(callback: (() -> Unit)?) {
        refreshNotification = callback
    }

    fun current(): Payload? = payload

    fun update(text: String?, translation: String?) {
        payload = text
            ?.takeIf { it.isNotBlank() }
            ?.let { Payload(it, translation?.takeIf { value -> value.isNotBlank() }) }
        refreshNotification?.invoke()
    }

    fun clear() {
        if (payload == null) return
        payload = null
        refreshNotification?.invoke()
    }
}
