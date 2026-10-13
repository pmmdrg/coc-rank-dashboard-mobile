package com.cocrank.dashboard.data.repository

import com.cocrank.dashboard.data.RankedTierMetadata
import com.cocrank.dashboard.data.api.CocApiService
import com.cocrank.dashboard.data.api.NetworkModule
import com.cocrank.dashboard.data.local.AppDatabase
import com.cocrank.dashboard.data.local.SeasonEntity
import com.cocrank.dashboard.data.local.UserPreferences
import com.cocrank.dashboard.domain.model.ClanData
import com.cocrank.dashboard.domain.model.ClanMember
import com.cocrank.dashboard.domain.model.Player
import com.cocrank.dashboard.domain.model.RankChangesSnapshot
import com.cocrank.dashboard.domain.model.RatingCategory
import com.cocrank.dashboard.domain.model.Season
import com.cocrank.dashboard.domain.usecase.RankingCalculator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RankRepository(
    private val database: AppDatabase,
    val userPreferences: UserPreferences
) {
    private val gson = Gson()
    private val playerListType = object : TypeToken<List<Player>>() {}.type

    val latestSeason: Flow<Season?> = database.seasonDao().getLatestSeason().map { entity ->
        if (entity != null) {
            val players: List<Player> = try {
                gson.fromJson(entity.playersJson, playerListType)
            } catch (e: Exception) {
                emptyList()
            }
            Season(
                league = entity.league,
                leagueIconUrl = entity.leagueIconUrl,
                seasonName = entity.seasonName,
                startsAt = entity.startsAt,
                endsAt = entity.endsAt,
                maxAttacks = entity.maxAttacks,
                maxDefenses = entity.maxDefenses,
                promotionCount = entity.promotionCount,
                demotionCount = entity.demotionCount,
                myPlayerId = entity.myPlayerId,
                players = players,
                leagueGroupTag = entity.leagueGroupTag,
                leagueSeasonId = entity.leagueSeasonId,
                lastSyncedAt = entity.lastSyncedAt
            )
        } else {
            getSampleSeason()
        }
    }

    suspend fun saveSeason(season: Season) {
        val normalized = RankingCalculator.normalizeSeason(season)
        val entity = SeasonEntity(
            id = normalized.seasonName.ifBlank { "season_active" },
            seasonName = normalized.seasonName,
            league = normalized.league,
            leagueIconUrl = normalized.leagueIconUrl,
            startsAt = normalized.startsAt,
            endsAt = normalized.endsAt,
            maxAttacks = normalized.maxAttacks,
            maxDefenses = normalized.maxDefenses,
            promotionCount = normalized.promotionCount,
            demotionCount = normalized.demotionCount,
            myPlayerId = normalized.myPlayerId,
            leagueGroupTag = normalized.leagueGroupTag,
            leagueSeasonId = normalized.leagueSeasonId,
            lastSyncedAt = normalized.lastSyncedAt ?: SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date()),
            playersJson = gson.toJson(normalized.players)
        )
        database.seasonDao().insertOrUpdate(entity)
    }

    suspend fun syncWithApi(
        overrideTag: String? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Season> {
        val tag = overrideTag ?: userPreferences.playerTag.first()
        if (tag.isBlank()) {
            return Result.failure(Exception("Vui lòng nhập mã Player Tag (#TAG) để đồng bộ"))
        }

        val customToken = userPreferences.apiToken.first()
        val customProxy = userPreferences.proxyUrl.first()
        val token = customToken.ifBlank { com.cocrank.dashboard.data.api.CocApiConfig.DEFAULT_API_TOKEN }
        val proxy = customProxy.ifBlank { com.cocrank.dashboard.data.api.CocApiConfig.DEFAULT_PROXY_URL }

        val service: CocApiService = NetworkModule.createService(proxy)
        val authHeader = if (token.isNotBlank()) "Bearer $token" else null
        val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
        val encodedTag = cleanTag.replace("#", "%23")

        onProgress("Đang kiểm tra thông tin người chơi $cleanTag...")

        return try {
            val playerRes = service.getPlayer(encodedTag, authHeader)
            if (!playerRes.isSuccessful || playerRes.body() == null) {
                val msg = when (playerRes.code()) {
                    404 -> "Không tìm thấy người chơi với mã $cleanTag trên Supercell API."
                    403 -> "Lỗi xác thực hoặc IP bị hạn chế (Mã: 403). Vui lòng thử lại sau ít phút."
                    502, 503, 520, 525 -> "Máy chủ CoC Proxy đang bảo trì hoặc gián đoạn. Vui lòng thử lại."
                    else -> "Lỗi khi đồng bộ dữ liệu CoC API (Mã lỗi: ${playerRes.code()})"
                }
                return Result.failure(Exception(msg))
            }
            val playerData = playerRes.body()!!
            val tierName = playerData.leagueTier?.name ?: "Legend III"
            val maxAttacks = RankedTierMetadata.getMaxAttacksForLeague(tierName)

            // Lấy dữ liệu bảng giải đấu (League Group) nếu có
            val groupSummary = playerData.leagueGroup
            val members = mutableListOf<Player>()

            if (groupSummary != null && groupSummary.tag.isNotBlank() && groupSummary.seasonId != null) {
                onProgress("Đang tải bảng đấu giải ${groupSummary.tag}...")
                val encodedGroupTag = groupSummary.tag.replace("#", "%23")
                val groupRes = service.getLeagueSeasonGroup(
                    leagueId = (playerData.leagueTier?.id ?: 105000034).toString(),
                    seasonId = groupSummary.seasonId,
                    groupTag = encodedGroupTag,
                    authHeader = authHeader
                )
                if (groupRes.isSuccessful && groupRes.body() != null) {
                    val groupData = groupRes.body()!!
                    groupData.members.forEach { m ->
                        val totalAttacks = m.attackWinCount + m.attackLoseCount
                        val totalDefenses = m.defenseWinCount + m.defenseLoseCount
                        members.add(
                            Player(
                                id = m.tag,
                                name = m.name,
                                playerTag = m.tag,
                                clanTag = m.clan?.tag,
                                clanName = m.clan?.name,
                                attacks = totalAttacks,
                                defenses = totalDefenses,
                                currentCups = m.trophies,
                                attackWinCount = m.attackWinCount,
                                attackLoseCount = m.attackLoseCount,
                                defenseWinCount = m.defenseWinCount,
                                defenseLoseCount = m.defenseLoseCount,
                                isMe = m.tag.equals(cleanTag, ignoreCase = true)
                            )
                        )
                    }
                }
            }

            // Nếu không thuộc bảng đấu cụ thể, tạo danh sách từ dữ liệu người chơi + các đối thủ giả lập tiêu biểu
            if (members.isEmpty()) {
                members.add(
                    Player(
                        id = playerData.tag,
                        name = playerData.name,
                        playerTag = playerData.tag,
                        clanTag = playerData.clan?.tag,
                        clanName = playerData.clan?.name,
                        attacks = playerData.attackWins,
                        defenses = playerData.defenseWins,
                        currentCups = playerData.trophies,
                        isMe = true
                    )
                )
            }

            val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
            val syncedTime = dateFormat.format(Date())

            val newSeason = Season(
                league = tierName,
                leagueIconUrl = playerData.leagueTier?.iconUrls?.medium,
                seasonName = "${tierName} - ${SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())}",
                startsAt = "",
                endsAt = "",
                maxAttacks = maxAttacks,
                maxDefenses = maxAttacks,
                promotionCount = 2,
                demotionCount = 1,
                myPlayerId = cleanTag,
                players = members,
                leagueGroupTag = groupSummary?.tag,
                leagueSeasonId = groupSummary?.seasonId,
                lastSyncedAt = syncedTime
            )

            val normalized = RankingCalculator.normalizeSeason(newSeason)
            saveSeason(normalized)
            Result.success(normalized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dữ liệu mẫu hoàn chỉnh khi người dùng vừa mở app
     */
    fun getSampleSeason(): Season {
        val myTag = "#90UQRP0"
        val samplePlayers = listOf(
            Player(id = "p-1", name = "Chief Arthur", currentCups = 1240, attacks = 24, defenses = 22, playerTag = "#P111111", clanName = "Vietnam Legends", rating = RatingCategory.DOMINANT),
            Player(id = "p-2", name = "Dragon Slayer", currentCups = 1180, attacks = 22, defenses = 20, playerTag = "#P222222", clanName = "Elite Warriors", rating = RatingCategory.SUPERIOR),
            Player(id = myTag, name = "Bạn (Hero)", currentCups = 1120, attacks = 18, defenses = 16, playerTag = myTag, clanName = "Golden Stars", isMe = true, rating = RatingCategory.SUPERIOR),
            Player(id = "p-4", name = "Ghost Hunter", currentCups = 1040, attacks = 20, defenses = 24, playerTag = "#P444444", clanName = "Saigon Club", rating = RatingCategory.SUPERIOR),
            Player(id = "p-5", name = "Dark Knight", currentCups = 960, attacks = 14, defenses = 12, playerTag = "#P555555", clanName = "Hanoi Clan", rating = RatingCategory.POTENTIAL),
            Player(id = "p-6", name = "Valkyrie Queen", currentCups = 890, attacks = 15, defenses = 18, playerTag = "#P666666", clanName = "Thunderbolt", rating = RatingCategory.POTENTIAL),
            Player(id = "p-7", name = "P.E.K.K.A Boy", currentCups = 780, attacks = 10, defenses = 8, playerTag = "#P777777", clanName = "Royal Aces", rating = RatingCategory.ALARM),
            Player(id = "p-8", name = "Barbarian King", currentCups = 650, attacks = 6, defenses = 6, playerTag = "#P888888", clanName = "Novice Clan", rating = RatingCategory.ALARM)
        )

        val season = Season(
            league = "Legend 3",
            seasonName = "Mùa giải Legend III",
            maxAttacks = 24,
            maxDefenses = 24,
            promotionCount = 2,
            demotionCount = 1,
            myPlayerId = myTag,
            players = samplePlayers,
            lastSyncedAt = "Vừa khởi tạo (Mẫu)"
        )

        return RankingCalculator.normalizeSeason(season)
    }
}
