package com.cocrank.dashboard

import com.cocrank.dashboard.domain.model.Player
import com.cocrank.dashboard.domain.model.RatingCategory
import com.cocrank.dashboard.domain.model.Season
import com.cocrank.dashboard.domain.usecase.RankingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingCalculatorTest {

    @Test
    fun testCalculateMaxPossibleCups() {
        // Current cups 1000, 20 attacks done out of 24 -> 4 attacks left (4 * 40 = 160)
        // 18 defenses done out of 24 -> 6 defenses left (6 * 40 = 240)
        // Total expected: 1000 + 160 + 240 = 1400
        val maxCups = RankingCalculator.calculateMaxPossibleCups(
            currentCups = 1000,
            attacks = 20,
            defenses = 18,
            maxAttacks = 24,
            maxDefenses = 24
        )
        assertEquals(1400, maxCups)
    }

    @Test
    fun testCalculateRating() {
        // Mặc định: 24 lượt công + 24 lượt thủ -> x = 1920 cúp
        // 60% x = 1152, 55% x = 1056, 50% x = 960
        assertEquals(RatingCategory.DOMINANT, RankingCalculator.calculatePlayerRating(1200))
        assertEquals(RatingCategory.DOMINANT, RankingCalculator.calculatePlayerRating(1152))
        assertEquals(RatingCategory.SUPERIOR, RankingCalculator.calculatePlayerRating(1151))
        assertEquals(RatingCategory.SUPERIOR, RankingCalculator.calculatePlayerRating(1056))
        assertEquals(RatingCategory.POTENTIAL, RankingCalculator.calculatePlayerRating(1055))
        assertEquals(RatingCategory.POTENTIAL, RankingCalculator.calculatePlayerRating(960))
        assertEquals(RatingCategory.ALARM, RankingCalculator.calculatePlayerRating(959))
        assertEquals(RatingCategory.ALARM, RankingCalculator.calculatePlayerRating(500))

        // Giải đấu khác: 12 lượt công + 12 lượt thủ -> x = 960 cúp
        // 60% x = 576, 55% x = 528, 50% x = 480
        assertEquals(RatingCategory.DOMINANT, RankingCalculator.calculatePlayerRating(600, maxAttacks = 12))
        assertEquals(RatingCategory.DOMINANT, RankingCalculator.calculatePlayerRating(576, maxAttacks = 12))
        assertEquals(RatingCategory.SUPERIOR, RankingCalculator.calculatePlayerRating(550, maxAttacks = 12))
        assertEquals(RatingCategory.POTENTIAL, RankingCalculator.calculatePlayerRating(500, maxAttacks = 12))
        assertEquals(RatingCategory.ALARM, RankingCalculator.calculatePlayerRating(400, maxAttacks = 12))
    }

    @Test
    fun testNormalizeSeasonAndRankingOrder() {
        val players = listOf(
            Player(id = "p1", name = "Player A", currentCups = 1000, attacks = 10),
            Player(id = "p2", name = "Player B", currentCups = 1200, attacks = 10),
            Player(id = "p3", name = "Player C", currentCups = 800, attacks = 10)
        )
        val season = Season(
            league = "Legend 3",
            seasonName = "Test Season",
            myPlayerId = "p1",
            players = players
        )

        val normalized = RankingCalculator.normalizeSeason(season)

        // Player B has 1200 cups -> rank 1
        // Player A has 1000 cups -> rank 2
        // Player C has 800 cups -> rank 3
        assertEquals("p2", normalized.players[0].id)
        assertEquals(1, normalized.players[0].rank)

        assertEquals("p1", normalized.players[1].id)
        assertEquals(2, normalized.players[1].rank)
        assertTrue(normalized.players[1].isMe)

        assertEquals("p3", normalized.players[2].id)
        assertEquals(3, normalized.players[2].rank)
    }

    @Test
    fun testRankingStats() {
        val players = listOf(
            Player(id = "p1", name = "Leader", currentCups = 1300, attacks = 24, defenses = 24), // maxPossibleCups = 1300
            Player(id = "me", name = "Me", currentCups = 1100, attacks = 20, defenses = 20),      // maxPossible = 1100 + 4*40 + 4*40 = 1420
            Player(id = "p3", name = "Trailing", currentCups = 700, attacks = 24, defenses = 24)  // maxPossible = 700
        )
        val season = Season(
            league = "Legend 3",
            myPlayerId = "me",
            players = players
        )

        val stats = RankingCalculator.getRankingStats(season)

        // Leader max cups is 1300, Me max cups is 1420.
        // Leader max cups is NOT > Me max cups.
        // Trailing max cups is 700, NOT > 1420.
        // So 0 players can pass me in theoretical ceiling:
        assertEquals(0, stats.playersWhoCanPassMe)
        assertEquals(1, stats.lowestPossibleRank)
    }

    @Test
    fun testPromotionAndDemotionRules() {
        // Electro 33: 15 promotion, 15 demotion
        val electroRules = com.cocrank.dashboard.data.RankedTierMetadata.getPromotionAndDemotionCount("Electro League 33", 105000033L)
        assertEquals(15, electroRules.first)
        assertEquals(15, electroRules.second)

        // Legend 3: 5 promotion, 15 demotion
        val legend3Rules = com.cocrank.dashboard.data.RankedTierMetadata.getPromotionAndDemotionCount("Legend III", 105000034L)
        assertEquals(5, legend3Rules.first)
        assertEquals(15, legend3Rules.second)

        // Legend 2: 5 promotion, 15 demotion
        val legend2Rules = com.cocrank.dashboard.data.RankedTierMetadata.getPromotionAndDemotionCount("Legend II", 105000035L)
        assertEquals(5, legend2Rules.first)
        assertEquals(15, legend2Rules.second)

        // Legend 1: 0 promotion, 15 demotion
        val legend1Rules = com.cocrank.dashboard.data.RankedTierMetadata.getPromotionAndDemotionCount("Legend I", 105000036L)
        assertEquals(0, legend1Rules.first)
        assertEquals(15, legend1Rules.second)
    }

    @Test
    fun testFindBestRankAndCupsByHighestTier() {
        val history = listOf(
            // Season 1: Electro 33 (tier 33), Rank #1, 3200 trophies
            com.cocrank.dashboard.domain.model.LeagueHistoryItem(
                leagueSeasonId = 1,
                leagueTierId = 105000033L,
                tierName = "Electro League 33",
                placement = 1,
                leagueTrophies = 3200
            ),
            // Season 2: Legend 3 (tier 34), Rank #80, 2500 trophies
            com.cocrank.dashboard.domain.model.LeagueHistoryItem(
                leagueSeasonId = 2,
                leagueTierId = 105000034L,
                tierName = "Legend III",
                placement = 80,
                leagueTrophies = 2500
            ),
            // Season 3: Legend 3 (tier 34), Rank #25, 2700 trophies
            com.cocrank.dashboard.domain.model.LeagueHistoryItem(
                leagueSeasonId = 3,
                leagueTierId = 105000034L,
                tierName = "Legend III",
                placement = 25,
                leagueTrophies = 2700
            )
        )

        val bestRank = RankingCalculator.findBestRankSeason(history)
        // Legend 3 (tier 34) > Electro 33 (tier 33)
        // Between season 2 (#80) and season 3 (#25), #25 is better!
        assertEquals(3L, bestRank?.leagueSeasonId)
        assertEquals(25, bestRank?.placement)
        assertEquals("Legend III", bestRank?.tierName)

        val bestCups = RankingCalculator.findBestCupsSeason(history)
        // Legend 3 (tier 34) > Electro 33 (tier 33)
        // Between season 2 (2500) and season 3 (2700), 2700 is higher!
        assertEquals(3L, bestCups?.leagueSeasonId)
        assertEquals(2700, bestCups?.leagueTrophies)
        assertEquals("Legend III", bestCups?.tierName)
    }
}
