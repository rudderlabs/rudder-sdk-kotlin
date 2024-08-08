package com.rudderstack.core.internals.web

import com.rudderstack.core.internals.utils.validatedBaseUrl
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WebServiceImpl(
    override var baseUrl: String,
    override val authHeaderString: String,
    override val isGzipEnabled: Boolean,
    override val connectTimeOut: Int = 10_000,
    override val readTimeOut: Int = 20_000,
    override val customHeaders: Map<String, String> = emptyMap(),
) : WebService {

    private val defaultHeaders = mapOf(
        "Content-Type" to "application/json",
        "Authorization" to String.format(
            Locale.US, "Basic $authHeaderString",
        )
    )

    override fun getSourceConfig(endpoint: String): Result<String> {
        val connection = connectionFactory(endpoint)
        return handleConnection(connection)
    }

    override fun connectionFactory(endpoint: String): HttpURLConnection {
        val urlStr = "${baseUrl.validatedBaseUrl}$endpoint"
        val url = URL(urlStr)
        return (url.openConnection() as HttpURLConnection).also {
            it.connectTimeout = connectTimeOut
            it.readTimeout = readTimeOut
            val defaultReadTimeOut = it.readTimeout
            println(defaultReadTimeOut)

            defaultHeaders.forEach(it::setRequestProperty)
            customHeaders.forEach(it::setRequestProperty)
        }
    }

    private fun handleConnection(connection: HttpURLConnection): Result<String> = try {
        connection.connect()
        constructResponse(connection)
    } catch (e: Exception) {
        connection.disconnect()
        Failure(
            status = NetworkStatus.ERROR,
            error = e
        )
    }

    private fun constructResponse(
        connection: HttpURLConnection
    ): Result<String> = if (connection.responseCode in 200..299) {
        Success(response = getInputResponseBody(connection))
    } else {
        Failure(
            status = getNetworkStatus(connection.responseCode),
            error = IOException("HTTP ${connection.responseCode}, URL:${connection.url} and Error: ${getErrorResponseBody(connection)}")
        )
    }

    private fun getInputResponseBody(connection: HttpURLConnection) =
        connection.inputStream.bufferedReader().use(BufferedReader::readText)

    private fun getErrorResponseBody(connection: HttpURLConnection) =
        connection.errorStream.bufferedReader().use(BufferedReader::readText)

    private fun getNetworkStatus(responseCode: Int): NetworkStatus {
        return when (responseCode) {
            200 -> NetworkStatus.SUCCESS
            404 -> NetworkStatus.RESOURCE_NOT_FOUND
            400 -> NetworkStatus.BAD_REQUEST
            else -> NetworkStatus.ERROR
        }
    }
}
