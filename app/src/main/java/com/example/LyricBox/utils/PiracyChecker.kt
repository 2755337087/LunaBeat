package com.example.LyricBox.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

object PiracyChecker {
    private const val XOR_KEY = 0x5A
    
    private val ENCRYPTED_SIGNATURE_HASH = "YjtrYjg+Ozw8bW4+OG9qbGJqaWs+bz8+bmw5OGw8Yz44PDltPDtqamw+OzhqO2k+O28/OT9jaW1paWJjP2xoaw=="
    
    private val ENCRYPTED_DEVICE_IDS = listOf(
        "OG47bD48PDg4Ozk7Ozxvbg==",
        "bWhqO25ibD9saj9uO2pqaA==",
        "bTtja21qPjk4bThqaWNrOw=="
    )
    
    private fun xorDecryptWithBase64(encrypted: String, key: Int): String {
        val decoded = String(Base64.decode(encrypted, Base64.NO_WRAP), Charsets.UTF_8)
        return decoded.map { (it.code xor key).toChar() }.joinToString("")
    }
    
    private val EXPECTED_SIGNATURE_HASH: String by lazy {
        xorDecryptWithBase64(ENCRYPTED_SIGNATURE_HASH, XOR_KEY)
    }
    
    private val DEBUG_DEVICE_IDS: Set<String> by lazy {
        ENCRYPTED_DEVICE_IDS.map { xorDecryptWithBase64(it, XOR_KEY) }.toSet()
    }
    
    private fun getDeviceId(context: Context): String {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        Log.d("PiracyChecker", "Device ID: $deviceId")
        return deviceId
    }
    
    fun isDebugDevice(context: Context): Boolean {
        val deviceId = getDeviceId(context)
        return deviceId in DEBUG_DEVICE_IDS
    }
    
    private fun checkSignature(context: Context): Boolean {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                packageInfo.signatures
            }
            
            if (signatures.isNullOrEmpty()) {
                return false
            }
            
            for (signature in signatures) {
                val hash = getSignatureHash(signature)
                if (hash == EXPECTED_SIGNATURE_HASH) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getSignatureHash(signature: Signature): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(signature.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun checkAll(context: Context): PiracyCheckResult {
        if (isDebugDevice(context)) {
            return PiracyCheckResult(isPirated = false)
        }
        
        val signatureValid = checkSignature(context)
        
        return PiracyCheckResult(
            isPirated = !signatureValid
        )
    }
}

data class PiracyCheckResult(
    val isPirated: Boolean
)
