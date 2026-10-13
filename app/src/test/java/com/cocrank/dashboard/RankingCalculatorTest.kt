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
}
