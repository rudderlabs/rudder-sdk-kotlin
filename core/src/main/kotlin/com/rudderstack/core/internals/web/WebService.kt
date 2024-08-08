package com.rudderstack.core.internals.web

import java.net.HttpURLConnection

interface WebService {
    val baseUrl: String
    val authHeaderString: String
    val isGzipEnabled: Boolean
    val connectTimeOut: Int
    val readTimeOut: Int
    val customHeaders: Map<String, String>?

    fun getSourceConfig(endpoint: String): Result<String>

    fun connectionFactory(endpoint: String): HttpURLConnection
}

enum class NetworkStatus {
    SUCCESS,
    ERROR,
    RESOURCE_NOT_FOUND,
    BAD_REQUEST,
}
