package com.cocrank.dashboard.data.api

import com.cocrank.dashboard.BuildConfig

/**
 * Cấu hình Clash of Clans API.
 * Token và Proxy URL được nạp an toàn từ secrets.properties hoặc biến môi trường cục bộ
 * thông qua BuildConfig lúc build, hoàn toàn KHÔNG lưu trong mã nguồn Git.
 */
object CocApiConfig {
    val DEFAULT_PROXY_URL: String = BuildConfig.COC_PROXY_URL.ifBlank { "https://cocproxy.royaleapi.dev/v1" }
    val DEFAULT_API_TOKEN: String = BuildConfig.COC_API_TOKEN
}
