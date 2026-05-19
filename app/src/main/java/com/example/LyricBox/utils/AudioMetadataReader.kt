package com.example.LyricBox.utils

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.lonx.audiotag.TagLib
import com.lonx.audiotag.model.AudioTagData
import com.lonx.audiotag.rw.AudioTagReader
import com.lonx.audiotag.rw.AudioTagWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AudioMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val comment: String = "",
    val lyrics: String = "",
    val duration: Long = 0,
    val cover: ByteArray? = null,
    val year: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
    val albumArtist: String = ""
)

data class WriteResult(
    val success: Boolean,
    val errorMessage: String = "",
    val needPermission: Boolean = false,
    val recoverableIntentSender: IntentSender? = null
)

object AudioMetadataReader {
    private const val TAG = "AudioMetadataReader"

    private fun resolveMediaStoreUri(context: Context, filePath: String, mediaStoreId: Long = -1L): Uri? {
        return try {
            if (mediaStoreId > 0L) {
                return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
            }
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DATA} = ?"
            val args = arrayOf(filePath)
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveMediaStoreUri failed for path=$filePath, id=$mediaStoreId", e)
            null
        }
    }
    
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            }
        }
    }
    
    fun readMetadata(filePath: String, includeCover: Boolean = true): AudioMetadata {
        val file = File(filePath)
        if (!file.exists()) {
            return AudioMetadata()
        }
        
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val result = readMetadataFromPfd(pfd, includeCover = includeCover)
            pfd.close()
            if (!includeCover || result.cover != null) {
                result
            } else {
                result.copy(cover = extractCoverWithRetriever(filePath))
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "TagLib unavailable, falling back to framework retriever for $filePath", e)
            readMetadataWithRetriever(filePath, includeCover)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading metadata from $filePath", e)
            readMetadataWithRetriever(filePath, includeCover)
        }
    }
    
    fun readMetadataFromUri(context: Context, uri: Uri, includeCover: Boolean = true): AudioMetadata {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) {
                return AudioMetadata()
            }
            val result = readMetadataFromPfd(pfd, includeCover = includeCover)
            pfd.close()
            if (!includeCover || result.cover != null) {
                result
            } else {
                result.copy(cover = extractCoverWithRetriever(context, uri))
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "TagLib unavailable, falling back to framework retriever for uri $uri", e)
            readMetadataWithRetriever(context, uri, includeCover)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading metadata from uri $uri", e)
            readMetadataWithRetriever(context, uri, includeCover)
        }
    }

    fun readMetadata(
        context: Context,
        filePath: String,
        mediaStoreId: Long = -1L,
        includeCover: Boolean = true
    ): AudioMetadata {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.Q) {
            val mediaUri = resolveMediaStoreUri(context, filePath, mediaStoreId)
            if (mediaUri != null) {
                return readMetadataFromUri(context, mediaUri, includeCover = includeCover)
            }
        }
        val fromFile = readMetadata(filePath, includeCover = includeCover)
        if (!shouldFallbackToUri(fromFile, includeCover = includeCover)) {
            return fromFile
        }

        val mediaUri = resolveMediaStoreUri(context, filePath, mediaStoreId) ?: return fromFile
        val fromUri = readMetadataFromUri(context, mediaUri, includeCover = includeCover)
        return mergePreferPrimary(fromFile, fromUri)
    }

    private fun readMetadataWithRetriever(filePath: String, includeCover: Boolean): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val coverData = if (includeCover) retriever.embeddedPicture else null
            AudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty(),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
                comment = "",
                lyrics = "",
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                cover = coverData,
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR).orEmpty(),
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).orEmpty(),
                discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).orEmpty(),
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Framework retriever fallback failed for $filePath", e)
            AudioMetadata()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun readMetadataWithRetriever(context: Context, uri: Uri, includeCover: Boolean): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val coverData = if (includeCover) retriever.embeddedPicture else null
            AudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty(),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
                comment = "",
                lyrics = "",
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                cover = coverData,
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR).orEmpty(),
                trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).orEmpty(),
                discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER).orEmpty(),
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Framework retriever fallback failed for uri $uri", e)
            AudioMetadata()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun shouldFallbackToUri(metadata: AudioMetadata, includeCover: Boolean): Boolean {
        return (includeCover && metadata.cover == null) ||
            metadata.title.isBlank() ||
            metadata.artist.isBlank() ||
            metadata.album.isBlank() ||
            metadata.duration <= 0L
    }

    private fun mergePreferPrimary(primary: AudioMetadata, fallback: AudioMetadata): AudioMetadata {
        return primary.copy(
            title = primary.title.takeIf { it.isNotBlank() } ?: fallback.title,
            artist = primary.artist.takeIf { it.isNotBlank() } ?: fallback.artist,
            album = primary.album.takeIf { it.isNotBlank() } ?: fallback.album,
            comment = primary.comment.takeIf { it.isNotBlank() } ?: fallback.comment,
            lyrics = primary.lyrics.takeIf { it.isNotBlank() } ?: fallback.lyrics,
            duration = if (primary.duration > 0L) primary.duration else fallback.duration,
            cover = primary.cover ?: fallback.cover,
            year = primary.year.takeIf { it.isNotBlank() } ?: fallback.year,
            trackNumber = primary.trackNumber.takeIf { it.isNotBlank() } ?: fallback.trackNumber,
            discNumber = primary.discNumber.takeIf { it.isNotBlank() } ?: fallback.discNumber,
            albumArtist = primary.albumArtist.takeIf { it.isNotBlank() } ?: fallback.albumArtist
        )
    }

    private fun extractCoverWithRetriever(filePath: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            retriever.embeddedPicture
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever cover fallback failed for path=$filePath", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractCoverWithRetriever(context: Context, uri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever cover fallback failed for uri=$uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
    
    private fun readMetadataFromPfd(pfd: ParcelFileDescriptor, includeCover: Boolean = true): AudioMetadata {
        return try {
            val nativeFd = pfd.dup().detachFd()
            
            val audioProps = TagLib.getAudioProperties(nativeFd)
            val metaFd = pfd.dup().detachFd()
            val metadata = TagLib.getMetadata(metaFd, false)
            
            val coverData = if (includeCover) {
                val coverFd = pfd.dup().detachFd()
                val coverPicture = TagLib.getFrontCover(coverFd)
                coverPicture?.data
            } else {
                null
            }
            
            if (metadata == null) {
                return AudioMetadata(cover = coverData)
            }
            
            val props = metadata.propertyMap
            
            fun firstOf(vararg keys: String): String? {
                for (key in keys) {
                    val arr = props[key]
                    if (!arr.isNullOrEmpty()) {
                        val value = arr[0].trim()
                        if (value.isNotEmpty()) return value
                    }
                }
                return null
            }
            
            val lyrics = firstOf(
                "LYRICS",
                "UNSYNCED LYRICS",
                "USLT",
                "LYRIC",
                "LYRICSENG"
            )
            
            AudioMetadata(
                title = firstOf("TITLE") ?: "",
                artist = firstOf("ARTIST") ?: "",
                album = firstOf("ALBUM") ?: "",
                comment = firstOf("COMMENT", "DESCRIPTION", "COMMENTS", "COMM") ?: "",
                lyrics = lyrics ?: "",
                duration = audioProps?.length?.toLong() ?: 0L,
                cover = coverData,
                year = firstOf("YEAR", "DATE") ?: "",
                trackNumber = firstOf("TRACK", "TRACKNUMBER") ?: "",
                discNumber = firstOf("DISC", "DISCNUMBER") ?: "",
                albumArtist = firstOf("ALBUMARTIST", "ALBUM ARTIST") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading metadata from pfd", e)
            AudioMetadata()
        }
    }
    
    fun readLyrics(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) {
            return null
        }
        
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val nativeFd = pfd.dup().detachFd()
            val metadata = TagLib.getMetadata(nativeFd, false)
            pfd.close()
            
            if (metadata == null) {
                return null
            }
            
            val props = metadata.propertyMap
            
            fun firstOf(vararg keys: String): String? {
                for (key in keys) {
                    val arr = props[key]
                    if (!arr.isNullOrEmpty()) {
                        val value = arr[0].trim()
                        if (value.isNotEmpty()) return value
                    }
                }
                return null
            }
            
            firstOf(
                "LYRICS",
                "UNSYNCED LYRICS",
                "USLT",
                "LYRIC",
                "LYRICSENG"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading lyrics from $filePath", e)
            null
        }
    }

    fun readLyrics(context: Context, filePath: String, mediaStoreId: Long = -1L): String? {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.Q) {
            return try {
                val mediaUri = resolveMediaStoreUri(context, filePath, mediaStoreId) ?: return null
                val pfd = context.contentResolver.openFileDescriptor(mediaUri, "r") ?: return null
                val nativeFd = pfd.dup().detachFd()
                val metadata = TagLib.getMetadata(nativeFd, false)
                pfd.close()
                val props = metadata?.propertyMap ?: return null

                fun firstOf(vararg keys: String): String? {
                    for (key in keys) {
                        val arr = props[key]
                        if (!arr.isNullOrEmpty()) {
                            val value = arr[0].trim()
                            if (value.isNotEmpty()) return value
                        }
                    }
                    return null
                }

                firstOf(
                    "LYRICS",
                    "UNSYNCED LYRICS",
                    "UNSYNCEDLYRICS",
                    "USLT",
                    "LYRIC",
                    "LYRICSENG"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error reading lyrics from uri (android10), path=$filePath", e)
                null
            }
        }

        val fromFile = readLyrics(filePath)
        if (!fromFile.isNullOrBlank()) return fromFile

        return try {
            val mediaUri = resolveMediaStoreUri(context, filePath, mediaStoreId) ?: return null
            val pfd = context.contentResolver.openFileDescriptor(mediaUri, "r") ?: return null
            val nativeFd = pfd.dup().detachFd()
            val metadata = TagLib.getMetadata(nativeFd, false)
            pfd.close()
            val props = metadata?.propertyMap ?: return null

            fun firstOf(vararg keys: String): String? {
                for (key in keys) {
                    val arr = props[key]
                    if (!arr.isNullOrEmpty()) {
                        val value = arr[0].trim()
                        if (value.isNotEmpty()) return value
                    }
                }
                return null
            }

            firstOf(
                "LYRICS",
                "UNSYNCED LYRICS",
                "UNSYNCEDLYRICS",
                "USLT",
                "LYRIC",
                "LYRICSENG"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading lyrics from uri fallback, path=$filePath", e)
            null
        }
    }
    
    suspend fun writeLyrics(
        context: Context,
        filePath: String,
        lyrics: String,
        mediaStoreId: Long = -1L
    ): WriteResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext WriteResult(false, "文件不存在: $filePath")
                }

                Log.d(TAG, "Writing lyrics to: $filePath, content length: ${lyrics.length}")

                val pfd = openReadWritePfdForLyrics(context, filePath, mediaStoreId)
                    ?: return@withContext WriteResult(false, "无法打开音频文件进行写入")

                val nativeFd = pfd.dup().detachFd()
                Log.d(TAG, "Native fd for reading: $nativeFd")

                val oldMeta = TagLib.getMetadata(nativeFd, false)
                val mapToSave = HashMap<String, Array<String>>()

                if (oldMeta != null) {
                    mapToSave.putAll(oldMeta.propertyMap)
                    Log.d(TAG, "Read existing properties: ${oldMeta.propertyMap.keys}")
                }

                mapToSave["LYRICS"] = arrayOf(lyrics)
                Log.d(TAG, "Properties to save: ${mapToSave.keys}")

                val saveFd = pfd.dup().detachFd()
                Log.d(TAG, "Native fd for saving: $saveFd")

                val success = TagLib.savePropertyMap(saveFd, mapToSave)
                Log.d(TAG, "TagLib.savePropertyMap result: $success")

                pfd.close()

                if (success) {
                    Log.d(TAG, "Lyrics written successfully")
                    WriteResult(true)
                } else {
                    WriteResult(false, "TagLib保存失败，可能是文件格式不支持")
                }
            } catch (e: RecoverableSecurityException) {
                val sender = e.userAction.actionIntent.intentSender
                WriteResult(
                    success = false,
                    errorMessage = "需要授予此歌曲的写入权限后才能继续",
                    needPermission = false,
                    recoverableIntentSender = sender
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "Security error writing lyrics to $filePath", e)
                val needAllFilesPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasStoragePermission(context)
                WriteResult(
                    success = false,
                    errorMessage = if (needAllFilesPermission) {
                        "需要\"所有文件访问\"权限才能写入此文件。请点击\"设置权限\"按钮授权。"
                    } else {
                        "写入失败: ${e.javaClass.simpleName} - ${e.message}"
                    },
                    needPermission = needAllFilesPermission
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error writing lyrics to $filePath", e)
                WriteResult(false, "写入失败: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }
    
    suspend fun writeLyricsToUri(context: Context, uri: Uri, lyrics: String): WriteResult {
        return withContext(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "rw")
                if (pfd == null) {
                    return@withContext WriteResult(false, "无法打开文件")
                }
                
                val updates = mapOf("LYRICS" to lyrics)
                val success = AudioTagWriter.writeTags(pfd, updates, true)
                pfd.close()
                
                if (success) {
                    WriteResult(true)
                } else {
                    WriteResult(false, "TagLib写入失败")
                }
            } catch (e: RecoverableSecurityException) {
                WriteResult(
                    success = false,
                    errorMessage = "需要授予此歌曲的写入权限后才能继续",
                    recoverableIntentSender = e.userAction.actionIntent.intentSender
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error writing lyrics to uri $uri", e)
                WriteResult(false, "写入失败: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }

    private fun openReadWritePfdForLyrics(
        context: Context,
        filePath: String,
        mediaStoreId: Long
    ): ParcelFileDescriptor? {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val mediaUri = resolveMediaStoreUri(context, filePath, mediaStoreId)
            if (mediaUri != null) {
                return context.contentResolver.openFileDescriptor(mediaUri, "rw")
            }
        }

        val file = File(filePath)
        if (file.exists()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        }

        val mediaUri = resolveMediaStoreUri(context, filePath, mediaStoreId) ?: return null
        return context.contentResolver.openFileDescriptor(mediaUri, "rw")
    }
}
