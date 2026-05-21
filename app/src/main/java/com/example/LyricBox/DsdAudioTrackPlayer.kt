package com.example.LyricBox

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

data class DsdPlaybackState(
    val path: String?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val hasEnded: Boolean = false,
    val errorMessage: String? = null
)

class DsdAudioTrackPlayer(
    private val onStateChanged: (DsdPlaybackState) -> Unit
) {
    private data class DsfInfo(
        val path: String,
        val channelCount: Int,
        val sampleRate: Int,
        val sampleCount: Long,
        val blockSizePerChannel: Int,
        val audioDataStart: Long,
        val audioDataEnd: Long
    ) {
        val durationMs: Long
            get() = if (sampleRate > 0) (sampleCount * 1000L) / sampleRate else 0L
    }

    private val byteBalance = IntArray(256) { value ->
        var ones = 0
        for (bit in 0 until 8) {
            ones += (value ushr bit) and 1
        }
        ones * 2 - 8
    }

    private val lock = Object()

    @Volatile
    private var stopRequested = false
    @Volatile
    private var paused = false
    @Volatile
    private var seekToMs: Long? = null
    @Volatile
    private var currentInfo: DsfInfo? = null
    @Volatile
    private var currentPath: String? = null
    @Volatile
    private var currentDurationMs: Long = 0L
    @Volatile
    private var renderedOutputSamples: Long = 0L
    @Volatile
    private var outputSampleRate: Int = 176_400
    private val playbackHeadLock = Object()
    private var playbackHeadBaseSamples: Long = 0L
    private var playbackHeadWrapSamples: Long = 0L
    private var lastPlaybackHeadRaw: Long = 0L

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    val isActive: Boolean
        get() = currentPath != null

    val isPlaying: Boolean
        get() = isActive && !paused && !stopRequested

    fun play(path: String, startPositionMs: Long = 0L, startPaused: Boolean = false) {
        stop()
        val info = try {
            parseDsf(path)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse DSF: $path", e)
            onStateChanged(DsdPlaybackState(path, false, 0L, 0L, errorMessage = e.message))
            return
        }

        stopRequested = false
        paused = startPaused
        currentInfo = info
        currentPath = path
        currentDurationMs = info.durationMs
        renderedOutputSamples = 0L
        outputSampleRate = chooseOutputSampleRate(info.sampleRate)
        seekToMs = startPositionMs.takeIf { it > 0L }

        onStateChanged(DsdPlaybackState(path, !startPaused, startPositionMs.coerceAtLeast(0L), info.durationMs))
        playbackThread = thread(start = true, name = "DsdAudioTrackPlayer") {
            runPlayback(info)
        }
    }

    fun pause() {
        if (!isActive) return
        paused = true
        audioTrack?.pause()
        publishState(isPlaying = false)
    }

    fun resume() {
        if (!isActive) return
        paused = false
        synchronized(lock) {
            lock.notifyAll()
        }
        audioTrack?.play()
        publishState(isPlaying = true)
    }

    fun seekTo(positionMs: Long) {
        if (!isActive) return
        seekToMs = positionMs.coerceAtLeast(0L)
    }

    fun stop() {
        val threadToJoin = playbackThread
        stopRequested = true
        paused = false
        synchronized(lock) {
            lock.notifyAll()
        }
        playbackThread = null
        if (threadToJoin != null && threadToJoin != Thread.currentThread()) {
            runCatching { threadToJoin.join(500L) }
        }
        releaseAudioTrack()
        currentInfo = null
        currentPath = null
        currentDurationMs = 0L
        renderedOutputSamples = 0L
        resetPlaybackHeadPosition(0L)
        seekToMs = null
    }

    fun currentPositionMs(): Long {
        val rate = outputSampleRate.takeIf { it > 0 } ?: return 0L
        val playedSamples = audioTrack?.let { readPlaybackHeadPositionSamples(it) } ?: renderedOutputSamples
        return ((playedSamples * 1000L) / rate).coerceAtLeast(0L)
    }

    fun probeDurationMs(path: String): Long {
        return parseDsf(path).durationMs
    }

    private fun runPlayback(info: DsfInfo) {
        var raf: RandomAccessFile? = null
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE)
            val outRate = chooseSupportedOutputSampleRate(info.sampleRate, info.channelCount)
            outputSampleRate = outRate
            val decimationRatio = info.sampleRate / outRate
            val bytesPerOutputSample = decimationRatio / 8
            if (decimationRatio <= 0 || decimationRatio % 8 != 0) {
                throw IllegalArgumentException("Unsupported DSF sample rate: ${info.sampleRate}")
            }

            val channelCount = info.channelCount.coerceIn(1, 2)
            val audioTrack = createAudioTrack(outRate, channelCount)
            this.audioTrack = audioTrack
            raf = RandomAccessFile(File(info.path), "r")
            resetPlaybackHeadPosition(0L)
            if (!paused) {
                audioTrack.play()
            }

            val channelBlocks = Array(channelCount) { ByteArray(info.blockSizePerChannel) }
            val outputFramesPerBlock = info.blockSizePerChannel / bytesPerOutputSample
            val pcm = ShortArray(outputFramesPerBlock * channelCount)
            var dataOffset = info.audioDataStart
            var nextPublishAt = System.currentTimeMillis()

            while (!stopRequested && dataOffset < info.audioDataEnd) {
                val requestedSeek = seekToMs
                if (requestedSeek != null) {
                    val seek = resolveSeekOffset(info, requestedSeek)
                    dataOffset = seek.first
                    renderedOutputSamples = seek.second
                    seekToMs = null
                    audioTrack.pause()
                    audioTrack.flush()
                    resetPlaybackHeadPosition(seek.second)
                    if (!paused) audioTrack.play()
                }

                waitWhilePaused()
                if (stopRequested) break

                val bytesNeeded = info.blockSizePerChannel.toLong() * channelCount
                if (dataOffset + bytesNeeded > info.audioDataEnd) break
                raf.seek(dataOffset)
                for (channel in 0 until channelCount) {
                    raf.readFully(channelBlocks[channel])
                }

                val samples = convertBlock(
                    channelBlocks = channelBlocks,
                    bytesPerOutputSample = bytesPerOutputSample,
                    channelCount = channelCount,
                    out = pcm
                )
                var written = 0
                while (written < samples && !stopRequested) {
                    waitWhilePaused()
                    val count = audioTrack.write(pcm, written, samples - written)
                    if (count < 0) throw IllegalStateException("AudioTrack write failed: $count")
                    written += count
                }

                renderedOutputSamples += outputFramesPerBlock
                dataOffset += bytesNeeded
                val now = System.currentTimeMillis()
                if (now >= nextPublishAt) {
                    publishState(isPlaying = !paused)
                    nextPublishAt = now + 500L
                }
            }

            val ended = !stopRequested && dataOffset >= info.audioDataEnd
            if (ended) {
                renderedOutputSamples = (info.durationMs * outRate) / 1000L
                onStateChanged(
                    DsdPlaybackState(
                        path = info.path,
                        isPlaying = false,
                        positionMs = info.durationMs,
                        durationMs = info.durationMs,
                        hasEnded = true
                    )
                )
            }
        } catch (e: Exception) {
            if (!stopRequested) {
                Log.e(TAG, "DSF streaming playback failed: ${info.path}", e)
                onStateChanged(
                    DsdPlaybackState(
                        path = info.path,
                        isPlaying = false,
                        positionMs = currentPositionMs(),
                        durationMs = info.durationMs,
                        errorMessage = e.message
                    )
                )
            }
        } finally {
            raf?.close()
            releaseAudioTrack()
            if (!stopRequested) {
                currentInfo = null
                currentPath = null
            }
        }
    }

    private fun waitWhilePaused() {
        while (paused && !stopRequested) {
            synchronized(lock) {
                lock.wait(40L)
            }
        }
    }

    private fun convertBlock(
        channelBlocks: Array<ByteArray>,
        bytesPerOutputSample: Int,
        channelCount: Int,
        out: ShortArray
    ): Int {
        val outputFrames = channelBlocks[0].size / bytesPerOutputSample
        var outIndex = 0
        for (frame in 0 until outputFrames) {
            val base = frame * bytesPerOutputSample
            for (channel in 0 until channelCount) {
                var balance = 0
                val block = channelBlocks[channel]
                for (i in 0 until bytesPerOutputSample) {
                    balance += byteBalance[block[base + i].toInt() and 0xFF]
                }
                val normalized = balance.toFloat() / (bytesPerOutputSample * 8).toFloat()
                out[outIndex++] = (normalized * PCM_GAIN).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return outIndex
    }

    private fun resolveSeekOffset(info: DsfInfo, positionMs: Long): Pair<Long, Long> {
        val samplesPerBlock = info.blockSizePerChannel * 8L
        val targetDsdSample = ((positionMs.coerceIn(0L, info.durationMs) * info.sampleRate) / 1000L)
            .coerceIn(0L, info.sampleCount)
        val blockIndex = targetDsdSample / samplesPerBlock
        val offset = info.audioDataStart + blockIndex * info.blockSizePerChannel * info.channelCount
        val outputSamples = ((blockIndex * samplesPerBlock) / (info.sampleRate / outputSampleRate))
        return offset.coerceIn(info.audioDataStart, info.audioDataEnd) to outputSamples
    }

    private fun createAudioTrack(sampleRate: Int, channelCount: Int): AudioTrack {
        val channelMask = resolveChannelMask(channelCount)
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelMask,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) {
            throw IllegalStateException("Unsupported AudioTrack sample rate: $sampleRate")
        }
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBufferSize * 4)
            .build()
    }

    private fun releaseAudioTrack() {
        val track = audioTrack
        audioTrack = null
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.release() }
    }

    private fun publishState(isPlaying: Boolean) {
        val path = currentPath
        onStateChanged(
            DsdPlaybackState(
                path = path,
                isPlaying = isPlaying,
                positionMs = currentPositionMs(),
                durationMs = currentDurationMs
            )
        )
    }

    private fun parseDsf(path: String): DsfInfo {
        RandomAccessFile(File(path), "r").use { raf ->
            if (raf.readAscii(4) != "DSD ") {
                throw IllegalArgumentException("Not a DSF file")
            }
            val dsdChunkSize = raf.readLongLe()
            if (dsdChunkSize < 28L) {
                throw IllegalArgumentException("Invalid DSF header")
            }
            val fileSize = raf.readLongLe()
            raf.readLongLe()
            var channelCount = 0
            var sampleRate = 0
            var bitsPerSample = 0
            var sampleCount = 0L
            var blockSizePerChannel = 0
            var audioDataStart = 0L
            var audioDataEnd = 0L

            var cursor = dsdChunkSize
            val boundedFileSize = fileSize.takeIf { it > 0L }?.coerceAtMost(raf.length()) ?: raf.length()
            while (cursor + 12L <= boundedFileSize) {
                raf.seek(cursor)
                val id = raf.readAscii(4)
                val chunkSize = raf.readLongLe()
                if (chunkSize < 12L || cursor + chunkSize > raf.length() + 1L) break
                val payloadStart = raf.filePointer
                when (id) {
                    "fmt " -> {
                        raf.readIntLe()
                        raf.readIntLe()
                        raf.readIntLe()
                        channelCount = raf.readIntLe()
                        sampleRate = raf.readIntLe()
                        bitsPerSample = raf.readIntLe()
                        sampleCount = raf.readLongLe()
                        blockSizePerChannel = raf.readIntLe()
                    }
                    "data" -> {
                        raf.readLongLe()
                        audioDataStart = raf.filePointer
                        audioDataEnd = (cursor + chunkSize).coerceAtMost(raf.length())
                    }
                }
                cursor = payloadStart + chunkSize - 12L
            }

            if (channelCount !in 1..2) {
                throw IllegalArgumentException("Only mono/stereo DSF is supported")
            }
            if (bitsPerSample != 1) {
                throw IllegalArgumentException("DST-compressed DSF is not supported")
            }
            if (sampleRate <= 0 || sampleCount <= 0L || blockSizePerChannel <= 0) {
                throw IllegalArgumentException("Invalid DSF format chunk")
            }
            if (audioDataStart <= 0L || audioDataEnd <= audioDataStart) {
                throw IllegalArgumentException("Missing DSF data chunk")
            }
            val outRate = chooseOutputSampleRate(sampleRate)
            val ratio = sampleRate / outRate
            if (sampleRate % outRate != 0 || ratio % 8 != 0) {
                throw IllegalArgumentException("Unsupported DSD rate: $sampleRate")
            }
            return DsfInfo(
                path = path,
                channelCount = channelCount,
                sampleRate = sampleRate,
                sampleCount = sampleCount,
                blockSizePerChannel = blockSizePerChannel,
                audioDataStart = audioDataStart,
                audioDataEnd = audioDataEnd
            )
        }
    }

    private fun chooseOutputSampleRate(sourceSampleRate: Int): Int {
        val preferred = intArrayOf(176_400, 88_200, 44_100)
        return preferred.firstOrNull { candidate ->
            sourceSampleRate % candidate == 0 && (sourceSampleRate / candidate) % 8 == 0
        } ?: 44_100
    }

    private fun chooseSupportedOutputSampleRate(sourceSampleRate: Int, channelCount: Int): Int {
        val channelMask = resolveChannelMask(channelCount)
        val preferred = intArrayOf(176_400, 88_200, 44_100)
        return preferred.firstOrNull { candidate ->
            sourceSampleRate % candidate == 0 &&
                (sourceSampleRate / candidate) % 8 == 0 &&
                AudioTrack.getMinBufferSize(
                    candidate,
                    channelMask,
                    AudioFormat.ENCODING_PCM_16BIT
                ) > 0
        } ?: chooseOutputSampleRate(sourceSampleRate)
    }

    private fun resolveChannelMask(channelCount: Int): Int {
        return if (channelCount == 1) {
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            AudioFormat.CHANNEL_OUT_STEREO
        }
    }

    private fun resetPlaybackHeadPosition(baseSamples: Long) {
        synchronized(playbackHeadLock) {
            playbackHeadBaseSamples = baseSamples.coerceAtLeast(0L)
            playbackHeadWrapSamples = 0L
            lastPlaybackHeadRaw = 0L
        }
    }

    private fun readPlaybackHeadPositionSamples(track: AudioTrack): Long {
        val raw = track.playbackHeadPosition.toLong() and 0xFFFF_FFFFL
        return synchronized(playbackHeadLock) {
            if (raw < lastPlaybackHeadRaw) {
                playbackHeadWrapSamples += 1L shl 32
            }
            lastPlaybackHeadRaw = raw
            playbackHeadBaseSamples + playbackHeadWrapSamples + raw
        }
    }

    private fun RandomAccessFile.readAscii(length: Int): String {
        val bytes = ByteArray(length)
        readFully(bytes)
        return bytes.toString(Charsets.US_ASCII)
    }

    private fun RandomAccessFile.readIntLe(): Int {
        val b0 = read()
        val b1 = read()
        val b2 = read()
        val b3 = read()
        if ((b0 or b1 or b2 or b3) < 0) throw IllegalArgumentException("Unexpected EOF")
        return (b0 and 0xFF) or
            ((b1 and 0xFF) shl 8) or
            ((b2 and 0xFF) shl 16) or
            ((b3 and 0xFF) shl 24)
    }

    private fun RandomAccessFile.readLongLe(): Long {
        var result = 0L
        for (shift in 0..56 step 8) {
            val value = read()
            if (value < 0) throw IllegalArgumentException("Unexpected EOF")
            result = result or ((value.toLong() and 0xFFL) shl shift)
        }
        return result
    }

    companion object {
        private const val TAG = "DsdAudioTrackPlayer"
        private const val PCM_GAIN = 28_000f
    }
}
