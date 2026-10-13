package com.cocrank.dashboard.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val DEFAULT_BASE_URL = "https://api.clashofclans.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun createService(baseUrl: String? = null): CocApiService {
        val raw = baseUrl?.trim()?.ifBlank { null } ?: CocApiConfig.DEFAULT_PROXY_URL
        val normalized = raw.trimEnd('/')
        // Nếu URL truyền vào có đuôi /v1 thì chuẩn hóa thành base URL cho Retrofit
        val finalUrl = if (normalized.endsWith("/v1", ignoreCase = true)) {
            normalized.removeSuffix("/v1").removeSuffix("/V1").trimEnd('/') + "/"
        } else {
            "$normalized/"
        }

        return Retrofit.Builder()
            .baseUrl(finalUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CocApiService::class.java)
    }
}
