package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AchievementEntity
import com.example.data.model.BlockedAppEntity
import com.example.data.model.DistractionEvent
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistTrackCrossRef
import com.example.data.model.StudyMusicTrack
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    // Study Sessions / Focus History
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE startTime >= :startOfDay")
    suspend fun getSessionsSince(startOfDay: Long): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE isCompleted = 1 ORDER BY startTime DESC")
    fun getCompletedSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): StudySession?

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): StudySession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Update
    suspend fun updateSession(session: StudySession)

    @Delete
    suspend fun deleteSession(session: StudySession)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllSessions()

    // Blocked Apps
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllAppConfigs(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isDistraction = 1")
    fun getDistractionApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT packageName FROM blocked_apps WHERE isDistraction = 1")
    suspend fun getDistractionPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllApps(apps: List<BlockedAppEntity>)

    @Query("UPDATE blocked_apps SET isDistraction = :isDistraction WHERE packageName = :packageName")
    suspend fun setAppDistractionState(packageName: String, isDistraction: Boolean)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackage(packageName: String)

    @Query("DELETE FROM blocked_apps WHERE packageName NOT IN (:packages)")
    suspend fun deleteAppsNotIn(packages: List<String>)

    // Distraction Events
    @Query("SELECT * FROM distraction_events ORDER BY timestamp DESC LIMIT 100")
    fun getRecentDistractions(): Flow<List<DistractionEvent>>

    @Query("SELECT COUNT(*) FROM distraction_events")
    fun getTotalDistractionsBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM distraction_events WHERE eventType = 'YOUTUBE_SHORTS_BLOCKED'")
    fun getShortsBlockedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistractionEvent(event: DistractionEvent)

    // Music Saver System
    @Query("SELECT * FROM study_music_tracks ORDER BY isFavorite DESC, dateAdded DESC")
    fun getAllMusicTracks(): Flow<List<StudyMusicTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusicTrack(track: StudyMusicTrack): Long

    @Update
    suspend fun updateMusicTrack(track: StudyMusicTrack)

    @Delete
    suspend fun deleteMusicTrack(track: StudyMusicTrack)

    @Query("UPDATE study_music_tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setTrackFavorite(id: Long, isFavorite: Boolean)

    // Achievements
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, targetValue ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): AchievementEntity?

    // Playlists
    @Query("SELECT * FROM study_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("""
        SELECT t.* FROM study_music_tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.addedAt ASC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<StudyMusicTrack>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: Long): Flow<Int>

    @Query("SELECT playlistId FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun getPlaylistsForTrack(trackId: Long): List<Long>
}
