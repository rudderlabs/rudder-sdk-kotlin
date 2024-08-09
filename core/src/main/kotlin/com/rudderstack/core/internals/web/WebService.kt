package com.rudderstack.core.internals.web

import java.net.HttpURLConnection

interface WebService {

    fun getData(endpoint: String): Result<String>

    fun sendData(endPoint: String, body: String): Result<String>

    fun connectionFactory(endpoint: String): HttpURLConnection
}

enum class ErrorStatus {
    ERROR,
    RESOURCE_NOT_FOUND,
    BAD_REQUEST,
}
