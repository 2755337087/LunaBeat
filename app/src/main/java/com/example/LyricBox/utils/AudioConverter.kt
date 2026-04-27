package com.example.LyricBox.utils

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.SessionState
import java.io.File
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.CopyOnWriteArraySet

object AudioConverter {
    
    private const val TAG = "AudioConverter"
    private var currentSession: FFmpegSession? = null
    private var progressSimulator: ProgressSimulator? = null
    private var currentFileName = ""
    
    private val refQueue = ReferenceQueue<FFmpegSession>()
    private val pendingRefs = CopyOnWriteArraySet<SessionPhantomRef>()
    
    init {
        val cleanupThread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val ref = refQueue.remove() as SessionPhantomRef
                    ref.cleanup()
                    pendingRefs.remove(ref)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }, "FFmpeg-Cleanup")
        cleanupThread.isDaemon = true
        cleanupThread.start()
    }
    
    private class SessionPhantomRef(
        referent: FFmpegSession
    ) : PhantomReference<FFmpegSession>(referent, refQueue) {
        private val sessionId: Long = referent.sessionId
        
        fun cleanup() {
            Log.d(TAG, "Phantom cleanup for session: $sessionId")
            FFmpegKit.cancel(sessionId)
        }
    }
    
    interface ConvertCallback {
        fun onProgress(progress: Int, time: Long)
        fun onComplete(success: Boolean, message: String)
        fun onError(error: String)
    }
    
    fun decodeToWav(
        inputPath: String,
        outputPath: String,
        callback: ConvertCallback?
    ) {
        val inputFile = File(inputPath)
        currentFileName = inputFile.name
        
        val commandList = mutableListOf<String>()
        commandList.add("-i")
        commandList.add(inputPath)
        
        commandList.add("-threads")
        commandList.add("0")
        
        commandList.add("-c:a")
        commandList.add("pcm_s16le")
        commandList.add("-ac")
        commandList.add("2")
        commandList.add("-y")
        commandList.add(outputPath)
        
        val command = commandList.toTypedArray()
        Log.d(TAG, "WAV解码命令: ${command.joinToString(" ")}")
        
        executeCommand(command, callback, inputPath)
    }
    
    private fun executeCommand(
        command: Array<String>,
        callback: ConvertCallback?,
        tempInputPath: String?
    ) {
        stopProgressSimulation()
        
        val commandBuilder = StringBuilder()
        for (i in command.indices) {
            val arg = command[i]
            
            if (isFilePathArgument(command, i, arg)) {
                commandBuilder.append("\"").append(arg).append("\"")
            } else {
                commandBuilder.append(arg)
            }
            
            if (i < command.size - 1) {
                commandBuilder.append(" ")
            }
        }
        
        val commandString = commandBuilder.toString()
        Log.d(TAG, "转义后的命令: $commandString")
        
        cancelCurrentTask()
        
        currentSession = FFmpegKit.executeAsync(commandString, object : FFmpegSessionCompleteCallback {
            override fun apply(session: FFmpegSession) {
                val returnCode = session.returnCode
                Log.d(TAG, "命令执行完成，返回码: ${returnCode?.value}")
                
                stopProgressSimulation()
                
                if (tempInputPath != null && tempInputPath.contains("/shared_files/")) {
                    deleteTempFile(tempInputPath)
                }
                
                callback?.let {
                    if (ReturnCode.isSuccess(returnCode)) {
                        it.onComplete(true, "转换完成")
                    } else {
                        var errorMessage = "转换失败"
                        if (session.failStackTrace != null) {
                            errorMessage += ": ${session.failStackTrace}"
                        } else if (returnCode != null) {
                            errorMessage += "，返回码: ${returnCode.value}"
                        }
                        it.onComplete(false, errorMessage)
                    }
                }
                
                currentSession = null
                currentFileName = ""
                FFmpegKitConfig.clearSessions()
            }
        })
        
        if (currentSession != null) {
            SessionPhantomRef(currentSession!!)
            startProgressSimulation(callback)
        } else {
            if (tempInputPath != null && tempInputPath.contains("/shared_files/")) {
                deleteTempFile(tempInputPath)
            }
            callback?.onError("命令执行失败，无法启动FFmpeg进程")
            currentFileName = ""
        }
    }
    
    private fun deleteTempFile(path: String) {
        try {
            val file = File(path)
            if (file.exists() && file.delete()) {
                Log.d(TAG, "已删除临时文件: $path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除临时文件失败: $path", e)
        }
    }
    
    private fun isFilePathArgument(command: Array<String>, index: Int, arg: String): Boolean {
        if (index > 0 && "-i" == command[index - 1]) {
            return true
        }
        
        if (index == command.size - 1) {
            return true
        }
        
        if (arg.contains("/") || arg.contains(".")) {
            return true
        }
        
        return false
    }
    
    fun cancelCurrentTask() {
        stopProgressSimulation()
        currentSession?.let { session ->
            val state = session.state
            if (state == SessionState.RUNNING || state == SessionState.CREATED) {
                Log.d(TAG, "Cancelling session: ${session.sessionId}")
                FFmpegKit.cancel(session.sessionId)
            }
            currentSession = null
        }
    }
    
    private fun startProgressSimulation(callback: ConvertCallback?) {
        if (callback == null) return
        stopProgressSimulation()
        progressSimulator = ProgressSimulator(callback)
        Thread(progressSimulator).start()
    }
    
    private fun stopProgressSimulation() {
        progressSimulator?.stop()
        progressSimulator = null
    }
    
    private class ProgressSimulator(
        private val callback: ConvertCallback
    ) : Runnable {
        @Volatile
        private var running = true
        
        override fun run() {
            var progress = 0
            val start = System.currentTimeMillis()
            try {
                while (running && progress < 95) {
                    Thread.sleep(500)
                    
                    if (currentSession == null || currentSession?.state == SessionState.COMPLETED) {
                        callback.onProgress(100, System.currentTimeMillis() - start)
                        break
                    }
                    progress += if (progress < 70) 5 else 2
                    callback.onProgress(minOf(progress, 95), System.currentTimeMillis() - start)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        
        fun stop() {
            running = false
        }
    }
}
