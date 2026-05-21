package com.example.LyricBox.lyrics.api

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object Netease163KeyCodec {
    const val KEY_PREFIX = "163 key(Don't modify):"
    private const val RAW_AES_KEY = "#14ljk_!\\]&0U<'("
    private val secretKey = SecretKeySpec(RAW_AES_KEY.toByteArray(Charsets.UTF_8), "AES")

    data class DecodeResult(
        val normalizedBase64: String,
        val plaintext: String,
        val musicJson: String?
    )

    fun encodeMusicJson(musicJson: String): String {
        val plaintext = "music:$musicJson"
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return KEY_PREFIX + Base64.getEncoder().encodeToString(encrypted)
    }

    fun decode(input: String): DecodeResult {
        val normalizedBase64 = normalizeInputToBase64(input)
        val encryptedBytes = try {
            Base64.getDecoder().decode(normalizedBase64)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("输入不是有效的 Base64 163key", e)
        }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val rawPlainBytes = cipher.doFinal(encryptedBytes)
        val plainBytes = removePkcs5PaddingIfPresent(rawPlainBytes)
        val plaintext = String(plainBytes, Charsets.UTF_8)
        val musicJson = if (plaintext.startsWith("music:")) {
            plaintext.removePrefix("music:")
        } else {
            null
        }

        return DecodeResult(
            normalizedBase64 = normalizedBase64,
            plaintext = plaintext,
            musicJson = musicJson
        )
    }

    private fun normalizeInputToBase64(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("请输入 163key")
        }

        return when {
            trimmed.startsWith(KEY_PREFIX, ignoreCase = true) -> {
                trimmed.substring(KEY_PREFIX.length).trim()
            }
            trimmed.contains(':') && trimmed.lowercase().startsWith("163 key") -> {
                trimmed.substringAfter(':').trim()
            }
            else -> trimmed
        }
    }

    private fun removePkcs5PaddingIfPresent(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val padLength = data.last().toInt() and 0xFF
        if (padLength !in 1..16 || padLength > data.size) return data
        val start = data.size - padLength
        for (i in start until data.size) {
            if ((data[i].toInt() and 0xFF) != padLength) return data
        }
        return data.copyOfRange(0, start)
    }
}
