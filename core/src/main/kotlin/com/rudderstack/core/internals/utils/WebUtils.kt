package com.rudderstack.core.internals.utils

import com.rudderstack.core.internals.web.ErrorStatus
import com.rudderstack.core.internals.web.GetConfig
import com.rudderstack.core.internals.web.PostConfig
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

fun HttpURLConnection.getSuccessResponse() =
    inputStream.bufferedReader().use(BufferedReader::readText)

fun HttpURLConnection.getErrorResponse() =
    errorStream.bufferedReader().use(BufferedReader::readText)

fun GZIPOutputStream.writeBodyToStream(body: String) = this.use {
    it.write(body.toByteArray())
}

fun OutputStream.writeBodyToStream(body: String) = this.use {
    it.write(body.toByteArray())
}

fun getErrorStatus(responseCode: Int): ErrorStatus = when (responseCode) {
    401 -> ErrorStatus.INVALID_WRITE_KEY
    404 -> ErrorStatus.RESOURCE_NOT_FOUND
    400 -> ErrorStatus.BAD_REQUEST
    429, in 500..599 -> ErrorStatus.RETRY_ABLE
    else -> ErrorStatus.ERROR
}

fun createURL(baseUrl: String, endPoint: String, query: Map<String, String> = emptyMap()): URL {
    return buildString {
        append(baseUrl.validatedBaseUrl)
        append(endPoint)
        if (query.isNotEmpty()) {
            append("?")
            query.entries.joinToString("&") { (key, value) ->
                "$key=$value"
            }.let { append(it) }
        }
    }.let {
        URL(it)
    }
}

fun createGetConfig(query: Map<String, String> = emptyMap()) = object : GetConfig {
    override val query: Map<String, String> = query
}

fun createPostConfig(
    isGZIPEnabled: Boolean,
    anonymousIdHeaderString: String = String.empty(),
) = object : PostConfig {
    override val isGZIPEnabled: Boolean = isGZIPEnabled
    override val anonymousIdHeaderString: String = anonymousIdHeaderString
}
