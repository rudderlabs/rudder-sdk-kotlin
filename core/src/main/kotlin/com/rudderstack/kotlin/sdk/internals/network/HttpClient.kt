package com.rudderstack.kotlin.sdk.internals.network

import java.net.HttpURLConnection
import java.net.URL

/**
 * Represents a client for making HTTP requests with predefined configurations.
 * Provides methods to send and retrieve data over HTTP and manage custom headers and configurations.
 */
interface HttpClient {

    /**
     * The base URL for the HTTP client.
     */
    val baseUrl: String

    /**
     * The specific endpoint to which HTTP requests will be sent.
     */
    val endPoint: String

    /**
     * The authorization header string used for authenticated requests.
     */
    val authHeaderString: String

    /**
     * The configuration for HTTP GET requests.
     */
    val getConfig: GetConfig

    /**
     * The configuration for HTTP POST requests.
     */
    var postConfig: PostConfig

    /**
     * A map of custom headers to be included in each HTTP request.
     */
    val customHeaders: Map<String, String>

    /**
     * A factory for creating [HttpURLConnection] instances for HTTP requests.
     */
    val connectionFactory: HttpURLConnectionFactory

    /**
     * Updates the anonymous ID header string for HTTP requests.
     *
     * @param anonymousIdHeaderString The new anonymous ID header string to set.
     */
    fun updateAnonymousIdHeaderString(anonymousIdHeaderString: String)

    /**
     * Retrieves data from the server using a GET request.
     *
     * @return A [Result] containing the response data as a [String], or an error message if the request fails.
     */
    fun getData(): Result<String>

    /**
     * Sends data to the server using a POST request.
     *
     * @param body The body of the POST request as a [String].
     * @return A [Result] containing the response data as a [String], or an error message if the request fails.
     */
    fun sendData(body: String): Result<String>
}

/**
 * Represents the configuration for HTTP GET requests.
 */
interface GetConfig {

    /**
     * A map of query parameters to be included in the GET request URL.
     */
    val query: Map<String, String>
}

/**
 * Represents the configuration for HTTP POST requests.
 */
interface PostConfig {

    /**
     * Indicates whether GZIP compression is enabled for the POST request body.
     */
    val isGZIPEnabled: Boolean

    /**
     * The anonymous ID header string to be included in the POST request.
     */
    val anonymousIdHeaderString: String
}

/**
 * Represents a factory for creating [HttpURLConnection] instances.
 */
interface HttpURLConnectionFactory {

    /**
     * Creates an [HttpURLConnection] instance for the specified URL and headers.
     *
     * @param url The [URL] for which the connection is to be created.
     * @param headers A map of headers to be included in the request.
     * @return An [HttpURLConnection] instance for the specified URL and headers.
     */
    fun createConnection(url: URL, headers: Map<String, String>): HttpURLConnection
}
