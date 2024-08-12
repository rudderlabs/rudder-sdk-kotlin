package internals.web.provider

import com.rudderstack.core.Constants.DEFAULT_GZIP_STATUS
import com.rudderstack.core.internals.web.HttpURLConnectionFactory
import com.rudderstack.core.internals.web.WebServiceImpl
import java.net.HttpURLConnection

fun provideWebServiceImplForGetRequest(
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
) = WebServiceImpl.getInstance(
    baseUrl = baseUrl,
    endPoint = endPoint,
    authHeaderString = authHeaderString,
    query = query,
    customHeaders = customHeaders,
    connectionFactory = connectionFactory,
)

fun provideWebServiceImplForPostRequest(
    connectionFactory: HttpURLConnectionFactory,
    baseUrl: String = "https://api.example.com",
    endPoint: String = "/test",
    authHeaderString: String = "auth-header",
    isGZIPEnabled: Boolean = DEFAULT_GZIP_STATUS,
    anonymousIdHeaderString: String = "anonymous-id",
    customHeaders: Map<String, String> = emptyMap(),
) = WebServiceImpl.postInstance(
    baseUrl = baseUrl,
    endPoint = endPoint,
    authHeaderString = authHeaderString,
    isGZIPEnabled = isGZIPEnabled,
    anonymousIdHeaderString = anonymousIdHeaderString,
    customHeaders = customHeaders,
    connectionFactory = connectionFactory,
)

fun provideErrorMessage(status: Int, connection: HttpURLConnection, msg: String = "Some error occurred")
 = "HTTP $status, URL: ${connection.url}, Error: $msg"
