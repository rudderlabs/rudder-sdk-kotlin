package internals.web.provider

import com.rudderstack.core.internals.web.HttpURLConnectionFactory
import com.rudderstack.core.internals.web.WebServiceImpl
import java.net.HttpURLConnection

fun provideWebServiceImpl(
    baseUrl: String = "https://api.example.com",
    authHeaderString: String = "auth-token",
    isGZIPEnabled: Boolean = false,
    anonymousIdHeaderString: String = "anonymousId",
    connectionFactory: HttpURLConnectionFactory,
) = WebServiceImpl(
    baseUrl = baseUrl,
    authHeaderString = authHeaderString,
    isGZIPEnabled = isGZIPEnabled,
    anonymousIdHeaderString = anonymousIdHeaderString,
    connectionFactory = connectionFactory,
)

fun provideErrorMessage(status: Int, connection: HttpURLConnection, msg: String = "Some error occurred")
 = "HTTP $status, URL: ${connection.url}, Error: $msg"
