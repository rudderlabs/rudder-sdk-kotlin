package com.rudderstack.core.internals.web

import com.rudderstack.core.internals.utils.validatedBaseUrl
import com.rudderstack.core.internals.web.ErrorStatus.Companion.getErrorStatus
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.GZIPOutputStream

private const val CONTENT_TYPE = "Content-Type"
private const val APPLICATION_JSON = "application/json"
private const val AUTHORIZATION = "Authorization"
private const val BASIC = "Basic"
private const val ANONYMOUS_ID_HEADER = "AnonymousId"
private const val CONTENT_ENCODING = "Content-Encoding"
private const val GZIP = "gzip"

private const val DEFAULT_CONNECTION_TIMEOUT: Int = 10_000
private const val DEFAULT_READ_TIMEOUT: Int = 20_000

/**
 * `HttpClientImpl` is a concrete implementation of the `HttpClient` interface that manages
 * HTTP connections for sending and retrieving data. This class is designed to handle both
 * GET and POST requests, offering support for custom headers, GZIP compression, and robust
 * error handling.
 *
 * @property baseUrl The base URL for the HttpClient to which requests are sent.
 * @property endPoint The specific endpoint appended to the base URL for each request.
 * @property authHeaderString The authorization header string, typically used for Basic Auth.
 * @property getConfig Configuration options specific to GET requests, such as query parameters.
 * @property postConfig Configuration options specific to POST requests, including GZIP
 * compression and a custom identifier header.
 * @property customHeaders Additional HTTP headers to include in the request.
 * @property connectionFactory A factory responsible for creating instances of `HttpURLConnection`.
 */
class HttpClientImpl private constructor(
    override val baseUrl: String,
    override val endPoint: String,
    override val authHeaderString: String,
    override val getConfig: GetConfig,
    override var postConfig: PostConfig,
    override val customHeaders: Map<String, String>,
    override val connectionFactory: HttpURLConnectionFactory,
) : HttpClient {

    companion object {
        /**
         * Creates a new instance of `HttpClientImpl` configured for making HTTP GET requests.
         *
         * This method configures a `HttpClientImpl` object specifically for GET requests,
         * with default settings that disable GZIP compression.
         *
         * @param baseUrl The base URL for the HttpClient.
         * @param endPoint The specific endpoint appended to the base URL.
         * @param authHeaderString The authorization header string, typically for Basic Auth.
         * @param query The query parameters appended to the URL. Defaults to an empty map.
         * @param customHeaders Additional HTTP headers for the request. Defaults to an empty map.
         * @param connectionFactory A factory for creating `HttpURLConnection` instances. Defaults to `DefaultHttpURLConnectionFactory()`.
         * @return A configured `HttpClientImpl` instance for GET requests.
         */
        fun createGetHttpClient(
            baseUrl: String,
            endPoint: String,
            authHeaderString: String,
            query: Map<String, String> = emptyMap(),
            customHeaders: Map<String, String> = emptyMap(),
            connectionFactory: HttpURLConnectionFactory = DefaultHttpURLConnectionFactory()
        ) = HttpClientImpl(
            baseUrl = baseUrl,
            endPoint = endPoint,
            authHeaderString = authHeaderString,
            getConfig = createGetConfig(query),
            postConfig = createPostConfig(isGZIPEnabled = false),
            customHeaders = customHeaders,
            connectionFactory = connectionFactory,
        )

        /**
         * Creates a new instance of `HttpClientImpl` configured for making HTTP POST requests.
         *
         * This method configures a `HttpClientImpl` object specifically for POST requests,
         * with options to enable GZIP compression and include a custom identifier header.
         *
         * @param baseUrl The base URL for the HttpClient.
         * @param endPoint The specific endpoint appended to the base URL.
         * @param authHeaderString The authorization header string, typically for Basic Auth.
         * @param isGZIPEnabled A flag indicating whether GZIP compression is enabled for POST requests.
         * @param anonymousIdHeaderString A custom header used to identify anonymous users.
         * @param customHeaders Additional HTTP headers for the request. Defaults to an empty map.
         * @param connectionFactory A factory for creating `HttpURLConnection` instances. Defaults to `DefaultHttpURLConnectionFactory()`.
         * @return A configured `HttpClientImpl` instance for POST requests.
         */
        fun createPostHttpClient(
            baseUrl: String,
            endPoint: String,
            authHeaderString: String,
            isGZIPEnabled: Boolean,
            anonymousIdHeaderString: String,
            customHeaders: Map<String, String> = emptyMap(),
            connectionFactory: HttpURLConnectionFactory = DefaultHttpURLConnectionFactory()
        ) = HttpClientImpl(
            baseUrl = baseUrl,
            endPoint = endPoint,
            authHeaderString = authHeaderString,
            getConfig = createGetConfig(),
            postConfig = createPostConfig(
                isGZIPEnabled = isGZIPEnabled,
                anonymousIdHeaderString = anonymousIdHeaderString,
            ),
            customHeaders = customHeaders,
            connectionFactory = connectionFactory,
        )
    }

    private val defaultHeaders = mapOf(
        CONTENT_TYPE to APPLICATION_JSON,
        AUTHORIZATION to String.format(
            Locale.US, BASIC + authHeaderString,
        )
    )

    private val headers = defaultHeaders + customHeaders

    /**
     * Updates the anonymous ID header string used in POST requests as an header.
     *
     * @param anonymousIdHeaderString The new custom header string to be used.
     */
    override fun updateAnonymousIdHeaderString(anonymousIdHeaderString: String) {
        postConfig = createPostConfig(
            isGZIPEnabled = postConfig.isGZIPEnabled,
            anonymousIdHeaderString = anonymousIdHeaderString,
        )
    }

    /**
     * Retrieves data from the specified endpoint using an HTTP GET request.
     * This method constructs a connection using the base URL, endpoint, query parameters,
     * and headers, and then reads the response.
     *
     * @return `Result<String>` containing the response data or an error.
     */
    override fun getData(): Result<String> {
        val url: URL = createURL(baseUrl, endPoint, getConfig.query)
        return connectionFactory.createConnection(url, headers)
            .useConnection()
    }

    /**
     * Sends data to the specified endpoint using an HTTP POST request.
     * This method constructs a connection using the base URL, endpoint, query parameters,
     * headers, and the provided request body, and then reads the response.
     *
     * @param body The body of the POST request to be sent.
     * @return `Result<String>` containing the response data or an error.
     */
    override fun sendData(body: String): Result<String> {
        val url = createURL(baseUrl, endPoint)
        return connectionFactory.createConnection(url, headers)
            .useConnection {
                setupPostConnection(body)
            }
    }

    private fun createURL(baseUrl: String, endPoint: String, query: Map<String, String> = emptyMap()): URL {
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
        setRequestProperty(ANONYMOUS_ID_HEADER, postConfig.anonymousIdHeaderString)
        if (postConfig.isGZIPEnabled) {
            setRequestProperty(CONTENT_ENCODING, GZIP)
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
    override fun createConnection(
        url: URL,
        headers: Map<String, String>,
    ): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        return connection.apply {
            connectTimeout = DEFAULT_CONNECTION_TIMEOUT
            readTimeout = DEFAULT_READ_TIMEOUT
            headers.forEach(::setRequestProperty)
        }
    }
}
