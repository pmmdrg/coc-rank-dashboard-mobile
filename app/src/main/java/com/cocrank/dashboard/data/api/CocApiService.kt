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

    @GET("v1/leagues/{leagueId}/seasons/{seasonId}/groups/{groupTag}")
    suspend fun getLeagueSeasonGroup(
        @Path("leagueId", encoded = true) leagueId: String,
        @Path("seasonId", encoded = true) seasonId: String,
        @Path("groupTag", encoded = true) groupTag: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<CocLeagueGroupResponse>

    @GET("v1/clans/{clanTag}")
    suspend fun getClan(
        @Path("clanTag", encoded = true) clanTag: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<CocClanDetailResponse>

    // Hỗ trợ gọi qua Proxy endpoint dạng ?path=...
    @GET("api/coc")
    suspend fun proxyGet(
        @Query("path") path: String
    ): Response<okhttp3.ResponseBody>
}
