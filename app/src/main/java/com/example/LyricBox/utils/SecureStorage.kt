package com.example.LyricBox.utils

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStorage {
    init {
        System.loadLibrary("lyricbox")
    }
    
    private const val TAG = "SecureStorage"
    private const val PREFS_NAME = "SecurePrefs"
    private const val KEY_CLOUDFLARE_SECRET_KEY = "cloudflare_secret_key"

    @JvmStatic
    private external fun getNativeDefaultKey(): String

    private fun getDecodedDefaultKey(): String {
        return try {
            getNativeDefaultKey()
        } catch (e: Exception) {
            Log.e(TAG, "获取原生密钥失败", e)
            "244394"
        }
    }

    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCloudflareSecretKey(context: Context, secretKey: String) {
        try {
            val prefs = getEncryptedPrefs(context)
            prefs.edit().putString(KEY_CLOUDFLARE_SECRET_KEY, secretKey).apply()
            Log.d(TAG, "Cloudflare 密钥已保存")
        } catch (e: Exception) {
            Log.e(TAG, "保存密钥失败", e)
        }
    }

    fun getCloudflareSecretKey(context: Context): String {
        return try {
            val prefs = getEncryptedPrefs(context)
            val key = prefs.getString(KEY_CLOUDFLARE_SECRET_KEY, getDecodedDefaultKey())
            key ?: getDecodedDefaultKey()
        } catch (e: Exception) {
            Log.e(TAG, "读取密钥失败", e)
            getDecodedDefaultKey()
        }
    }

    fun initializeIfNeeded(context: Context) {
        val prefs = getEncryptedPrefs(context)
        if (!prefs.contains(KEY_CLOUDFLARE_SECRET_KEY)) {
            Log.d(TAG, "初始化密钥已保存默认密钥")
            saveCloudflareSecretKey(context, getDecodedDefaultKey())
        }
    }
}
