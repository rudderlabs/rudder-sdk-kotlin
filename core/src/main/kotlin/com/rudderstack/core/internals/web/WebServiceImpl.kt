package com.rudderstack.core.internals.web

import com.rudderstack.core.Constants.DEFAULT_CONNECTION_TIMEOUT
import com.rudderstack.core.Constants.DEFAULT_READ_TIMEOUT
import com.rudderstack.core.internals.utils.getErrorResponse
import com.rudderstack.core.internals.utils.getErrorStatus
import com.rudderstack.core.internals.utils.getSuccessResponse
import com.rudderstack.core.internals.utils.validatedBaseUrl
import com.rudderstack.core.internals.utils.writeBodyToStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.GZIPOutputStream

class WebServiceImpl(
    private var baseUrl: String,
    authHeaderString: String,
    private val isGZIPEnabled: Boolean,
    private val anonymousIdHeaderString: String,
    private val customHeaders: Map<String, String> = emptyMap(),
    private val connectionFactory: HttpURLConnectionFactory = DefaultHttpURLConnectionFactory()
) : WebService {

    private val defaultHeaders = mapOf(
        "Content-Type" to "application/json",
        "Authorization" to String.format(
            Locale.US, "Basic $authHeaderString",
        )
    )

    override fun getData(endpoint: String): Result<String> =
        connectionFactory(endpoint).useConnection()

    override fun sendData(endPoint: String, body: String): Result<String> =
        connectionFactory(endPoint).useConnection {
            setupPostConnection(body)
        }

    override fun connectionFactory(endpoint: String): HttpURLConnection {
        val url = URL("${baseUrl.validatedBaseUrl}$endpoint")
        return connectionFactory.createConnection(url).apply {
            connectTimeout = DEFAULT_CONNECTION_TIMEOUT
            readTimeout = DEFAULT_READ_TIMEOUT
            (defaultHeaders + customHeaders).forEach(::setRequestProperty)
        }
    }

    private fun HttpURLConnection.useConnection(setup: HttpURLConnection.() -> Unit = {}): Result<String> {
        return try {
            this.apply(setup)
            connect()
            constructResponse()
        } catch (e: Exception) {
            Failure(status = ErrorStatus.ERROR, error = e)
        } finally {
            disconnect()
        }
    }

    private fun HttpURLConnection.setupPostConnection(body: String) = apply {
        doOutput = true
        setChunkedStreamingMode(0)
        setRequestProperty("AnonymousId", anonymousIdHeaderString)
        if (isGZIPEnabled) {
            setRequestProperty("Content-Encoding", "gzip")
            GZIPOutputStream(outputStream).writeBodyToStream(body)
        } else {
            outputStream.writeBodyToStream(body)
        }
    }

    private fun HttpURLConnection.constructResponse(): Result<String> =
        when (responseCode) {
            in 200..299 -> Success(response = getSuccessResponse())

            else -> Failure(
                status = getErrorStatus(responseCode),
                error = IOException(
                    "HTTP $responseCode, URL: $url, Error: ${getErrorResponse()}"
                )
            )
        }
}

class DefaultHttpURLConnectionFactory : HttpURLConnectionFactory {
    override fun createConnection(url: URL): HttpURLConnection {
        return url.openConnection() as HttpURLConnection
    }
}
