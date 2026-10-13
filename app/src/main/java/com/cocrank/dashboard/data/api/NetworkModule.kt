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
        val url = if (!baseUrl.isNullOrBlank()) {
            if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        } else {
            DEFAULT_BASE_URL
        }

        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CocApiService::class.java)
    }
}
