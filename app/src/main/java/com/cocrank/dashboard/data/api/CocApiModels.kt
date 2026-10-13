package com.cocrank.dashboard.data.api

import com.google.gson.annotations.SerializedName

data class CocPlayerResponse(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("townHallLevel") val townHallLevel: Int = 0,
    @SerializedName("expLevel") val expLevel: Int = 0,
    @SerializedName("trophies") val trophies: Int = 0,
    @SerializedName("bestTrophies") val bestTrophies: Int = 0,
    @SerializedName("attackWins") val attackWins: Int = 0,
    @SerializedName("defenseWins") val defenseWins: Int = 0,
    @SerializedName("clan") val clan: CocClanSummary? = null,
    @SerializedName("leagueTier") val leagueTier: CocLeagueTier? = null,
    @SerializedName("leagueGroup") val leagueGroup: CocLeagueGroupSummary? = null
)

data class CocClanSummary(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("clanLevel") val clanLevel: Int = 0,
    @SerializedName("badgeUrls") val badgeUrls: CocBadgeUrls? = null
)

data class CocBadgeUrls(
    @SerializedName("small") val small: String? = null,
    @SerializedName("medium") val medium: String? = null,
    @SerializedName("large") val large: String? = null
)

data class CocLeagueTier(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("iconUrls") val iconUrls: CocBadgeUrls? = null
)

data class CocLeagueGroupSummary(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("seasonId") val seasonId: String? = null
)

data class CocLeagueGroupResponse(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("tierId") val tierId: Long = 0,
    @SerializedName("tierName") val tierName: String? = null,
    @SerializedName("seasonId") val seasonId: String? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("members") val members: List<CocGroupMember> = emptyList()
)

data class CocGroupMember(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("clan") val clan: CocClanSummary? = null,
    @SerializedName("trophies") val trophies: Int = 0,
    @SerializedName("attackWinCount") val attackWinCount: Int = 0,
    @SerializedName("attackLoseCount") val attackLoseCount: Int = 0,
    @SerializedName("defenseWinCount") val defenseWinCount: Int = 0,
    @SerializedName("defenseLoseCount") val defenseLoseCount: Int = 0,
    @SerializedName("rank") val rank: Int = 0
)

data class CocClanDetailResponse(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("clanLevel") val clanLevel: Int = 0,
    @SerializedName("clanPoints") val clanPoints: Int = 0,
    @SerializedName("members") val members: Int = 0,
    @SerializedName("badgeUrls") val badgeUrls: CocBadgeUrls? = null,
    @SerializedName("memberList") val memberList: List<CocClanMemberItem> = emptyList()
)

data class CocClanMemberItem(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("role") val role: String = "",
    @SerializedName("townHallLevel") val townHallLevel: Int = 0,
    @SerializedName("expLevel") val expLevel: Int = 0,
    @SerializedName("clanRank") val clanRank: Int = 0,
    @SerializedName("previousClanRank") val previousClanRank: Int = 0,
    @SerializedName("trophies") val trophies: Int = 0,
    @SerializedName("donations") val donations: Int = 0,
    @SerializedName("donationsReceived") val donationsReceived: Int = 0,
    @SerializedName("leagueTier") val leagueTier: CocLeagueTier? = null
)
