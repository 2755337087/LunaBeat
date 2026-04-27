package com.example.LyricBox.lyrics.decryptor

import android.util.Log
import java.util.zip.Inflater

object QrcDecryptor {
    private const val TAG = "QrcDecryptor"
    private const val QRC_KEY = "!@#)(*\$%123ZXC!@!@#)(NHL"
    
    fun decrypt(encryptedQrc: String): String? {
        if (encryptedQrc.isBlank()) {
            Log.e(TAG, "加密数据为空")
            return null
        }
        
        return try {
            Log.d(TAG, "开始解密, 加密数据长度: ${encryptedQrc.length}")
            val encryptedBytes = hexStringToByteArray(encryptedQrc)
            Log.d(TAG, "转换为字节数组, 长度: ${encryptedBytes.size}")
            decryptBytes(encryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "解密失败", e)
            null
        }
    }
    
    fun decryptBytes(encryptedBytes: ByteArray): String? {
        return try {
            val schedule = TripleDes.keySetup(QRC_KEY.toByteArray(), TripleDes.DECRYPT)
            val data = mutableListOf<Byte>()
            
            for (i in encryptedBytes.indices step 8) {
                val remaining = encryptedBytes.size - i
                if (remaining >= 8) {
                    val chunk = encryptedBytes.copyOfRange(i, i + 8)
                    val decryptedChunk = TripleDes.crypt(chunk, schedule[0], schedule[1], schedule[2])
                    data.addAll(decryptedChunk.toList())
                }
            }
            
            Log.d(TAG, "3DES解密完成, 数据长度: ${data.size}")
            decompress(data.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "解密失败", e)
            null
        }
    }
    
    private fun decompress(data: ByteArray): String {
        val decompressor = Inflater()
        decompressor.setInput(data)
        val output = ByteArray(1024 * 1024)
        val resultLength = decompressor.inflate(output)
        decompressor.end()
        Log.d(TAG, "解压完成, 结果长度: $resultLength")
        return String(output, 0, resultLength, Charsets.UTF_8)
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        return ByteArray(hex.length / 2) {
            hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }
}

object KrcDecryptor {
    private val KRC_KEY = byteArrayOf(
        0x40, 0x47, 0x61, 0x77, 0x5E, 0x32, 0x74, 0x47,
        0x51, 0x36, 0x31, 0x2D, 0xCE.toByte(), 0xD2.toByte(), 0x6E, 0x69
    )
    
    fun decrypt(encryptedData: ByteArray): String? {
        return try {
            val data = encryptedData.copyOfRange(4, encryptedData.size)
            val decrypted = ByteArray(data.size)
            
            for (i in data.indices) {
                decrypted[i] = (data[i].toInt() xor KRC_KEY[i % KRC_KEY.size].toInt()).toByte()
            }
            
            decompress(decrypted)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun decompress(data: ByteArray): String {
        val decompressor = Inflater()
        decompressor.setInput(data)
        val output = ByteArray(1024 * 1024)
        val resultLength = decompressor.inflate(output)
        decompressor.end()
        return String(output, 0, resultLength, Charsets.UTF_8)
    }
}
