package com.rudderstack.core.internals.web

import java.net.HttpURLConnection
import java.net.URL

interface WebService {

    fun getData(endpoint: String): Result<String>

    fun sendData(endPoint: String, body: String): Result<String>

    fun connectionFactory(endpoint: String): HttpURLConnection
}

enum class ErrorStatus {
    INVALID_WRITE_KEY,
    ERROR,
    RESOURCE_NOT_FOUND,
    BAD_REQUEST,
    RETRY_ABLE,
}

interface HttpURLConnectionFactory {
    fun createConnection(url: URL): HttpURLConnection
}
