package com.cocrank.dashboard.data

data class RankedTierDefinition(
    val id: Long,
    val tierNumber: Int,
    val name: String,
    val group: String,
    val baseAttacks: Int,
    val isPerDay: Boolean = false
)

object RankedTierMetadata {
    val TIERS = listOf(
        // Skeleton League: 6 lượt
        RankedTierDefinition(105000001, 1, "Skeleton League 1", "Skeleton", 6),
        RankedTierDefinition(105000002, 2, "Skeleton League 2", "Skeleton", 6),
        RankedTierDefinition(105000003, 3, "Skeleton League 3", "Skeleton", 6),

        // Barbarian League: 6 lượt
        RankedTierDefinition(105000004, 4, "Barbarian League 4", "Barbarian", 6),
        RankedTierDefinition(105000005, 5, "Barbarian League 5", "Barbarian", 6),
        RankedTierDefinition(105000006, 6, "Barbarian League 6", "Barbarian", 6),

        // Archer League: 8 lượt
        RankedTierDefinition(105000007, 7, "Archer League 7", "Archer", 8),
        RankedTierDefinition(105000008, 8, "Archer League 8", "Archer", 8),
        RankedTierDefinition(105000009, 9, "Archer League 9", "Archer", 8),

        // Wizard League: 8 lượt
        RankedTierDefinition(105000010, 10, "Wizard League 10", "Wizard", 8),
        RankedTierDefinition(105000011, 11, "Wizard League 11", "Wizard", 8),
        RankedTierDefinition(105000012, 12, "Wizard League 12", "Wizard", 8),

        // Valkyrie League: 10 lượt
        RankedTierDefinition(105000013, 13, "Valkyrie League 13", "Valkyrie", 10),
        RankedTierDefinition(105000014, 14, "Valkyrie League 14", "Valkyrie", 10),
        RankedTierDefinition(105000015, 15, "Valkyrie League 15", "Valkyrie", 10),

        // Witch League: 10 lượt
        RankedTierDefinition(105000016, 16, "Witch League 16", "Witch", 10),
        RankedTierDefinition(105000017, 17, "Witch League 17", "Witch", 10),
        RankedTierDefinition(105000018, 18, "Witch League 18", "Witch", 10),

        // Golem League: 12 lượt
        RankedTierDefinition(105000019, 19, "Golem League 19", "Golem", 12),
        RankedTierDefinition(105000020, 20, "Golem League 20", "Golem", 12),
        RankedTierDefinition(105000021, 21, "Golem League 21", "Golem", 12),

        // P.E.K.K.A League: 12 lượt
        RankedTierDefinition(105000022, 22, "P.E.K.K.A League 22", "P.E.K.K.A", 12),
        RankedTierDefinition(105000023, 23, "P.E.K.K.A League 23", "P.E.K.K.A", 12),
        RankedTierDefinition(105000024, 24, "P.E.K.K.A League 24", "P.E.K.K.A", 12),

        // Titan League: 12 lượt
        RankedTierDefinition(105000025, 25, "Titan League 25", "Titan", 12),
        RankedTierDefinition(105000026, 26, "Titan League 26", "Titan", 12),
        RankedTierDefinition(105000027, 27, "Titan League 27", "Titan", 12),

        // Dragon League: 14 lượt
        RankedTierDefinition(105000028, 28, "Dragon League 28", "Dragon", 14),
        RankedTierDefinition(105000029, 29, "Dragon League 29", "Dragon", 14),
        RankedTierDefinition(105000030, 30, "Dragon League 30", "Dragon", 14),

        // Electro League: 18 lượt
        RankedTierDefinition(105000031, 31, "Electro League 31", "Electro", 18),
        RankedTierDefinition(105000032, 32, "Electro League 32", "Electro", 18),
        RankedTierDefinition(105000033, 33, "Electro League 33", "Electro", 18),

        // Legend III: 24 lượt
        RankedTierDefinition(105000034, 34, "Legend III", "Legend 3", 24),

        // Legend II: 30 lượt
        RankedTierDefinition(105000035, 35, "Legend II", "Legend 2", 30),

        // Legend I: 8 lượt 1 ngày (mặc định 6 ngày = 48 lượt)
        RankedTierDefinition(105000036, 36, "Legend I", "Legend 1", 8, isPerDay = true)
    )

    fun getMaxAttacksForLeague(leagueName: String?, observedMaxAttacks: Int = 0): Int {
        if (leagueName.isNullOrBlank()) {
            return if (observedMaxAttacks > 0) observedMaxAttacks else 24
        }
        val normalized = leagueName.lowercase()
        val found = TIERS.firstOrNull {
            normalized.contains(it.name.lowercase()) ||
            normalized.contains(it.group.lowercase())
        }
        return when {
            found != null && found.isPerDay -> 48 // Legend 1 ~ 6 ngày * 8 lượt
            found != null -> found.baseAttacks
            observedMaxAttacks > 0 -> observedMaxAttacks
            else -> 24
        }
    }
}
