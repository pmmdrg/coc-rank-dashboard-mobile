package com.cocrank.dashboard.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "seasons")
data class SeasonEntity(
    @PrimaryKey val id: String, // format: "season_name" hoặc seasonId
    val seasonName: String,
    val league: String,
    val leagueIconUrl: String?,
    val startsAt: String,
    val endsAt: String,
    val maxAttacks: Int,
    val maxDefenses: Int,
    val promotionCount: Int,
    val demotionCount: Int,
    val myPlayerId: String,
    val leagueGroupTag: String?,
    val leagueSeasonId: String?,
    val lastSyncedAt: String?,
    val playersJson: String // Serialized JSON list of Player
)

@Dao
interface SeasonDao {
    @Query("SELECT * FROM seasons ORDER BY lastSyncedAt DESC")
    fun getAllSeasons(): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons WHERE id = :id LIMIT 1")
    suspend fun getSeasonById(id: String): SeasonEntity?

    @Query("SELECT * FROM seasons ORDER BY lastSyncedAt DESC LIMIT 1")
    fun getLatestSeason(): Flow<SeasonEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(season: SeasonEntity)

    @Query("DELETE FROM seasons WHERE id = :id")
    suspend fun deleteSeason(id: String)
}
