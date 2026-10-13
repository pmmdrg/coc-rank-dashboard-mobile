package com.cocrank.dashboard.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface CocApiService {

    @GET("v1/players/{playerTag}")
    suspend fun getPlayer(
        @Path("playerTag", encoded = true) playerTag: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<CocPlayerResponse>

    @GET("v1/players/{playerTag}/leaguehistory")
    suspend fun getPlayerLeagueHistory(
        @Path("playerTag", encoded = true) playerTag: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<CocLeagueHistoryResponse>

    @GET("v1/leaguegroup/{groupTag}/{seasonId}")
    suspend fun getLeagueGroup(
        @Path("groupTag", encoded = true) groupTag: String,
        @Path("seasonId", encoded = true) seasonId: String,
        @Query("playerTag") playerTag: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): Response<CocLeagueGroupResponse>

    @GET("v1/clans/{clanTag}")
    suspend fun getClan(
        @Path("clanTag", encoded = true) clanTag: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<CocClanDetailResponse>
}
