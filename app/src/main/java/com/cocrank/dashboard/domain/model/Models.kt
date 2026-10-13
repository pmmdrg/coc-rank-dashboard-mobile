package com.cocrank.dashboard.domain.model

enum class RatingCategory(val displayName: String, val colorHex: Long) {
    DOMINANT("Áp đảo", 0xFF8B5CF6),     // >= 60% cup tối đa ban đầu
    SUPERIOR("Vượt trội", 0xFF10B981),  // 55% đến < 60% cup tối đa ban đầu
    POTENTIAL("Tiềm năng", 0xFF0284C7), // 50% đến < 55% cup tối đa ban đầu
    ALARM("Báo động", 0xFFE11D48);      // < 50% cup tối đa ban đầu

    companion object {
        fun fromCups(cups: Int, initialMaxCups: Int): RatingCategory {
            if (initialMaxCups <= 0) return ALARM
            val ratio = cups.toDouble() / initialMaxCups
            return when {
                ratio >= 0.60 -> DOMINANT
                ratio >= 0.55 -> SUPERIOR
                ratio >= 0.50 -> POTENTIAL
                else -> ALARM
            }
        }

        fun fromCups(cups: Int, maxAttacks: Int = 24, maxDefenses: Int = maxAttacks): RatingCategory {
            val safeAtk = if (maxAttacks > 0) maxAttacks else 24
            val safeDef = if (maxDefenses > 0) maxDefenses else safeAtk
            val initialMaxCups = (safeAtk + safeDef) * 40
            return fromCups(cups, initialMaxCups)
        }

        fun fromString(str: String?): RatingCategory = when (str?.lowercase()) {
            "dominant" -> DOMINANT
            "superior" -> SUPERIOR
            "potential" -> POTENTIAL
            else -> ALARM
        }
    }
}

enum class AttackStatusCategory(val displayName: String, val colorHex: Long) {
    FINISHED("Đã đánh xong", 0xFF10B981),
    IN_PROGRESS("Chưa đánh xong", 0xFFF59E0B),
    NOT_STARTED("Chưa đánh lượt nào", 0xFF64748B);

    companion object {
        fun fromAttacks(attacks: Int, maxAttacks: Int): AttackStatusCategory = when {
            attacks <= 0 -> NOT_STARTED
            attacks >= maxAttacks -> FINISHED
            else -> IN_PROGRESS
        }
    }
}

data class BattleLogEntry(
    val opponentPlayerTag: String,
    val opponentName: String? = null,
    val stars: Int = 0,
    val destructionPercentage: Double = 0.0,
    val trophies: Int = 0,
    val creationTime: String? = null
)

data class Player(
    val id: String,
    val name: String,
    val rank: Int = 0,
    val prevRank: Int? = null,
    val rankDiff: Int? = null,
    val attacks: Int = 0,
    val defenses: Int = 0,
    val attackDestruction: Double = 0.0,
    val defenseDestruction: Double = 0.0,
    val currentCups: Int = 0,
    val maxPossibleCups: Int = 0,
    val rating: RatingCategory = RatingCategory.ALARM,
    val attackCups: Int = 0,
    val defenseCups: Int = 0,
    val playerTag: String? = null,
    val clanTag: String? = null,
    val clanName: String? = null,
    val attackWinCount: Int? = null,
    val attackLoseCount: Int? = null,
    val defenseWinCount: Int? = null,
    val defenseLoseCount: Int? = null,
    val attackAvgStars: Double? = null,
    val defenseAvgStars: Double? = null,
    val isMe: Boolean = false,
    val attackedByMe: List<BattleLogEntry> = emptyList(),
    val defendedAgainstMe: List<BattleLogEntry> = emptyList()
)

data class Season(
    val league: String = "Legend 3",
    val leagueIconUrl: String? = null,
    val seasonName: String = "Current Season",
    val startsAt: String = "",
    val endsAt: String = "",
    val maxAttacks: Int = 24,
    val maxDefenses: Int = 24,
    val promotionCount: Int = 5,
    val demotionCount: Int = 15,
    val myPlayerId: String = "",
    val players: List<Player> = emptyList(),
    val attackLogs: List<BattleLogEntry> = emptyList(),
    val defenseLogs: List<BattleLogEntry> = emptyList(),
    val leagueGroupTag: String? = null,
    val leagueSeasonId: String? = null,
    val lastSyncedAt: String? = null,
    val leagueHistory: List<LeagueHistoryItem> = emptyList()
)

data class RankingStats(
    val myPlayer: Player? = null,
    val playersWhoCanPassMe: Int = 0,
    val playersDefinitelyBelowMe: Int = 0,
    val lowestPossibleRank: Int = 0,
    val ratingCounts: Map<RatingCategory, Int> = emptyMap(),
    val attackStatusCounts: Map<AttackStatusCategory, Int> = emptyMap()
)

data class RankChangeItem(
    val id: String,
    val name: String,
    val playerTag: String? = null,
    val oldRank: Int,
    val newRank: Int,
    val rankDiff: Int,
    val oldCups: Int,
    val newCups: Int,
    val cupsDiff: Int,
    val oldAttacks: Int = 0,
    val newAttacks: Int = 0,
    val attacksDiff: Int = 0,
    val oldDefenses: Int = 0,
    val newDefenses: Int = 0,
    val defensesDiff: Int = 0,
    val isMe: Boolean = false
)

data class RankChangesSnapshot(
    val seasonId: String? = null,
    val groupTag: String? = null,
    val updatedAt: String = "",
    val previousUpdatedAt: String? = null,
    val changes: List<RankChangeItem> = emptyList()
)

data class ClanMember(
    val tag: String,
    val name: String,
    val role: String,
    val townHallLevel: Int,
    val expLevel: Int,
    val clanRank: Int,
    val previousClanRank: Int,
    val trophies: Int,
    val donations: Int,
    val donationsReceived: Int,
    val leagueIconUrl: String? = null
)

data class ClanData(
    val tag: String,
    val name: String,
    val description: String? = null,
    val clanLevel: Int = 0,
    val clanPoints: Int = 0,
    val members: Int = 0,
    val warLeagueName: String? = null,
    val badgeUrl: String? = null,
    val memberList: List<ClanMember> = emptyList(),
    val lastSyncedAt: String? = null
)

data class LeagueHistoryItem(
    val leagueSeasonId: Long = 0,
    val leagueTrophies: Int = 0,
    val leagueTierId: Long = 0,
    val placement: Int = 0,
    val attackWins: Int = 0,
    val attackLosses: Int = 0,
    val defenseWins: Int = 0,
    val defenseLosses: Int = 0,
    val tierName: String = "",
    val displayPeriod: String = ""
)
