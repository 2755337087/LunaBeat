package com.example.LyricBox

import android.content.Context
import android.util.Log
import java.io.File

private const val PLAYLIST_DIR_NAME = "playlists"
private const val FAVORITES_PLAYLIST_FILE_NAME = "favorites.m3u8"
private const val PLAYBACK_QUEUE_FILE_NAME = "playback_queue.m3u8"

data class LocalPlaylistEntry(
    val path: String,
    val title: String = "",
    val artist: String = "",
    val durationSeconds: Long = -1L
)

object LocalPlaylistStore {
    private const val TAG = "LocalPlaylistStore"

    private fun ensurePlaylistDir(context: Context): File {
        val dir = File(context.filesDir, PLAYLIST_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun favoritesFile(context: Context): File {
        return File(ensurePlaylistDir(context), FAVORITES_PLAYLIST_FILE_NAME)
    }

    private fun playbackQueueFile(context: Context): File {
        return File(ensurePlaylistDir(context), PLAYBACK_QUEUE_FILE_NAME)
    }

    fun loadFavoritePaths(context: Context): Set<String> {
        return parseM3uPaths(favoritesFile(context)).toSet()
    }

    fun saveFavorites(context: Context, entries: List<LocalPlaylistEntry>) {
        writeM3u(favoritesFile(context), entries)
    }

    fun loadPlaybackQueuePaths(context: Context): List<String> {
        return parseM3uPaths(playbackQueueFile(context))
    }

    fun savePlaybackQueue(context: Context, entries: List<LocalPlaylistEntry>) {
        writeM3u(playbackQueueFile(context), entries)
    }

    private fun parseM3uPaths(file: File): List<String> {
        if (!file.exists()) return emptyList()
        return try {
            val lines = file.readLines(Charsets.UTF_8)
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing m3u file: ${file.absolutePath}", e)
            emptyList()
        }
    }

    private fun writeM3u(file: File, entries: List<LocalPlaylistEntry>) {
        try {
            file.parentFile?.mkdirs()
            val normalized = entries
                .map { it.copy(path = it.path.trim()) }
                .filter { it.path.isNotEmpty() }
                .distinctBy { it.path }
            val content = buildString {
                appendLine("#EXTM3U")
                normalized.forEach { entry ->
                    val name = buildString {
                        if (entry.artist.isNotBlank()) {
                            append(entry.artist)
                            append(" - ")
                        }
                        append(if (entry.title.isNotBlank()) entry.title else File(entry.path).nameWithoutExtension)
                    }
                    appendLine("#EXTINF:${entry.durationSeconds},$name")
                    appendLine(entry.path)
                }
            }
            file.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing m3u file: ${file.absolutePath}", e)
        }
    }
}
