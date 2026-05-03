package com.example.LyricBox

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.example.LyricBox.utils.AudioConverter
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object PlaybackAlacTranscodeManager {

    private const val TAG = "PlaybackAlacTranscode"
    private const val CACHE_DIR_NAME = "playback_audio_cache"
    private const val CACHE_PREFIX = "playback_transcoded_"

    private val lock = Any()
    private val cachedOutputBySource = mutableMapOf<String, String>()

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
        val shouldTranscode = if (forceForM4aFailure) {
            sourceFile.name.lowercase().endsWith(".m4a")
        } else {
            isAlacEncodedM4a(sourcePath)
        }
        if (!shouldTranscode) return sourcePath

        synchronized(lock) {
            val cached = cachedOutputBySource[sourcePath]
            if (cached != null && File(cached).exists()) {
                cleanupCacheFiles(context, keepPath = cached)
                return cached
            }
            cachedOutputBySource.remove(sourcePath)
        }

        val outputPath = synchronized(lock) {
            val cacheDir = getCacheDir(context)
            cleanupCacheFiles(context, keepPath = null)
            val output = File(
                cacheDir,
                "${CACHE_PREFIX}${System.currentTimeMillis()}_${sourceFile.nameWithoutExtension}.wav"
            )
            output.absolutePath
        }

        val outputFile = File(outputPath)
        val done = CountDownLatch(1)
        val callbackHandled = AtomicBoolean(false)
        val successFlag = AtomicBoolean(false)

        AudioConverter.decodeToWav(
            inputPath = sourcePath,
            outputPath = outputPath,
            callback = object : AudioConverter.ConvertCallback {
                override fun onProgress(progress: Int, time: Long) = Unit

                override fun onComplete(successState: Boolean, message: String) {
                    successFlag.set(successState)
                    if (callbackHandled.compareAndSet(false, true)) {
                        done.countDown()
                    }
                    if (!successState) {
                        Log.e(TAG, "Playback fallback transcode failed: $message")
                    }
                }

                override fun onError(error: String) {
                    successFlag.set(false)
                    if (callbackHandled.compareAndSet(false, true)) {
                        done.countDown()
                    }
                    Log.e(TAG, "Playback fallback transcode error: $error")
                }
            }
        )

        val finished = runCatching { done.await(8, TimeUnit.MINUTES) }.getOrDefault(false)
        if (!finished || !successFlag.get() || !outputFile.exists()) {
            if (outputFile.exists()) {
                outputFile.delete()
            }
            synchronized(lock) {
                cachedOutputBySource.remove(sourcePath)
                cleanupCacheFiles(context, keepPath = null)
            }
            return null
        }

        synchronized(lock) {
            cachedOutputBySource.clear()
            cachedOutputBySource[sourcePath] = outputFile.absolutePath
            cleanupCacheFiles(context, keepPath = outputFile.absolutePath)
        }
        return outputFile.absolutePath
    }

    fun cleanupCacheFiles(context: Context, keepPath: String? = null) {
        synchronized(lock) {
            val dir = getCacheDir(context)
            dir.listFiles()?.forEach { file ->
                if (keepPath != null && file.absolutePath == keepPath) return@forEach
                if (file.name.startsWith(CACHE_PREFIX) || file.name.startsWith("temp_")) {
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to delete playback cache file: ${file.absolutePath}")
                    }
                }
            }
            cachedOutputBySource.entries.removeAll { (_, outputPath) ->
                keepPath != outputPath && !File(outputPath).exists()
            }
        }
    }

    fun isPlaybackCachePath(context: Context, path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val cacheDirPath = getCacheDir(context).absolutePath
        return path.startsWith(cacheDirPath) && File(path).name.startsWith(CACHE_PREFIX)
    }

    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
