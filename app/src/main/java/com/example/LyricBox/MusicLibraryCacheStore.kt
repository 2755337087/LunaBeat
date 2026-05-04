package com.example.LyricBox

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Entity(tableName = "music_library_cache")
data class MusicLibraryCacheEntity(
    @PrimaryKey val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val fileSize: Long,
    val lastModified: Long,
    val addedTime: Long,
    val coverCachePath: String?,
    val year: String,
    val mediaStoreId: Long
)

private fun AudioFile.toCacheEntity(): MusicLibraryCacheEntity {
    return MusicLibraryCacheEntity(
        path = path,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        fileSize = fileSize,
        lastModified = lastModified,
        addedTime = addedTime,
        coverCachePath = coverCachePath,
        year = year,
        mediaStoreId = mediaStoreId
    )
}

private fun MusicLibraryCacheEntity.toAudioFile(): AudioFile {
    return AudioFile(
        path = path,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        fileSize = fileSize,
        lastModified = lastModified,
        addedTime = addedTime,
        coverCachePath = coverCachePath,
        year = year,
        mediaStoreId = mediaStoreId
    )
}

@Dao
interface MusicLibraryCacheDao {
    @Query("SELECT COUNT(*) FROM music_library_cache")
    suspend fun countAll(): Int

    @Query("SELECT * FROM music_library_cache ORDER BY addedTime DESC LIMIT :limit OFFSET :offset")
    suspend fun loadPage(limit: Int, offset: Int): List<MusicLibraryCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MusicLibraryCacheEntity>)

    @Query("DELETE FROM music_library_cache")
    suspend fun clearAll()

    @Query("SELECT * FROM music_library_cache ORDER BY addedTime DESC")
    fun pagingSource(): PagingSource<Int, MusicLibraryCacheEntity>
}

@Database(
    entities = [MusicLibraryCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusicLibraryCacheDatabase : RoomDatabase() {
    abstract fun musicLibraryCacheDao(): MusicLibraryCacheDao
}

class MusicLibraryCacheStore(context: Context) {
    private val appContext = context.applicationContext
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private var writeJob: Job? = null

    private val database: MusicLibraryCacheDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            MusicLibraryCacheDatabase::class.java,
            "music_library_cache.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    private suspend fun replaceAllInternal(audioFiles: List<AudioFile>) {
        val dao = database.musicLibraryCacheDao()
        dao.clearAll()
        if (audioFiles.isEmpty()) return
        audioFiles
            .map { it.toCacheEntity() }
            .chunked(400)
            .forEach { chunk ->
                dao.upsertAll(chunk)
            }
    }

    suspend fun loadAllPaged(pageSize: Int = 300): List<AudioFile> {
        val dao = database.musicLibraryCacheDao()
        val total = dao.countAll()
        if (total <= 0) return emptyList()
        val result = ArrayList<AudioFile>(total)
        var offset = 0
        while (true) {
            val page = dao.loadPage(pageSize, offset)
            if (page.isEmpty()) break
            result.addAll(page.map { it.toAudioFile() })
            offset += page.size
        }
        return result
    }

    fun saveAllAsync(audioFiles: List<AudioFile>) {
        val snapshot = audioFiles.toList()
        writeJob?.cancel()
        writeJob = writeScope.launch {
            writeMutex.withLock {
                replaceAllInternal(snapshot)
            }
        }
    }

    suspend fun saveAllBlocking(audioFiles: List<AudioFile>) {
        writeMutex.withLock {
            replaceAllInternal(audioFiles)
        }
    }

    fun pager(pageSize: Int = 80): Pager<Int, MusicLibraryCacheEntity> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { database.musicLibraryCacheDao().pagingSource() }
        )
    }
}

private object MusicLibraryCacheStoreHolder {
    @Volatile
    private var instance: MusicLibraryCacheStore? = null

    fun get(context: Context): MusicLibraryCacheStore {
        return instance ?: synchronized(this) {
            instance ?: MusicLibraryCacheStore(context).also { instance = it }
        }
    }
}

fun musicLibraryCacheStore(context: Context): MusicLibraryCacheStore {
    return MusicLibraryCacheStoreHolder.get(context)
}
