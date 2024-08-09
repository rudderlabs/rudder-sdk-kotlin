package com.rudderstack.core.internals.utils

import com.rudderstack.core.internals.web.ErrorStatus
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
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
