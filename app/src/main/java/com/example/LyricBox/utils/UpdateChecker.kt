package com.example.LyricBox.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val updateTime: String,
    val downloadName1: String?,
    val downloadName2: String?,
    val downloadUrl1: String?,
    val downloadUrl2: String?,
    val notice: String?,
    val showNotice: Boolean,
    val amUrl: String?,
    val amUrlName: String?,
    val amUrlCountry: String?,
    val amUrlNameContributor: String?,
    val amUrlCountryContributor: String?,
    val noticeContributor: String?
) : Parcelable {
    val hasDownloadUrl1: Boolean
        get() = !downloadUrl1.isNullOrEmpty()
    
    val hasDownloadUrl2: Boolean
        get() = !downloadUrl2.isNullOrEmpty()
    
    val downloadButtonCount: Int
        get() {
            var count = 0
            if (hasDownloadUrl1) count++
            if (hasDownloadUrl2) count++
            return count
        }
    
    constructor(parcel: Parcel) : this(
        versionCode = parcel.readInt(),
        versionName = parcel.readString() ?: "",
        changelog = parcel.readString() ?: "",
        updateTime = parcel.readString() ?: "",
        downloadName1 = parcel.readString(),
        downloadName2 = parcel.readString(),
        downloadUrl1 = parcel.readString(),
        downloadUrl2 = parcel.readString(),
        notice = parcel.readString(),
        showNotice = parcel.readByte() != 0.toByte(),
        amUrl = parcel.readString(),
        amUrlName = parcel.readString(),
        amUrlCountry = parcel.readString(),
        amUrlNameContributor = parcel.readString(),
        amUrlCountryContributor = parcel.readString(),
        noticeContributor = parcel.readString()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(versionCode)
        parcel.writeString(versionName)
        parcel.writeString(changelog)
        parcel.writeString(updateTime)
        parcel.writeString(downloadName1)
        parcel.writeString(downloadName2)
        parcel.writeString(downloadUrl1)
        parcel.writeString(downloadUrl2)
        parcel.writeString(notice)
        parcel.writeByte(if (showNotice) 1 else 0)
        parcel.writeString(amUrl)
        parcel.writeString(amUrlName)
        parcel.writeString(amUrlCountry)
        parcel.writeString(amUrlNameContributor)
        parcel.writeString(amUrlCountryContributor)
        parcel.writeString(noticeContributor)
    }
    
    override fun describeContents(): Int {
        return 0
    }
    
    companion object CREATOR : Parcelable.Creator<UpdateInfo> {
        override fun createFromParcel(parcel: Parcel): UpdateInfo {
            return UpdateInfo(parcel)
        }
        
        override fun newArray(size: Int): Array<UpdateInfo?> {
            return arrayOfNulls(size)
        }
    }
}

object UpdateChecker {
    const val UPDATE_URL = "https://gitee.com/lb244394/lyric-box/raw/master/update.json"
    private const val TIMEOUT_MS = 3000L
    
    suspend fun checkForUpdate(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(TIMEOUT_MS) {
                val currentVersionCode = getCurrentVersionCode(context)
                val updateInfo = fetchUpdateInfo()
                
                if (updateInfo != null) {
                    if (updateInfo.versionCode > currentVersionCode) {
                        UpdateResult.UpdateAvailable(updateInfo)
                    } else {
                        UpdateResult.NoUpdate(updateInfo.versionName)
                    }
                } else {
                    UpdateResult.Error("无法解析更新信息")
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            UpdateResult.Timeout
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "网络请求失败")
        }
    }
    
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "未知"
        } catch (e: PackageManager.NameNotFoundException) {
            "未知"
        }
    }
    
    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
    }
    
    private fun fetchUpdateInfo(): UpdateInfo? {
        val url = URL(UPDATE_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 3000
            readTimeout = 3000
        }
        
        return try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseUpdateInfo(response)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }
    
    private fun parseUpdateInfo(jsonString: String): UpdateInfo? {
        return try {
            val json = JSONObject(jsonString)
            UpdateInfo(
                versionCode = json.optInt("versionCode", 0),
                versionName = json.optString("versionName", ""),
                changelog = json.optString("changelog", ""),
                updateTime = json.optString("updateTime", ""),
                downloadName1 = json.optString("downloadName1", null).takeIf { it.isNotEmpty() },
                downloadName2 = json.optString("downloadName2", null).takeIf { it.isNotEmpty() },
                downloadUrl1 = json.optString("downloadUrl1", null).takeIf { it.isNotEmpty() },
                downloadUrl2 = json.optString("downloadUrl2", null).takeIf { it.isNotEmpty() },
                notice = json.optString("notice", null).takeIf { it.isNotEmpty() },
                showNotice = json.optBoolean("showNotice", false),
                amUrl = json.optString("AMURL", null).takeIf { it.isNotEmpty() },
                amUrlName = json.optString("AMURLname", null).takeIf { it.isNotEmpty() },
                amUrlCountry = json.optString("AMURLcountry", null).takeIf { it.isNotEmpty() },
                amUrlNameContributor = json.optString("AMURLname_contributor", null).takeIf { it.isNotEmpty() },
                amUrlCountryContributor = json.optString("AMURLcountry_contributor", null).takeIf { it.isNotEmpty() },
                noticeContributor = json.optString("Notice_contributor", null).takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }
}

sealed class UpdateResult {
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateResult()
    data class NoUpdate(val latestVersion: String = "") : UpdateResult()
    object Timeout : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}
