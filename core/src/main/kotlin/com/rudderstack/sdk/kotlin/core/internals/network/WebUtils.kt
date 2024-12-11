package com.rudderstack.sdk.kotlin.core.internals.network

import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.util.zip.GZIPOutputStream

/**
 * Utility extension functions and internal configuration functions for working with `HttpURLConnection`
 * and handling HTTP requests and responses. These functions provide streamlined methods for reading
 * HTTP responses, writing data to streams, and configuring GET and POST request options.
 *
 * This collection of functions is designed to be used internally within the library for
 * managing HTTP connections and their associated configurations, particularly for handling
 * GZIP compression and custom headers.
 */

/**
 * Reads the response from an `HttpURLConnection` input stream when a request is successful.
 *
 * This extension function reads the entire response body from the connection's input stream
 * and returns it as a `String`. It is specifically used for handling successful HTTP responses.
 *
 * @return A `String` containing the response body.
 */
fun HttpURLConnection.getSuccessResponse(): String = inputStream.bufferedReader().use(BufferedReader::readText)

/**
 * Reads the response from an `HttpURLConnection` error stream when a request fails.
 *
 * This extension function reads the entire response body from the connection's error stream
 * and returns it as a `String`. It is specifically used for handling error HTTP responses.
 *
 * @return A `String` containing the error response body.
 */
fun HttpURLConnection.getErrorResponse(): String = errorStream.bufferedReader().use(BufferedReader::readText)

/**
 * Writes the provided request body to a `GZIPOutputStream`.
 *
 * This extension function enables writing a request body to a `GZIPOutputStream` for POST requests
 * where GZIP compression is enabled. It automatically manages the lifecycle of the stream using `use`.
 *
 * @param body The `String` body to write to the stream.
 */
fun GZIPOutputStream.writeBodyToStream(body: String) = this.use {
    it.write(body.toByteArray())
}

/**
 * Writes the provided request body to an `OutputStream`.
 *
 * This extension function enables writing a request body to a regular `OutputStream` for POST requests
 * without GZIP compression. It automatically manages the lifecycle of the stream using `use`.
 *
 * @param body The `String` body to write to the stream.
 */
fun OutputStream.writeBodyToStream(body: String) = this.use {
    it.write(body.toByteArray())
}

/**
 * Creates a configuration object for HTTP GET requests.
 *
 * This internal function returns an anonymous implementation of the `GetConfig` interface, which
 * includes a map of query parameters for the GET request. It is designed to be used internally
 * within the library to configure GET requests with optional query parameters.
 *
 * @param query A `Map<String, String>` representing the query parameters to include in the GET request URL. Defaults to an empty map.
 * @return An instance of `GetConfig` with the specified query parameters.
 */
internal fun createGetConfig(query: Map<String, String> = emptyMap()): GetConfig = object : GetConfig {
    override val query: Map<String, String> = query
}

/**
 * Creates a configuration object for HTTP POST requests.
 *
 * This internal function returns an anonymous implementation of the `PostConfig` interface, which
 * includes options for enabling GZIP compression and specifying a custom anonymous ID header. It
 * is designed to be used internally within the library to configure POST requests with optional GZIP
 * compression and custom headers.
 *
 * @param isGZIPEnabled A `Boolean` indicating whether GZIP compression is enabled for POST requests.
 * @param anonymousIdHeaderString A `String` representing a custom header for identifying anonymous users. Defaults to an empty string.
 * @return An instance of `PostConfig` with the specified GZIP setting and custom anonymous ID header.
 */
internal fun createPostConfig(isGZIPEnabled: Boolean, anonymousIdHeaderString: String = String.empty()): PostConfig =
    object : PostConfig {
        override val isGZIPEnabled: Boolean = isGZIPEnabled
        override val anonymousIdHeaderString: String = anonymousIdHeaderString
    }
