package com.rudderstack.sdk.kotlin.core.internals.network.provider

import com.rudderstack.sdk.kotlin.core.Configuration.Companion.DEFAULT_GZIP_STATUS
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.network.HttpURLConnectionFactory
import java.net.HttpURLConnection

internal fun provideHttpClientImplForGetRequest(
    connectionFactory: HttpURLConnectionFactory,
    baseUrl: String = "https://api.example.com",
    endPoint: String = "/test",
    authHeaderString: String = "auth-header",
    query: Map<String, String> = mapOf(
        "p" to "android",
        "v" to "2.0",
        "bv" to "34",
    ),
    customHeaders: Map<String, String> = emptyMap(),
) = HttpClientImpl.createGetHttpClient(
    baseUrl = baseUrl,
    endPoint = endPoint,
    authHeaderString = authHeaderString,
    query = query,
    customHeaders = customHeaders,
    connectionFactory = connectionFactory,
)

internal fun provideHttpClientImplForPostRequest(
    connectionFactory: HttpURLConnectionFactory,
    baseUrl: String = "https://api.example.com",
    endPoint: String = "/test",
    authHeaderString: String = "auth-header",
    isGZIPEnabled: Boolean = DEFAULT_GZIP_STATUS,
    anonymousIdHeaderString: String = "anonymous-id",
    customHeaders: Map<String, String> = emptyMap(),
) = HttpClientImpl.createPostHttpClient(
    baseUrl = baseUrl,
    endPoint = endPoint,
    authHeaderString = authHeaderString,
    isGZIPEnabled = isGZIPEnabled,
    anonymousIdHeaderString = anonymousIdHeaderString,
    customHeaders = customHeaders,
    connectionFactory = connectionFactory,
)

fun provideErrorMessage(
    status: Int,
    connection: HttpURLConnection,
    msg: String = "Some error occurred"
) = "HTTP $status, URL: ${connection.url}, Error: $msg"
