package com.auraplayer.offline

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val size: Long,
    val folder: String,
    val dateAdded: Long,
    val mime: String
)

object LocalLibrary {
    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val c = MediaStore.Audio.Media
        val columns = arrayOf(c._ID, c.TITLE, c.ARTIST, c.ALBUM, c.ALBUM_ID, c.DURATION,
            c.SIZE, c.DATE_ADDED, c.MIME_TYPE, MediaStore.MediaColumns.RELATIVE_PATH)
        try {
            context.contentResolver.query(c.EXTERNAL_CONTENT_URI, columns, null, null, "${c.DATE_ADDED} DESC")?.use { cursor ->
                fun number(name: String): Long = cursor.getLong(cursor.getColumnIndexOrThrow(name))
                fun value(name: String): String = cursor.getString(cursor.getColumnIndexOrThrow(name)) ?: ""
                while (cursor.moveToNext()) {
                    val id = number(c._ID)
                    list += Song(id, ContentUris.withAppendedId(c.EXTERNAL_CONTENT_URI, id),
                        value(c.TITLE).ifBlank { "Unknown song" },
                        value(c.ARTIST).takeUnless { it == "<unknown>" }.orEmpty().ifBlank { "Unknown artist" },
                        value(c.ALBUM).takeUnless { it == "<unknown>" }.orEmpty(),
                        number(c.ALBUM_ID), number(c.DURATION), number(c.SIZE),
                        value(MediaStore.MediaColumns.RELATIVE_PATH).trimEnd('/').ifBlank { "Music" },
                        number(c.DATE_ADDED), value(c.MIME_TYPE))
                }
            }
        } catch (_: SecurityException) { }
        list
    }
}

@Entity(tableName = "song_state")
data class SongState(
    @PrimaryKey val id: Long,
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val plays: Int = 0,
    val lastPlayed: Long = 0
)

@Entity(tableName = "playlists")
data class PlaylistRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val songIds: String = ""
) {
    fun ids(): List<Long> = songIds.split(',').mapNotNull { it.toLongOrNull() }
}

@Dao interface MusicDao {
    @Query("SELECT * FROM song_state") fun states(): Flow<List<SongState>>
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") fun playlists(): Flow<List<PlaylistRow>>
    @Query("SELECT * FROM song_state WHERE id=:id") suspend fun get(id: Long): SongState?
    @Upsert suspend fun put(state: SongState)
    @Insert suspend fun addPlaylist(row: PlaylistRow): Long
    @Update suspend fun updatePlaylist(row: PlaylistRow)
    @Delete suspend fun deletePlaylist(row: PlaylistRow)
}

@Database(entities = [SongState::class, PlaylistRow::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun dao(): MusicDao
    companion object {
        @Volatile private var instance: MusicDatabase? = null
        fun get(context: Context): MusicDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MusicDatabase::class.java, "as_music.db").build()
                .also { instance = it }
        }
    }
}
