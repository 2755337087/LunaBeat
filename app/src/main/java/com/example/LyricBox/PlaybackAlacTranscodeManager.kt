package com.example.LyricBox

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileInputStream

object PlaybackAlacTranscodeManager {

    private const val TAG = "PlaybackAlacTranscode"
    private const val CACHE_DIR_NAME = "playback_audio_cache"
    private const val CACHE_PREFIX = "playback_transcoded_"

    fun isAlacEncodedM4a(path: String): Boolean {
        val file = File(path)
        if (!file.exists() || !file.name.lowercase().endsWith(".m4a")) {
            return false
        }

        val extractorDetected = runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(path)
                for (index in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(index)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.contains("alac", ignoreCase = true)) {
                        return@runCatching true
                    }
                }
                false
            } finally {
                extractor.release()
            }
        }.getOrDefault(false)
        if (extractorDetected) return true

        return try {
            FileInputStream(file).use { input ->
                val maxProbeBytes = minOf(1024 * 1024, file.length().toInt().coerceAtLeast(0))
                if (maxProbeBytes <= 0) return false
                val buffer = ByteArray(maxProbeBytes)
                val readCount = input.read(buffer)
                if (readCount <= 0) return false
                String(buffer, 0, readCount, Charsets.ISO_8859_1).contains("alac", ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ALAC probe failed for: $path", e)
            false
        }
    }

    fun ensureTranscodedPath(
        context: Context,
        sourcePath: String,
        forceForM4aFailure: Boolean = false
    ): String? {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return null
        val shouldLegacyTranscode = if (forceForM4aFailure) {
            sourceFile.name.lowercase().endsWith(".m4a")
        } else {
            isAlacEncodedM4a(sourcePath)
        }
        if (shouldLegacyTranscode) {
            Log.i(TAG, "Legacy transcode disabled, use direct decode path: $sourcePath")
        }
        cleanupCacheFiles(context, keepPath = null)
        return sourcePath
    }

    fun cleanupCacheFiles(context: Context, keepPath: String? = null) {
        val dir = getCacheDir(context)
        dir.listFiles()?.forEach { file ->
            if (keepPath != null && file.absolutePath == keepPath) return@forEach
            if (file.name.startsWith(CACHE_PREFIX) || file.name.startsWith("temp_")) {
                if (!file.delete()) {
                    Log.w(TAG, "Failed to delete playback cache file: ${file.absolutePath}")
                }
            }
        }
    }

    fun isPlaybackCachePath(context: Context, path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val cacheDirPath = getCacheDir(context).absolutePath
        return path.startsWith(cacheDirPath) && File(path).name.startsWith(CACHE_PREFIX)
    }

    fun releaseStreamingResources() {
        // No-op. Kept as a stable hook for playback-service cleanup.
    }

    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
