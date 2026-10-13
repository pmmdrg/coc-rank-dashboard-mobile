package com.cocrank.dashboard.domain.usecase

import com.cocrank.dashboard.data.RankedTierMetadata
import com.cocrank.dashboard.domain.model.AttackStatusCategory
import com.cocrank.dashboard.domain.model.LeagueHistoryItem
import com.cocrank.dashboard.domain.model.Player
import com.cocrank.dashboard.domain.model.RankChangeItem
import com.cocrank.dashboard.domain.model.RankChangesSnapshot
import com.cocrank.dashboard.domain.model.RankingStats
import com.cocrank.dashboard.domain.model.RatingCategory
import com.cocrank.dashboard.domain.model.Season
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object RankingCalculator {

    /**
     * Tính cúp tối đa có thể đạt:
     * Mỗi trận công còn lại tối đa +40 cúp
     * Mỗi trận thủ còn lại (giả định thủ hoàn hảo) tối đa +40 cúp
     */
    fun calculateMaxPossibleCups(
        currentCups: Int,
        attacks: Int,
        defenses: Int = 0,
        maxAttacks: Int = 24,
        maxDefenses: Int = 24
    ): Int {
        val remainingAttacks = max(0, maxAttacks - max(0, attacks))
        val remainingDefenses = max(0, maxDefenses - max(0, defenses))
        return max(0, currentCups) + remainingAttacks * 40 + remainingDefenses * 40
    }

    /**
     * Phân loại đánh giá kỹ năng/mức cúp theo % so với cúp tối đa ban đầu x:
     * - Áp đảo (Dominant): >= 70% x
     * - Vượt trội (Superior): 60% đến < 70% x
     * - Tiềm năng (Potential): 50% đến < 60% x
     * - Báo động (Alarm): < 50% x
     */
    fun calculatePlayerRating(currentCups: Int, initialMaxCups: Int): RatingCategory {
        return RatingCategory.fromCups(currentCups, initialMaxCups)
    }

    fun calculatePlayerRating(
        currentCups: Int,
        maxAttacks: Int = 24,
        maxDefenses: Int = maxAttacks
    ): RatingCategory {
        return RatingCategory.fromCups(currentCups, maxAttacks, maxDefenses)
    }

    /**
     * Sắp xếp và đánh thứ hạng cho danh sách người chơi
     */
    fun sortAndRankPlayers(players: List<Player>): List<Player> {
        val sorted = players.sortedWith(
            compareByDescending<Player> { it.currentCups }
                .thenByDescending { it.attackWinCount ?: 0 }
                .thenBy { it.attacks }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id }
        )

        return sorted.mapIndexed { index, player ->
            val rank = index + 1
            val prevRank = player.prevRank
            val rankDiff = if (prevRank != null && prevRank > 0) prevRank - rank else null
            player.copy(
                rank = rank,
                prevRank = prevRank,
                rankDiff = rankDiff
            )
        }
    }

    /**
     * Chuẩn hóa toàn bộ mùa giải và tính toán các chỉ số phái sinh
     */
    fun normalizeSeason(season: Season): Season {
        val maxAttacks = season.maxAttacks.takeIf { it > 0 } ?: 24
        val maxDefenses = season.maxDefenses.takeIf { it > 0 } ?: maxAttacks
        val initialMaxCups = (maxAttacks + maxDefenses) * 40
        val myCleanTag = season.myPlayerId.trim().uppercase().removePrefix("#")

        val updatedPlayers = season.players.mapIndexed { idx, p ->
            val attacks = min(maxAttacks, max(0, p.attacks))
            val defenses = min(maxDefenses, max(0, p.defenses))
            val currentCups = max(0, p.currentCups)
            val maxCups = calculateMaxPossibleCups(
                currentCups = currentCups,
                attacks = attacks,
                defenses = defenses,
                maxAttacks = maxAttacks,
                maxDefenses = maxDefenses
            )
            val rating = calculatePlayerRating(currentCups, initialMaxCups)
            val pTag = (p.playerTag ?: p.id).trim().uppercase().removePrefix("#")
            val isMe = myCleanTag.isNotBlank() && (pTag == myCleanTag || p.id.uppercase().removePrefix("#") == myCleanTag)

            p.copy(
                id = p.id.ifBlank { "p-${idx + 1}" },
                name = p.name.ifBlank { "Player ${idx + 1}" },
                attacks = attacks,
                defenses = defenses,
                currentCups = currentCups,
                maxPossibleCups = maxCups,
                rating = rating,
                attackCups = currentCups,
                isMe = isMe
            )
        }

        val rankedPlayers = if (updatedPlayers.size <= 1) {
            // Nếu chỉ có 1 người chơi (chưa nạp được bảng đấu đầy đủ), giữ nguyên unranked (rank = 0)
            updatedPlayers.map { it.copy(rank = 0) }
        } else {
            sortAndRankPlayers(updatedPlayers)
        }
        val myPlayer = rankedPlayers.firstOrNull { it.isMe }

        return season.copy(
            maxAttacks = maxAttacks,
            maxDefenses = maxDefenses,
            myPlayerId = season.myPlayerId.ifBlank { myPlayer?.playerTag ?: "" },
            players = rankedPlayers
        )
    }

    /**
     * Tính toán tổng hợp số liệu xếp hạng (RankingStats)
     */
    fun getRankingStats(season: Season, myPlayerTagOverride: String? = null): RankingStats {
        val myCleanTag = (myPlayerTagOverride?.takeIf { it.isNotBlank() } ?: season.myPlayerId)
            .trim().uppercase().removePrefix("#")
        val seasonWithTag = if (myCleanTag.isNotBlank() && season.myPlayerId != myCleanTag) {
            season.copy(myPlayerId = myCleanTag)
        } else season
        val normalized = normalizeSeason(seasonWithTag)

        val myPlayer = normalized.players.firstOrNull {
            val pTag = (it.playerTag ?: it.id).trim().uppercase().removePrefix("#")
            myCleanTag.isNotBlank() && (pTag == myCleanTag || it.id.trim().uppercase().removePrefix("#") == myCleanTag)
        } ?: normalized.players.firstOrNull { it.isMe }

        val ratingCounts = mutableMapOf<RatingCategory, Int>().apply {
            RatingCategory.entries.forEach { this[it] = 0 }
            normalized.players.forEach { this[it.rating] = (this[it.rating] ?: 0) + 1 }
        }

        val attackStatusCounts = mutableMapOf<AttackStatusCategory, Int>().apply {
            AttackStatusCategory.entries.forEach { this[it] = 0 }
            normalized.players.forEach {
                val status = AttackStatusCategory.fromAttacks(it.attacks, normalized.maxAttacks)
                this[status] = (this[status] ?: 0) + 1
            }
        }

        val canPassMe = if (myPlayer != null) {
            normalized.players.count { it.id != myPlayer.id && it.maxPossibleCups > myPlayer.maxPossibleCups }
        } else 0

        val definitelyBelowMe = if (myPlayer != null) {
            normalized.players.count { it.id != myPlayer.id && it.maxPossibleCups <= myPlayer.maxPossibleCups }
        } else normalized.players.size

        val lowestRank = if (myPlayer != null) canPassMe + 1 else 0

        return RankingStats(
            myPlayer = myPlayer,
            playersWhoCanPassMe = canPassMe,
            playersDefinitelyBelowMe = definitelyBelowMe,
            lowestPossibleRank = lowestRank,
            ratingCounts = ratingCounts,
            attackStatusCounts = attackStatusCounts
        )
    }

    /**
     * Tính toán bảng biến động rank so sánh giữa 2 lần đồng bộ
     */
    fun calculateRankChanges(
        currentSeason: Season,
        previousPlayers: List<Player>,
        myPlayerTag: String,
        previousUpdatedAt: String? = null
    ): RankChangesSnapshot {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val updatedAt = currentSeason.lastSyncedAt ?: dateFormat.format(Date())
        val cleanTag = myPlayerTag.trim().uppercase().removePrefix("#")

        if (previousPlayers.isEmpty() || previousPlayers.none { it.rank > 0 }) {
            return RankChangesSnapshot(
                seasonId = currentSeason.leagueSeasonId,
                groupTag = currentSeason.leagueGroupTag,
                updatedAt = updatedAt,
                previousUpdatedAt = null,
                changes = emptyList()
            )
        }

        val oldMap = previousPlayers.associateBy { (it.playerTag ?: it.id).trim().uppercase().removePrefix("#") }
        val changes = mutableListOf<RankChangeItem>()

        for (p in currentSeason.players) {
            val tag = (p.playerTag ?: p.id).trim().uppercase().removePrefix("#")
            val oldP = oldMap[tag]

            if (oldP != null && oldP.rank > 0) {
                val isMe = cleanTag.isNotBlank() && tag == cleanTag
                val rankDiff = oldP.rank - p.rank
                val cupsDiff = p.currentCups - oldP.currentCups
                val atkDiff = p.attacks - oldP.attacks
                val defDiff = p.defenses - oldP.defenses

                val hasRankChange = rankDiff != 0
                val hasBattleChange = cupsDiff != 0 || atkDiff != 0 || defDiff != 0

                if (hasRankChange || (isMe && hasBattleChange)) {
                    changes.add(
                        RankChangeItem(
                            id = p.id,
                            name = p.name,
                            playerTag = p.playerTag,
                            oldRank = oldP.rank,
                            newRank = p.rank,
                            rankDiff = rankDiff,
                            oldCups = oldP.currentCups,
                            newCups = p.currentCups,
                            cupsDiff = cupsDiff,
                            oldAttacks = oldP.attacks,
                            newAttacks = p.attacks,
                            attacksDiff = atkDiff,
                            oldDefenses = oldP.defenses,
                            newDefenses = p.defenses,
                            defensesDiff = defDiff,
                            isMe = isMe
                        )
                    )
                }
            }
        }

        changes.sortWith(
            compareByDescending<RankChangeItem> { it.isMe }
                .thenByDescending { abs(it.rankDiff) }
        )

        return RankChangesSnapshot(
            seasonId = currentSeason.leagueSeasonId,
            groupTag = currentSeason.leagueGroupTag,
            updatedAt = updatedAt,
            previousUpdatedAt = previousUpdatedAt,
            changes = changes
        )
    }

    /**
     * Kỷ lục thứ hạng: Ưu tiên giải đấu cấp cao hơn trước (tierNumber cao hơn),
     * sau đó mới đến thứ hạng (#1 tốt hơn #80).
     */
    fun findBestRankSeason(history: List<LeagueHistoryItem>): LeagueHistoryItem? {
        val validSeasons = history.filter { it.placement > 0 }
        return validSeasons.reduceOrNull { best, cur ->
            val curTier = RankedTierMetadata.getTierNumber(cur.leagueTierId, cur.tierName)
            val bestTier = RankedTierMetadata.getTierNumber(best.leagueTierId, best.tierName)
            when {
                curTier > bestTier -> cur
                curTier < bestTier -> best
                cur.placement < best.placement -> cur
                cur.placement > best.placement -> best
                cur.leagueTrophies >= best.leagueTrophies -> cur
                else -> best
            }
        } ?: history.firstOrNull()
    }

    /**
     * Kỷ lục cúp: Ưu tiên giải đấu cấp cao hơn trước (tierNumber cao hơn),
     * sau đó mới đến số cúp (nhiều hơn tốt hơn).
     */
    fun findBestCupsSeason(history: List<LeagueHistoryItem>): LeagueHistoryItem? {
        if (history.isEmpty()) return null
        return history.reduce { best, cur ->
            val curTier = RankedTierMetadata.getTierNumber(cur.leagueTierId, cur.tierName)
            val bestTier = RankedTierMetadata.getTierNumber(best.leagueTierId, best.tierName)
            when {
                curTier > bestTier -> cur
                curTier < bestTier -> best
                cur.leagueTrophies > best.leagueTrophies -> cur
                cur.leagueTrophies < best.leagueTrophies -> best
                cur.placement <= best.placement -> cur
                else -> best
            }
        }
    }
}
