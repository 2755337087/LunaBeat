package com.example.LyricBox

import android.content.Context
import android.os.SystemClock
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "DailyPlaybackStats")
data class DailyPlaybackStats(
    @PrimaryKey val date: String,
    val totalDurationMs: Long,
    val updatedAt: Long
)

@Entity(tableName = "SongPlaybackStats")
data class SongPlaybackStats(
    @PrimaryKey val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val totalDurationMs: Long,
    val playCount: Int,
    val lastPlayedAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "ArtistPlaybackStats")
data class ArtistPlaybackStats(
    @PrimaryKey val artistName: String,
    val totalDurationMs: Long,
    val playCount: Int,
    val lastPlayedAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "AlbumPlaybackStats")
data class AlbumPlaybackStats(
    @PrimaryKey val albumKey: String,
    val albumName: String,
    val artistName: String,
    val totalDurationMs: Long,
    val playCount: Int,
    val lastPlayedAt: Long,
    val updatedAt: Long
)

@Dao
interface PlaybackStatsDao {
    @Query("SELECT * FROM DailyPlaybackStats WHERE date = :date LIMIT 1")
    suspend fun getDailyByDate(date: String): DailyPlaybackStats?

    @Upsert
    suspend fun upsertDaily(item: DailyPlaybackStats)

    @Query("SELECT * FROM SongPlaybackStats WHERE songKey = :songKey LIMIT 1")
    suspend fun getSongByKey(songKey: String): SongPlaybackStats?

    @Upsert
    suspend fun upsertSong(item: SongPlaybackStats)

    @Query("SELECT * FROM ArtistPlaybackStats WHERE artistName = :artistName LIMIT 1")
    suspend fun getArtistByName(artistName: String): ArtistPlaybackStats?

    @Upsert
    suspend fun upsertArtist(item: ArtistPlaybackStats)

    @Query("SELECT * FROM AlbumPlaybackStats WHERE albumKey = :albumKey LIMIT 1")
    suspend fun getAlbumByKey(albumKey: String): AlbumPlaybackStats?

    @Upsert
    suspend fun upsertAlbum(item: AlbumPlaybackStats)

    @Query("SELECT totalDurationMs FROM DailyPlaybackStats WHERE date = :date LIMIT 1")
    suspend fun getDailyTotalDurationMs(date: String): Long?

    @Query("SELECT * FROM DailyPlaybackStats ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDailyStats(limit: Int): List<DailyPlaybackStats>

    @Query("SELECT * FROM SongPlaybackStats ORDER BY totalDurationMs DESC LIMIT :limit")
    suspend fun getTopSongs(limit: Int): List<SongPlaybackStats>

    @Query("SELECT * FROM ArtistPlaybackStats ORDER BY totalDurationMs DESC LIMIT :limit")
    suspend fun getTopArtists(limit: Int): List<ArtistPlaybackStats>

    @Query("SELECT * FROM AlbumPlaybackStats ORDER BY totalDurationMs DESC LIMIT :limit")
    suspend fun getTopAlbums(limit: Int): List<AlbumPlaybackStats>

    @Query("SELECT * FROM DailyPlaybackStats WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDailyStatsInRange(startDate: String, endDate: String): List<DailyPlaybackStats>

    @Query("SELECT * FROM SongPlaybackStats WHERE lastPlayedAt BETWEEN :startMs AND :endMs ORDER BY totalDurationMs DESC")
    suspend fun getSongsTouchedInRange(startMs: Long, endMs: Long): List<SongPlaybackStats>

    @Query("SELECT * FROM ArtistPlaybackStats WHERE lastPlayedAt BETWEEN :startMs AND :endMs ORDER BY totalDurationMs DESC")
    suspend fun getArtistsTouchedInRange(startMs: Long, endMs: Long): List<ArtistPlaybackStats>

    @Query("SELECT * FROM AlbumPlaybackStats WHERE lastPlayedAt BETWEEN :startMs AND :endMs ORDER BY totalDurationMs DESC")
    suspend fun getAlbumsTouchedInRange(startMs: Long, endMs: Long): List<AlbumPlaybackStats>
}

@Database(
    entities = [
        DailyPlaybackStats::class,
        SongPlaybackStats::class,
        ArtistPlaybackStats::class,
        AlbumPlaybackStats::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PlaybackStatsDatabase : RoomDatabase() {
    abstract fun playbackStatsDao(): PlaybackStatsDao
}

data class PlaybackStatsSong(
    val songKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long
)

data class PlaybackStatsRangeData(
    val dailyStats: List<DailyPlaybackStats>,
    val songs: List<SongPlaybackStats>,
    val artists: List<ArtistPlaybackStats>,
    val albums: List<AlbumPlaybackStats>
)

class PlaybackStatsManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private val stateLock = Any()

    private val database: PlaybackStatsDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            PlaybackStatsDatabase::class.java,
            "playback_stats.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    private val dao: PlaybackStatsDao
        get() = database.playbackStatsDao()

    private var currentSong: PlaybackStatsSong? = null
    private var isPlaybackActive = false
    private var isSeeking = false
    private var isBuffering = false
    private var currentSessionPlayedMs: Long = 0L
    private var currentSessionPlayCounted = false
    private var lastFlushRealtimeMs: Long = SystemClock.elapsedRealtime()

    private val pendingDaily = mutableMapOf<String, PendingDaily>()
    private val pendingSong = mutableMapOf<String, PendingSong>()
    private val pendingArtist = mutableMapOf<String, PendingArtist>()
    private val pendingAlbum = mutableMapOf<String, PendingAlbum>()

    fun onPlaybackStarted(song: PlaybackStatsSong?) {
        synchronized(stateLock) {
            if (song != null) {
                switchCurrentSongLocked(song)
            }
            isPlaybackActive = song != null || currentSong != null
        }
    }

    fun onPlaybackPaused() {
        synchronized(stateLock) {
            isPlaybackActive = false
        }
    }

    fun onPlaybackStopped() {
        synchronized(stateLock) {
            isPlaybackActive = false
            isSeeking = false
            isBuffering = false
            resetPlayCountSessionLocked()
        }
    }

    fun onSongChanged(oldSong: PlaybackStatsSong?, newSong: PlaybackStatsSong?) {
        synchronized(stateLock) {
            val previous = currentSong
            if (oldSong != null && previous?.songKey == oldSong.songKey) {
                // Keep metadata current until switch.
                currentSong = oldSong
            }
            if (newSong == null) {
                currentSong = null
                resetPlayCountSessionLocked()
                return
            }
            switchCurrentSongLocked(newSong)
        }
    }

    fun onPlaybackTick(currentSong: PlaybackStatsSong?, deltaMs: Long, tickEndWallClockMs: Long = System.currentTimeMillis()) {
        if (deltaMs <= 0L) return

        synchronized(stateLock) {
            if (currentSong != null) {
                switchCurrentSongLocked(currentSong)
            }
            val song = this.currentSong ?: return
            if (!isPlaybackActive || isSeeking || isBuffering) return

            val safeDelta = deltaMs.coerceAtMost(MAX_SINGLE_TICK_MS)
            addDurationLocked(song, safeDelta, tickEndWallClockMs)

            currentSessionPlayedMs += safeDelta
            if (!currentSessionPlayCounted && isPlayCountQualified(song, currentSessionPlayedMs)) {
                addPlayCountLocked(song, tickEndWallClockMs)
                currentSessionPlayCounted = true
            }

            val nowRealtime = SystemClock.elapsedRealtime()
            if (nowRealtime - lastFlushRealtimeMs >= FLUSH_INTERVAL_MS) {
                scheduleFlushLocked(nowRealtime)
            }
        }
    }

    fun onSeekStarted() {
        synchronized(stateLock) {
            isSeeking = true
        }
    }

    fun onSeekFinished() {
        synchronized(stateLock) {
            isSeeking = false
        }
    }

    fun onBufferingStateChanged(buffering: Boolean) {
        synchronized(stateLock) {
            isBuffering = buffering
        }
    }

    fun flush() {
        val snapshot = synchronized(stateLock) {
            snapshotAndClearPendingLocked()
        }
        if (snapshot.isEmpty()) return

        scope.launch {
            flushSnapshot(snapshot)
        }
    }

    fun flushBlocking() {
        runBlocking(Dispatchers.IO) {
            flushPendingNow()
        }
    }

    suspend fun getDayTotalDurationMs(date: String): Long {
        flushPendingNow()
        return dao.getDailyTotalDurationMs(date) ?: 0L
    }

    suspend fun getRecentDailyPlaybackStats(days: Int): List<DailyPlaybackStats> {
        flushPendingNow()
        return dao.getRecentDailyStats(days.coerceAtLeast(1))
    }

    suspend fun getTopSongs(limit: Int): List<SongPlaybackStats> {
        flushPendingNow()
        return dao.getTopSongs(limit.coerceAtLeast(1))
    }

    suspend fun getTopArtists(limit: Int): List<ArtistPlaybackStats> {
        flushPendingNow()
        return dao.getTopArtists(limit.coerceAtLeast(1))
    }

    suspend fun getTopAlbums(limit: Int): List<AlbumPlaybackStats> {
        flushPendingNow()
        return dao.getTopAlbums(limit.coerceAtLeast(1))
    }

    suspend fun getStatsInRange(startDate: String, endDate: String): PlaybackStatsRangeData {
        flushPendingNow()
        val rangeStartMs = parseDateStartMs(startDate)
        val rangeEndMs = parseDateEndMs(endDate)
        return PlaybackStatsRangeData(
            dailyStats = dao.getDailyStatsInRange(startDate, endDate),
            songs = dao.getSongsTouchedInRange(rangeStartMs, rangeEndMs),
            artists = dao.getArtistsTouchedInRange(rangeStartMs, rangeEndMs),
            albums = dao.getAlbumsTouchedInRange(rangeStartMs, rangeEndMs)
        )
    }

    private fun scheduleFlushLocked(nowRealtime: Long) {
        lastFlushRealtimeMs = nowRealtime
        val snapshot = snapshotAndClearPendingLocked()
        if (snapshot.isEmpty()) return
        scope.launch {
            flushSnapshot(snapshot)
        }
    }

    private fun switchCurrentSongLocked(song: PlaybackStatsSong) {
        if (currentSong?.songKey == song.songKey) {
            currentSong = song
            return
        }
        currentSong = song
        resetPlayCountSessionLocked()
    }

    private fun resetPlayCountSessionLocked() {
        currentSessionPlayedMs = 0L
        currentSessionPlayCounted = false
    }

    private fun addDurationLocked(song: PlaybackStatsSong, deltaMs: Long, tickEndWallClockMs: Long) {
        val safeSong = song.normalized()
        splitByDate(tickEndWallClockMs, deltaMs).forEach { (date, durationPart) ->
            val daily = pendingDaily.getOrPut(date) { PendingDaily(date = date) }
            daily.totalDurationMs += durationPart
            daily.updatedAt = tickEndWallClockMs
        }

        val songPending = pendingSong.getOrPut(safeSong.songKey) {
            PendingSong(
                songKey = safeSong.songKey,
                title = safeSong.title,
                artist = safeSong.artist,
                album = safeSong.album
            )
        }
        songPending.totalDurationMs += deltaMs
        songPending.lastPlayedAt = maxOf(songPending.lastPlayedAt, tickEndWallClockMs)
        songPending.updatedAt = tickEndWallClockMs
        songPending.title = safeSong.title
        songPending.artist = safeSong.artist
        songPending.album = safeSong.album

        val artistPending = pendingArtist.getOrPut(safeSong.artist) {
            PendingArtist(artistName = safeSong.artist)
        }
        artistPending.totalDurationMs += deltaMs
        artistPending.lastPlayedAt = maxOf(artistPending.lastPlayedAt, tickEndWallClockMs)
        artistPending.updatedAt = tickEndWallClockMs

        val albumKey = buildAlbumKey(safeSong.album, safeSong.artist)
        val albumPending = pendingAlbum.getOrPut(albumKey) {
            PendingAlbum(
                albumKey = albumKey,
                albumName = safeSong.album,
                artistName = safeSong.artist
            )
        }
        albumPending.totalDurationMs += deltaMs
        albumPending.lastPlayedAt = maxOf(albumPending.lastPlayedAt, tickEndWallClockMs)
        albumPending.updatedAt = tickEndWallClockMs
        albumPending.albumName = safeSong.album
        albumPending.artistName = safeSong.artist
    }

    private fun addPlayCountLocked(song: PlaybackStatsSong, playedAtMs: Long) {
        val safeSong = song.normalized()

        pendingSong.getOrPut(safeSong.songKey) {
            PendingSong(
                songKey = safeSong.songKey,
                title = safeSong.title,
                artist = safeSong.artist,
                album = safeSong.album
            )
        }.apply {
            playCount += 1
            lastPlayedAt = maxOf(lastPlayedAt, playedAtMs)
            updatedAt = playedAtMs
            title = safeSong.title
            artist = safeSong.artist
            album = safeSong.album
        }

        pendingArtist.getOrPut(safeSong.artist) {
            PendingArtist(artistName = safeSong.artist)
        }.apply {
            playCount += 1
            lastPlayedAt = maxOf(lastPlayedAt, playedAtMs)
            updatedAt = playedAtMs
        }

        val albumKey = buildAlbumKey(safeSong.album, safeSong.artist)
        pendingAlbum.getOrPut(albumKey) {
            PendingAlbum(
                albumKey = albumKey,
                albumName = safeSong.album,
                artistName = safeSong.artist
            )
        }.apply {
            playCount += 1
            lastPlayedAt = maxOf(lastPlayedAt, playedAtMs)
            updatedAt = playedAtMs
            albumName = safeSong.album
            artistName = safeSong.artist
        }
    }

    private fun isPlayCountQualified(song: PlaybackStatsSong, playedMs: Long): Boolean {
        if (playedMs >= MIN_PLAY_COUNT_DURATION_MS) return true
        val duration = song.durationMs
        if (duration <= 0L) return false
        return playedMs * 2L >= duration
    }

    private fun snapshotAndClearPendingLocked(): PendingSnapshot {
        val snapshot = PendingSnapshot(
            daily = pendingDaily.values.map { it.copy() },
            songs = pendingSong.values.map { it.copy() },
            artists = pendingArtist.values.map { it.copy() },
            albums = pendingAlbum.values.map { it.copy() }
        )
        pendingDaily.clear()
        pendingSong.clear()
        pendingArtist.clear()
        pendingAlbum.clear()
        return snapshot
    }

    private suspend fun flushSnapshot(snapshot: PendingSnapshot) {
        if (snapshot.isEmpty()) return

        writeMutex.withLock {
            database.withTransaction {
                snapshot.daily.forEach { pending ->
                    val current = dao.getDailyByDate(pending.date)
                    val merged = if (current == null) {
                        DailyPlaybackStats(
                            date = pending.date,
                            totalDurationMs = pending.totalDurationMs,
                            updatedAt = pending.updatedAt
                        )
                    } else {
                        current.copy(
                            totalDurationMs = current.totalDurationMs + pending.totalDurationMs,
                            updatedAt = maxOf(current.updatedAt, pending.updatedAt)
                        )
                    }
                    dao.upsertDaily(merged)
                }

                snapshot.songs.forEach { pending ->
                    val current = dao.getSongByKey(pending.songKey)
                    val merged = if (current == null) {
                        SongPlaybackStats(
                            songKey = pending.songKey,
                            title = pending.title,
                            artist = pending.artist,
                            album = pending.album,
                            totalDurationMs = pending.totalDurationMs,
                            playCount = pending.playCount,
                            lastPlayedAt = pending.lastPlayedAt,
                            updatedAt = pending.updatedAt
                        )
                    } else {
                        current.copy(
                            title = pending.title,
                            artist = pending.artist,
                            album = pending.album,
                            totalDurationMs = current.totalDurationMs + pending.totalDurationMs,
                            playCount = current.playCount + pending.playCount,
                            lastPlayedAt = maxOf(current.lastPlayedAt, pending.lastPlayedAt),
                            updatedAt = maxOf(current.updatedAt, pending.updatedAt)
                        )
                    }
                    dao.upsertSong(merged)
                }

                snapshot.artists.forEach { pending ->
                    val current = dao.getArtistByName(pending.artistName)
                    val merged = if (current == null) {
                        ArtistPlaybackStats(
                            artistName = pending.artistName,
                            totalDurationMs = pending.totalDurationMs,
                            playCount = pending.playCount,
                            lastPlayedAt = pending.lastPlayedAt,
                            updatedAt = pending.updatedAt
                        )
                    } else {
                        current.copy(
                            totalDurationMs = current.totalDurationMs + pending.totalDurationMs,
                            playCount = current.playCount + pending.playCount,
                            lastPlayedAt = maxOf(current.lastPlayedAt, pending.lastPlayedAt),
                            updatedAt = maxOf(current.updatedAt, pending.updatedAt)
                        )
                    }
                    dao.upsertArtist(merged)
                }

                snapshot.albums.forEach { pending ->
                    val current = dao.getAlbumByKey(pending.albumKey)
                    val merged = if (current == null) {
                        AlbumPlaybackStats(
                            albumKey = pending.albumKey,
                            albumName = pending.albumName,
                            artistName = pending.artistName,
                            totalDurationMs = pending.totalDurationMs,
                            playCount = pending.playCount,
                            lastPlayedAt = pending.lastPlayedAt,
                            updatedAt = pending.updatedAt
                        )
                    } else {
                        current.copy(
                            albumName = pending.albumName,
                            artistName = pending.artistName,
                            totalDurationMs = current.totalDurationMs + pending.totalDurationMs,
                            playCount = current.playCount + pending.playCount,
                            lastPlayedAt = maxOf(current.lastPlayedAt, pending.lastPlayedAt),
                            updatedAt = maxOf(current.updatedAt, pending.updatedAt)
                        )
                    }
                    dao.upsertAlbum(merged)
                }
            }
        }
    }

    private suspend fun flushPendingNow() {
        val snapshot = synchronized(stateLock) {
            snapshotAndClearPendingLocked()
        }
        if (snapshot.isEmpty()) return
        flushSnapshot(snapshot)
    }

    private fun splitByDate(endWallClockMs: Long, deltaMs: Long): List<Pair<String, Long>> {
        val result = ArrayList<Pair<String, Long>>(2)
        var remaining = deltaMs
        val absoluteEnd = endWallClockMs.coerceAtLeast(0L)
        while (remaining > 0L) {
            val segmentStart = absoluteEnd - remaining
            val nextDayStart = startOfNextDayMs(segmentStart)
            val segmentEnd = minOf(absoluteEnd, nextDayStart)
            val part = (segmentEnd - segmentStart).coerceAtLeast(0L)
            if (part <= 0L) break
            result += formatDate(segmentStart) to part
            remaining -= part
        }
        return result
    }

    private fun formatDate(timeMs: Long): String {
        return dayFormatter.get()!!.format(Date(timeMs))
    }

    private fun startOfNextDayMs(timeMs: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMs
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun parseDateStartMs(date: String): Long {
        return dayFormatter.get()!!.parse(date)?.time ?: 0L
    }

    private fun parseDateEndMs(date: String): Long {
        val start = parseDateStartMs(date)
        return startOfNextDayMs(start) - 1L
    }

    private fun buildAlbumKey(album: String, artist: String): String {
        return "${artist.trim().lowercase(Locale.ROOT)}|${album.trim().lowercase(Locale.ROOT)}"
    }

    private fun PlaybackStatsSong.normalized(): PlaybackStatsSong {
        val safeKey = songKey.takeIf { it.isNotBlank() } ?: title.takeIf { it.isNotBlank() } ?: "unknown_song"
        val safeTitle = title.ifBlank { File(safeKey).nameWithoutExtension.ifBlank { "Unknown Title" } }
        val safeArtist = artist.ifBlank { "Unknown Artist" }
        val safeAlbum = album.ifBlank { "Unknown Album" }
        return copy(songKey = safeKey, title = safeTitle, artist = safeArtist, album = safeAlbum)
    }

    companion object {
        private const val MIN_PLAY_COUNT_DURATION_MS = 30_000L
        private const val MAX_SINGLE_TICK_MS = 60_000L
        private const val FLUSH_INTERVAL_MS = 20_000L

        private val dayFormatter = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            }
        }
    }
}

private data class PendingDaily(
    val date: String,
    var totalDurationMs: Long = 0L,
    var updatedAt: Long = 0L
)

private data class PendingSong(
    val songKey: String,
    var title: String,
    var artist: String,
    var album: String,
    var totalDurationMs: Long = 0L,
    var playCount: Int = 0,
    var lastPlayedAt: Long = 0L,
    var updatedAt: Long = 0L
)

private data class PendingArtist(
    val artistName: String,
    var totalDurationMs: Long = 0L,
    var playCount: Int = 0,
    var lastPlayedAt: Long = 0L,
    var updatedAt: Long = 0L
)

private data class PendingAlbum(
    val albumKey: String,
    var albumName: String,
    var artistName: String,
    var totalDurationMs: Long = 0L,
    var playCount: Int = 0,
    var lastPlayedAt: Long = 0L,
    var updatedAt: Long = 0L
)

private data class PendingSnapshot(
    val daily: List<PendingDaily>,
    val songs: List<PendingSong>,
    val artists: List<PendingArtist>,
    val albums: List<PendingAlbum>
) {
    fun isEmpty(): Boolean {
        return daily.isEmpty() && songs.isEmpty() && artists.isEmpty() && albums.isEmpty()
    }
}

private object PlaybackStatsManagerHolder {
    @Volatile
    private var instance: PlaybackStatsManager? = null

    fun get(context: Context): PlaybackStatsManager {
        return instance ?: synchronized(this) {
            instance ?: PlaybackStatsManager(context).also { instance = it }
        }
    }
}

fun playbackStatsManagerOf(context: Context): PlaybackStatsManager {
    return PlaybackStatsManagerHolder.get(context)
}
