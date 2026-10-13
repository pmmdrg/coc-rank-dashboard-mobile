package com.cocrank.dashboard.data.repository

import com.cocrank.dashboard.data.LeagueIcons
import com.cocrank.dashboard.data.RankedTierMetadata
import com.cocrank.dashboard.data.api.CocApiConfig
import com.cocrank.dashboard.data.api.CocClanDetailResponse
import com.cocrank.dashboard.data.api.CocApiService
import com.cocrank.dashboard.data.api.CocGroupMember
import com.cocrank.dashboard.data.api.NetworkModule
import com.cocrank.dashboard.data.local.AppDatabase
import com.cocrank.dashboard.data.local.SeasonEntity
import com.cocrank.dashboard.data.local.UserPreferences
import com.cocrank.dashboard.domain.model.BattleLogEntry
import com.cocrank.dashboard.domain.model.ClanData
import com.cocrank.dashboard.domain.model.ClanMember
import com.cocrank.dashboard.domain.model.LeagueHistoryItem
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

data class SeasonDatePeriod(
    val startsAt: String,
    val endsAt: String,
    val displayPeriod: String
)

data class SyncResult(
    val currentSeason: Season,
    val previousSeason: Season?,
    val allSeasons: List<Season>,
    val clanData: ClanData?,
    val leagueHistory: List<LeagueHistoryItem>
)

class RankRepository(
    private val database: AppDatabase,
    val userPreferences: UserPreferences
) {
    private val gson = Gson()
    private val playerListType = object : TypeToken<List<Player>>() {}.type

    companion object {
        fun parseSeasonDateRange(seasonId: String?, fallbackStartsAt: String? = null): SeasonDatePeriod {
            if (seasonId.isNullOrBlank() && fallbackStartsAt.isNullOrBlank()) {
                return SeasonDatePeriod("", "", "--")
            }
            var startDate: Date? = null
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val vnFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            if (!fallbackStartsAt.isNullOrBlank()) {
                try {
                    startDate = isoFormat.parse(fallbackStartsAt)
                } catch (_: Exception) {}
            }

            if (startDate == null && !seasonId.isNullOrBlank()) {
                val num = seasonId.toLongOrNull()
                if (num != null && num > 1_000_000_000L) {
                    val refTime = 1789966800L
                    val cal = Calendar.getInstance().apply {
                        set(2026, Calendar.SEPTEMBER, 22, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val diffSec = num - refTime
                    val diffDays = (diffSec / 86400.0).roundToLong()
                    cal.add(Calendar.DAY_OF_YEAR, diffDays.toInt())
                    startDate = cal.time
                }
            }

            if (startDate == null) {
                return SeasonDatePeriod("", "", "--")
            }

            val endCal = Calendar.getInstance().apply {
                time = startDate
                add(Calendar.DAY_OF_YEAR, 6)
            }
            val endDate = endCal.time

            val startsAt = isoFormat.format(startDate)
            val endsAt = isoFormat.format(endDate)
            val displayPeriod = "${vnFormat.format(startDate)} - ${vnFormat.format(endDate)}"

            return SeasonDatePeriod(startsAt, endsAt, displayPeriod)
        }
    }

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

    private fun mapMembersToPlayers(
        rawMembers: List<CocGroupMember>,
        cleanTargetTag: String,
        maxAttacks: Int,
        maxDefenses: Int,
        attackBattleLogs: List<BattleLogEntry>,
        defenseBattleLogs: List<BattleLogEntry>
    ): List<Player> {
        val attackMap = attackBattleLogs.groupBy { it.opponentPlayerTag.trim().uppercase().removePrefix("#") }
        val defenseMap = defenseBattleLogs.groupBy { it.opponentPlayerTag.trim().uppercase().removePrefix("#") }

        val myAtkDest = if (attackBattleLogs.isNotEmpty()) {
            Math.round(attackBattleLogs.map { it.destructionPercentage }.average() * 10.0) / 10.0
        } else 0.0

        val myAtkStars = if (attackBattleLogs.isNotEmpty()) {
            Math.round(attackBattleLogs.map { it.stars.toDouble() }.average() * 10.0) / 10.0
        } else 0.0

        val myDefDest = if (defenseBattleLogs.isNotEmpty()) {
            Math.round(defenseBattleLogs.map { it.destructionPercentage }.average() * 10.0) / 10.0
        } else 0.0

        val myDefStars = if (defenseBattleLogs.isNotEmpty()) {
            Math.round(defenseBattleLogs.map { it.stars.toDouble() }.average() * 10.0) / 10.0
        } else 0.0

        return rawMembers.mapIndexed { index, m ->
            val mTag = m.memberTag
            val mName = m.memberName
            val cups = m.memberCups
            val cleanMemberTag = mTag.trim().uppercase().removePrefix("#")
            val isMe = cleanTargetTag.isNotBlank() && cleanMemberTag == cleanTargetTag

            val totalAttacks = m.attackWinCount + m.attackLoseCount
            val totalDefenses = m.defenseWinCount + m.defenseLoseCount

            val opponentAtkLogs = attackMap[cleanMemberTag] ?: emptyList()
            val opponentDefLogs = defenseMap[cleanMemberTag] ?: emptyList()

            val memberAtkDest = when {
                isMe -> myAtkDest
                opponentAtkLogs.isNotEmpty() -> Math.round(opponentAtkLogs.map { it.destructionPercentage }.average() * 10.0) / 10.0
                else -> 0.0
            }

            val memberDefDest = when {
                isMe -> myDefDest
                opponentDefLogs.isNotEmpty() -> Math.round(opponentDefLogs.map { it.destructionPercentage }.average() * 10.0) / 10.0
                else -> 0.0
            }

            Player(
                id = mTag,
                name = mName,
                rank = index + 1,
                playerTag = mTag,
                clanTag = m.memberClanTag,
                clanName = m.memberClanName,
                attacks = totalAttacks,
                defenses = totalDefenses,
                currentCups = cups,
                attackWinCount = m.attackWinCount,
                attackLoseCount = m.attackLoseCount,
                defenseWinCount = m.defenseWinCount,
                defenseLoseCount = m.defenseLoseCount,
                attackDestruction = memberAtkDest,
                defenseDestruction = memberDefDest,
                attackAvgStars = if (isMe && myAtkStars > 0) myAtkStars else null,
                defenseAvgStars = if (isMe && myDefStars > 0) myDefStars else null,
                isMe = isMe
            )
        }
    }

    suspend fun syncAllWithApi(
        overrideTag: String? = null,
        onProgress: (String) -> Unit = {}
    ): Result<SyncResult> {
        val tag = overrideTag?.trim() ?: userPreferences.playerTag.first().trim()
        if (tag.isBlank()) {
            return Result.failure(Exception("Vui lòng nhập mã Player Tag (#TAG) để đồng bộ"))
        }

        val customToken = userPreferences.apiToken.first()
        val customProxy = userPreferences.proxyUrl.first()
        val token = customToken.ifBlank { CocApiConfig.DEFAULT_API_TOKEN }
        val proxy = customProxy.ifBlank { CocApiConfig.DEFAULT_PROXY_URL }

        val service: CocApiService = NetworkModule.createService(proxy)
        val authHeader = if (token.isNotBlank()) "Bearer $token" else null
        val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
        val encodedTag = cleanTag.replace("#", "%23")
        val cleanTargetTag = cleanTag.trim().uppercase().removePrefix("#")

        onProgress("Đang kiểm tra thông tin người chơi $cleanTag...")

        return try {
            // 1. Hồ sơ người chơi
            val playerRes = service.getPlayer(encodedTag, authHeader)
            if (!playerRes.isSuccessful || playerRes.body() == null) {
                val msg = when (playerRes.code()) {
                    404 -> "Không tìm thấy người chơi với mã $cleanTag trên Supercell API."
                    403 -> "Lỗi xác thực hoặc IP bị hạn chế (Mã: 403). Vui lòng kiểm tra API Token hoặc thử lại."
                    502, 503, 520, 525 -> "Máy chủ CoC Proxy đang bảo trì hoặc gián đoạn kết nối. Vui lòng thử lại sau ít phút."
                    else -> "Lỗi khi gọi Supercell API (Mã lỗi: ${playerRes.code()})"
                }
                return Result.failure(Exception(msg))
            }
            val playerData = playerRes.body()!!
            val tierName = playerData.leagueTier?.name ?: "Legend III"
            val maxAttacks = RankedTierMetadata.getMaxAttacksForLeague(tierName)

            val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
            val syncedTime = dateFormat.format(Date())
            val leagueIconUrl = LeagueIcons.getLeagueIconUrl(
                tierName,
                playerData.leagueTier?.iconUrls?.medium
                    ?: playerData.leagueTier?.iconUrls?.large
                    ?: playerData.leagueTier?.iconUrls?.small
            )

            // 2. Lịch sử mùa giải
            onProgress("Đang tải lịch sử mùa giải...")
            val historyItems = try {
                val historyRes = service.getPlayerLeagueHistory(encodedTag, authHeader)
                if (historyRes.isSuccessful && historyRes.body() != null) {
                    historyRes.body()!!.items.map { item ->
                        val itemTierName = RankedTierMetadata.getTierName(item.leagueTierId)
                        val itemPeriod = parseSeasonDateRange(item.leagueSeasonId.toString())
                        LeagueHistoryItem(
                            leagueSeasonId = item.leagueSeasonId,
                            leagueTrophies = item.leagueTrophies,
                            leagueTierId = item.leagueTierId,
                            placement = item.placement,
                            attackWins = item.attackWins,
                            attackLosses = item.attackLosses,
                            defenseWins = item.defenseWins,
                            defenseLosses = item.defenseLosses,
                            tierName = itemTierName,
                            displayPeriod = itemPeriod.displayPeriod
                        )
                    }
                } else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            // 3. Mùa giải hiện tại
            val currentGroupTag = playerData.currentLeagueGroupTag ?: playerData.leagueGroup?.tag
            val currentSeasonId = playerData.currentLeagueSeasonId ?: playerData.leagueGroup?.seasonId

            var builtCurrentSeason: Season? = null
            if (!currentGroupTag.isNullOrBlank() && !currentSeasonId.isNullOrBlank()) {
                onProgress("Đang tải bảng đấu mùa hiện tại $currentGroupTag...")
                val encodedCurrentGroupTag = currentGroupTag.replace("#", "%23")
                try {
                    val groupRes = service.getLeagueGroup(
                        groupTag = encodedCurrentGroupTag,
                        seasonId = currentSeasonId,
                        playerTag = cleanTag,
                        authHeader = authHeader
                    )
                    if (groupRes.isSuccessful && groupRes.body() != null) {
                        val groupData = groupRes.body()!!
                        val attackBattleLogs = groupData.attackLogs.map {
                            BattleLogEntry(
                                opponentPlayerTag = it.opponentPlayerTag,
                                opponentName = it.opponentName,
                                stars = it.stars,
                                destructionPercentage = it.destructionPercentage,
                                trophies = it.trophies,
                                creationTime = it.creationTime
                            )
                        }
                        val defenseBattleLogs = groupData.defenseLogs.map {
                            BattleLogEntry(
                                opponentPlayerTag = it.opponentPlayerTag,
                                opponentName = it.opponentName,
                                stars = it.stars,
                                destructionPercentage = it.destructionPercentage,
                                trophies = it.trophies,
                                creationTime = it.creationTime
                            )
                        }
                        val currentPeriod = parseSeasonDateRange(currentSeasonId)
                        val members = mapMembersToPlayers(
                            groupData.members,
                            cleanTargetTag,
                            maxAttacks,
                            maxAttacks,
                            attackBattleLogs,
                            defenseBattleLogs
                        )
                        builtCurrentSeason = Season(
                            league = tierName,
                            leagueIconUrl = leagueIconUrl,
                            seasonName = "Mùa giải hiện tại (${currentPeriod.displayPeriod})",
                            startsAt = currentPeriod.startsAt,
                            endsAt = currentPeriod.endsAt,
                            maxAttacks = maxAttacks,
                            maxDefenses = maxAttacks,
                            promotionCount = 2,
                            demotionCount = 1,
                            myPlayerId = cleanTag,
                            players = members,
                            attackLogs = attackBattleLogs,
                            defenseLogs = defenseBattleLogs,
                            leagueGroupTag = currentGroupTag,
                            leagueSeasonId = currentSeasonId,
                            lastSyncedAt = syncedTime,
                            leagueHistory = historyItems
                        )
                    }
                } catch (_: Exception) {}
            }

            // 4. Mùa giải ngay trước đó
            val prevGroupTag = playerData.previousLeagueGroupTag
            val prevSeasonId = playerData.previousLeagueSeasonId

            var builtPreviousSeason: Season? = null
            if (!prevGroupTag.isNullOrBlank() && !prevSeasonId.isNullOrBlank()) {
                onProgress("Đang tải bảng đấu mùa trước $prevGroupTag...")
                val encodedPrevGroupTag = prevGroupTag.replace("#", "%23")
                try {
                    val prevRes = service.getLeagueGroup(
                        groupTag = encodedPrevGroupTag,
                        seasonId = prevSeasonId,
                        playerTag = cleanTag,
                        authHeader = authHeader
                    )
                    if (prevRes.isSuccessful && prevRes.body() != null) {
                        val prevData = prevRes.body()!!
                        val prevAttackLogs = prevData.attackLogs.map {
                            BattleLogEntry(
                                opponentPlayerTag = it.opponentPlayerTag,
                                opponentName = it.opponentName,
                                stars = it.stars,
                                destructionPercentage = it.destructionPercentage,
                                trophies = it.trophies,
                                creationTime = it.creationTime
                            )
                        }
                        val prevDefenseLogs = prevData.defenseLogs.map {
                            BattleLogEntry(
                                opponentPlayerTag = it.opponentPlayerTag,
                                opponentName = it.opponentName,
                                stars = it.stars,
                                destructionPercentage = it.destructionPercentage,
                                trophies = it.trophies,
                                creationTime = it.creationTime
                            )
                        }
                        val prevHistItem = historyItems.find { it.leagueSeasonId.toString() == prevSeasonId }
                        val prevTierName = prevHistItem?.tierName?.ifBlank { null } ?: tierName
                        val prevMaxAttacks = RankedTierMetadata.getMaxAttacksForLeague(prevTierName)
                        val prevPeriod = parseSeasonDateRange(prevSeasonId)

                        val prevMembers = mapMembersToPlayers(
                            prevData.members,
                            cleanTargetTag,
                            prevMaxAttacks,
                            prevMaxAttacks,
                            prevAttackLogs,
                            prevDefenseLogs
                        )
                        builtPreviousSeason = Season(
                            league = prevTierName,
                            leagueIconUrl = LeagueIcons.getLeagueIconUrl(prevTierName),
                            seasonName = "Mùa giải trước (${prevPeriod.displayPeriod})",
                            startsAt = prevPeriod.startsAt,
                            endsAt = prevPeriod.endsAt,
                            maxAttacks = prevMaxAttacks,
                            maxDefenses = prevMaxAttacks,
                            promotionCount = 2,
                            demotionCount = 1,
                            myPlayerId = cleanTag,
                            players = prevMembers,
                            attackLogs = prevAttackLogs,
                            defenseLogs = prevDefenseLogs,
                            leagueGroupTag = prevGroupTag,
                            leagueSeasonId = prevSeasonId,
                            lastSyncedAt = syncedTime,
                            leagueHistory = historyItems
                        )
                    }
                } catch (_: Exception) {}
            }

            // 5. Nếu không thuộc bảng đấu cụ thể nào, tạo người chơi dạng unranked
            val effectiveCurrentSeason = builtCurrentSeason ?: if (builtPreviousSeason != null) {
                builtPreviousSeason
            } else {
                val singlePlayer = Player(
                    id = playerData.tag,
                    name = playerData.name,
                    rank = 0,
                    playerTag = playerData.tag,
                    clanTag = playerData.clan?.tag,
                    clanName = playerData.clan?.name,
                    attacks = playerData.attackWins,
                    defenses = playerData.defenseWins,
                    currentCups = playerData.trophies,
                    isMe = true
                )
                Season(
                    league = tierName,
                    leagueIconUrl = leagueIconUrl,
                    seasonName = "${tierName} - Chưa vào bảng đấu",
                    startsAt = "",
                    endsAt = "",
                    maxAttacks = maxAttacks,
                    maxDefenses = maxAttacks,
                    promotionCount = 2,
                    demotionCount = 1,
                    myPlayerId = cleanTag,
                    players = listOf(singlePlayer),
                    leagueGroupTag = currentGroupTag,
                    leagueSeasonId = currentSeasonId,
                    lastSyncedAt = syncedTime,
                    leagueHistory = historyItems
                )
            }

            // 6. Đồng bộ thông tin Clan (nếu người chơi có tham gia Clan)
            var syncedClanData: ClanData? = null
            val clanTag = playerData.clan?.tag
            if (!clanTag.isNullOrBlank()) {
                onProgress("Đang tải dữ liệu Clan...")
                try {
                    val clanRes = getClanData(clanTag)
                    clanRes.onSuccess { syncedClanData = it }
                } catch (_: Exception) {}
            }

            val allSeasons = mutableListOf<Season>()
            allSeasons.add(RankingCalculator.normalizeSeason(effectiveCurrentSeason))
            if (builtPreviousSeason != null && builtPreviousSeason != effectiveCurrentSeason) {
                allSeasons.add(RankingCalculator.normalizeSeason(builtPreviousSeason))
            }

            // Lưu cache toàn bộ dữ liệu vào Room và DataStore
            saveSeason(allSeasons[0])
            if (syncedClanData != null) {
                userPreferences.saveClanDataJson(gson.toJson(syncedClanData))
            }
            if (historyItems.isNotEmpty()) {
                userPreferences.saveLeagueHistoryJson(gson.toJson(historyItems))
            }
            if (builtPreviousSeason != null) {
                userPreferences.savePreviousSeasonJson(gson.toJson(builtPreviousSeason))
            }

            Result.success(
                SyncResult(
                    currentSeason = allSeasons[0],
                    previousSeason = builtPreviousSeason,
                    allSeasons = allSeasons,
                    clanData = syncedClanData,
                    leagueHistory = if (historyItems.isNotEmpty()) historyItems else getSampleLeagueHistory()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithApi(
        overrideTag: String? = null,
        onProgress: (String) -> Unit = {}
    ): Result<Season> {
        return syncAllWithApi(overrideTag, onProgress).map { it.currentSeason }
    }

    suspend fun fetchClan(clanTag: String): Result<CocClanDetailResponse> {
        val customToken = userPreferences.apiToken.first()
        val customProxy = userPreferences.proxyUrl.first()
        val token = customToken.ifBlank { CocApiConfig.DEFAULT_API_TOKEN }
        val proxy = customProxy.ifBlank { CocApiConfig.DEFAULT_PROXY_URL }

        val service: CocApiService = NetworkModule.createService(proxy)
        val authHeader = if (token.isNotBlank()) "Bearer $token" else null
        val cleanTag = if (clanTag.startsWith("#")) clanTag else "#$clanTag"
        val encodedTag = cleanTag.replace("#", "%23")

        return try {
            val res = service.getClan(encodedTag, authHeader)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Không thể tải dữ liệu clan (${res.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClanData(clanTag: String): Result<ClanData> {
        val res = fetchClan(clanTag)
        return res.map { detail ->
            val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
            val members = detail.memberList.map { m ->
                ClanMember(
                    tag = m.tag,
                    name = m.name,
                    role = m.role,
                    townHallLevel = m.townHallLevel,
                    expLevel = m.expLevel,
                    clanRank = m.clanRank,
                    previousClanRank = m.previousClanRank,
                    trophies = m.trophies,
                    donations = m.donations,
                    donationsReceived = m.donationsReceived,
                    leagueIconUrl = m.leagueTier?.iconUrls?.medium ?: m.leagueTier?.iconUrls?.small
                )
            }
            ClanData(
                tag = detail.tag,
                name = detail.name,
                description = detail.description,
                clanLevel = detail.clanLevel,
                clanPoints = detail.clanPoints,
                members = detail.members,
                warLeagueName = detail.warLeague?.name,
                badgeUrl = detail.badgeUrls?.large ?: detail.badgeUrls?.medium ?: detail.badgeUrls?.small,
                memberList = members,
                lastSyncedAt = dateFormat.format(Date())
            )
        }
    }

    fun getSampleLeagueHistory(): List<com.cocrank.dashboard.domain.model.LeagueHistoryItem> = listOf(
        com.cocrank.dashboard.domain.model.LeagueHistoryItem(
            leagueSeasonId = 202609,
            leagueTrophies = 5820,
            leagueTierId = 105000034,
            placement = 14,
            attackWins = 22,
            attackLosses = 2,
            defenseWins = 18,
            defenseLosses = 6,
            tierName = "Legend III",
            displayPeriod = "Mùa 09/2026"
        ),
        com.cocrank.dashboard.domain.model.LeagueHistoryItem(
            leagueSeasonId = 202608,
            leagueTrophies = 5740,
            leagueTierId = 105000034,
            placement = 18,
            attackWins = 20,
            attackLosses = 4,
            defenseWins = 16,
            defenseLosses = 8,
            tierName = "Legend III",
            displayPeriod = "Mùa 08/2026"
        ),
        com.cocrank.dashboard.domain.model.LeagueHistoryItem(
            leagueSeasonId = 202607,
            leagueTrophies = 5650,
            leagueTierId = 105000033,
            placement = 8,
            attackWins = 17,
            attackLosses = 1,
            defenseWins = 15,
            defenseLosses = 3,
            tierName = "Electro 33",
            displayPeriod = "Mùa 07/2026"
        ),
        com.cocrank.dashboard.domain.model.LeagueHistoryItem(
            leagueSeasonId = 202606,
            leagueTrophies = 5490,
            leagueTierId = 105000032,
            placement = 12,
            attackWins = 16,
            attackLosses = 2,
            defenseWins = 14,
            defenseLosses = 4,
            tierName = "Electro 32",
            displayPeriod = "Mùa 06/2026"
        )
    )

    fun getSampleClanData(): ClanData = ClanData(
        tag = "#2Q0Y9L9G",
        name = "Golden Stars Vietnam",
        description = "Clan giao lưu học hỏi chiến thuật, tham gia giải đấu và War League hàng tháng. Đoàn kết và nhiệt huyết!",
        clanLevel = 22,
        clanPoints = 48920,
        members = 48,
        warLeagueName = "Champions League I",
        badgeUrl = "https://api-assets.clashofclans.com/badges/512/q14K7yC1P54_1l1bZ13uKqD43fO9m9o3K6e6e2J6B0.png",
        memberList = listOf(
            ClanMember(tag = "#90UQRP0", name = "Bạn (Hero)", role = "leader", townHallLevel = 17, expLevel = 240, clanRank = 1, previousClanRank = 1, trophies = 5850, donations = 4500, donationsReceived = 1200),
            ClanMember(tag = "#M222222", name = "Arthur Pendragon", role = "coLeader", townHallLevel = 17, expLevel = 235, clanRank = 2, previousClanRank = 2, trophies = 5720, donations = 3800, donationsReceived = 2100),
            ClanMember(tag = "#M333333", name = "Shadow Assassin", role = "admin", townHallLevel = 16, expLevel = 210, clanRank = 3, previousClanRank = 4, trophies = 5480, donations = 2900, donationsReceived = 1800),
            ClanMember(tag = "#M444444", name = "Valkyrie Storm", role = "member", townHallLevel = 16, expLevel = 195, clanRank = 4, previousClanRank = 3, trophies = 5320, donations = 1500, donationsReceived = 1600),
            ClanMember(tag = "#M555555", name = "Dragon Rider", role = "member", townHallLevel = 15, expLevel = 180, clanRank = 5, previousClanRank = 5, trophies = 5150, donations = 950, donationsReceived = 1400)
        ),
        lastSyncedAt = "Vừa khởi tạo (Mẫu)"
    )

    /**
     * Dữ liệu mẫu hoàn chỉnh khi người dùng vừa mở app
     */
    fun getSampleSeason(): Season {
        val myTag = "#90UQRP0"
        val samplePlayers = listOf(
            Player(id = "p-1", name = "Chief Arthur", currentCups = 1420, attacks = 24, defenses = 22, playerTag = "#P111111", clanName = "Vietnam Legends", rating = RatingCategory.DOMINANT),
            Player(id = "p-2", name = "Dragon Slayer", currentCups = 1240, attacks = 22, defenses = 20, playerTag = "#P222222", clanName = "Elite Warriors", rating = RatingCategory.SUPERIOR),
            Player(id = myTag, name = "Bạn (Hero)", currentCups = 1200, attacks = 18, defenses = 16, attackDestruction = 99.5, defenseDestruction = 92.2, attackAvgStars = 2.8, defenseAvgStars = 2.7, playerTag = myTag, clanName = "Golden Stars", isMe = true, rating = RatingCategory.SUPERIOR),
            Player(id = "p-4", name = "Ghost Hunter", currentCups = 1160, attacks = 20, defenses = 24, playerTag = "#P444444", clanName = "Saigon Club", rating = RatingCategory.SUPERIOR),
            Player(id = "p-5", name = "Dark Knight", currentCups = 1040, attacks = 14, defenses = 12, playerTag = "#P555555", clanName = "Hanoi Clan", rating = RatingCategory.POTENTIAL),
            Player(id = "p-6", name = "Valkyrie Queen", currentCups = 980, attacks = 15, defenses = 18, playerTag = "#P666666", clanName = "Thunderbolt", rating = RatingCategory.POTENTIAL),
            Player(id = "p-7", name = "P.E.K.K.A Boy", currentCups = 850, attacks = 10, defenses = 8, playerTag = "#P777777", clanName = "Royal Aces", rating = RatingCategory.ALARM),
            Player(id = "p-8", name = "Barbarian King", currentCups = 650, attacks = 6, defenses = 6, playerTag = "#P888888", clanName = "Novice Clan", rating = RatingCategory.ALARM)
        )

        val season = Season(
            league = "Legend 3",
            leagueIconUrl = com.cocrank.dashboard.data.LeagueIcons.getLeagueIconUrl("Legend 3"),
            seasonName = "Mùa giải Legend III",
            maxAttacks = 24,
            maxDefenses = 24,
            promotionCount = 2,
            demotionCount = 1,
            myPlayerId = myTag,
            players = samplePlayers,
            lastSyncedAt = "Vừa khởi tạo (Mẫu)",
            leagueHistory = getSampleLeagueHistory()
        )

        return RankingCalculator.normalizeSeason(season)
    }
}
