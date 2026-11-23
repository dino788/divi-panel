package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiviDao {

    // --- Playlists ---

    @Query("SELECT * FROM playlists ORDER BY isActive DESC, lastSyncedAt DESC, id DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    fun observeActivePlaylist(): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlaylistOnce(): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET isActive = 0")
    suspend fun clearActivePlaylists()

    @Query("UPDATE playlists SET isActive = 1 WHERE id = :playlistId")
    suspend fun setPlaylistActive(playlistId: Long)

    @Transaction
    suspend fun activatePlaylist(playlistId: Long) {
        clearActivePlaylists()
        setPlaylistActive(playlistId)
    }

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    // --- Categories ---

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND mediaType = :mediaType ORDER BY id ASC")
    fun observeCategories(playlistId: Long, mediaType: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId ORDER BY mediaType ASC, categoryName ASC")
    fun observeAllCategories(playlistId: Long): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET isLocked = :locked WHERE id = :categoryRowId")
    suspend fun setCategoryLocked(categoryRowId: Long, locked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE playlistId = :playlistId")
    suspend fun deleteCategoriesForPlaylist(playlistId: Long)

    // --- Media Items ---

    @Query(
        """
        SELECT * FROM media_items 
        WHERE playlistId = :playlistId AND mediaType = :mediaType
        ORDER BY channelNumber ASC, id ASC
        LIMIT 3000
        """
    )
    fun observeMediaByType(playlistId: Long, mediaType: String): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items 
        WHERE playlistId = :playlistId AND mediaType = :mediaType AND name LIKE '%' || :query || '%'
        ORDER BY channelNumber ASC, id ASC
        LIMIT 500
        """
    )
    fun searchMediaByType(playlistId: Long, mediaType: String, query: String): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items 
        WHERE playlistId = :playlistId AND mediaType = :mediaType AND categoryId = :categoryId
        ORDER BY channelNumber ASC, id ASC
        """
    )
    fun observeMediaByCategory(
        playlistId: Long,
        mediaType: String,
        categoryId: String
    ): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE playlistId = :playlistId AND isFavorite = 1
        ORDER BY mediaType ASC, name ASC
        """
    )
    fun observeFavorites(playlistId: Long): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE playlistId = :playlistId AND mediaType = :mediaType AND isFavorite = 1
        ORDER BY name ASC
        """
    )
    fun observeFavoritesByType(playlistId: Long, mediaType: String): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE playlistId = :playlistId AND lastWatchedAt > 0
        ORDER BY lastWatchedAt DESC
        LIMIT 40
        """
    )
    fun observeRecentlyWatched(playlistId: Long): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE playlistId = :playlistId AND mediaType = :mediaType AND lastWatchedAt > 0
        ORDER BY lastWatchedAt DESC
        LIMIT 40
        """
    )
    fun observeRecentlyWatchedByType(playlistId: Long, mediaType: String): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%'
        ORDER BY mediaType ASC, name ASC
        LIMIT 200
        """
    )
    fun searchMedia(playlistId: Long, query: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :itemId LIMIT 1")
    suspend fun getMediaItemById(itemId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id = :itemId LIMIT 1")
    fun observeMediaItemById(itemId: Long): Flow<MediaItemEntity?>

    @Query("SELECT streamId FROM media_items WHERE playlistId = :playlistId AND isFavorite = 1")
    suspend fun getFavoriteStreamIds(playlistId: Long): List<String>

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :itemId")
    suspend fun setFavorite(itemId: Long, isFavorite: Boolean)

    @Query(
        """
        UPDATE media_items 
        SET lastWatchedAt = :watchedAt, resumePositionMs = :positionMs, durationMs = :durationMs 
        WHERE id = :itemId
        """
    )
    suspend fun updateWatchProgress(itemId: Long, watchedAt: Long, positionMs: Long, durationMs: Long)

    @Query("UPDATE media_items SET lastWatchedAt = 0, resumePositionMs = 0 WHERE playlistId = :playlistId")
    suspend fun clearWatchHistory(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE playlistId = :playlistId")
    suspend fun deleteMediaForPlaylist(playlistId: Long)

    @Transaction
    suspend fun replacePlaylistContent(
        playlistId: Long,
        categories: List<CategoryEntity>,
        mediaItems: List<MediaItemEntity>
    ) {
        deleteCategoriesForPlaylist(playlistId)
        deleteMediaForPlaylist(playlistId)
        if (categories.isNotEmpty()) {
            categories.chunked(500).forEach { insertCategories(it) }
        }
        if (mediaItems.isNotEmpty()) {
            mediaItems.chunked(1000).forEach { insertMediaItems(it) }
        }
    }

    @Transaction
    suspend fun deletePlaylistCompletely(playlistId: Long) {
        deleteCategoriesForPlaylist(playlistId)
        deleteMediaForPlaylist(playlistId)
        deletePlaylistById(playlistId)
    }
}
