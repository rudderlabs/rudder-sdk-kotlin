package internals.web

import com.rudderstack.core.internals.utils.getErrorResponse
import com.rudderstack.core.internals.utils.getSuccessResponse
import com.rudderstack.core.internals.web.ErrorStatus
import com.rudderstack.core.internals.web.Failure
import com.rudderstack.core.internals.web.HttpURLConnectionFactory
import com.rudderstack.core.internals.web.Result
import com.rudderstack.core.internals.web.Success
import com.rudderstack.core.internals.web.WebServiceImpl
import internals.web.provider.provideErrorMessage
import internals.web.provider.provideWebServiceImpl
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException

private const val ENDPOINT = "/endpoint"
private const val SUCCESS_RESPONSE = "Success Response"
private const val ERROR_RESPONSE = "Some error occurred"

private const val REQUEST_BODY = "body"

private const val s = REQUEST_BODY

class WebServiceImplTest {

    @MockK
    private lateinit var mockConnectionFactory: HttpURLConnectionFactory

    @MockK
    private lateinit var mockConnection: HttpURLConnection

    private lateinit var webService: WebServiceImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockConnectionFactory.createConnection(any()) } returns mockConnection

        // Mock extension functions
        mockkStatic("com.rudderstack.core.internals.utils.WebUtilsKt")
        every { mockConnection.getSuccessResponse() } returns SUCCESS_RESPONSE
        every { mockConnection.getErrorResponse() } returns ERROR_RESPONSE
    }

    @Test
    fun `given connection is successful, when getData is called, then return Success`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 200

        val result = webService.getData(ENDPOINT)

        assertSuccess(result)
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is successful and it returns 201, when getData is called, then return Success`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 201

        val result = webService.getData(ENDPOINT)

        assertSuccess(result)
        verify { mockConnection.connect() }
    }

    @Test(expected = MalformedURLException::class)
    fun `given wrong url, when getData is called, then throw MalformedURLException`() {
        webService = provideWebServiceImpl(
            baseUrl = "wrong-url",
            connectionFactory = mockConnectionFactory,
        )

        webService.getData(ENDPOINT)
    }

    @Test
    fun `given connection is unsuccessful, when getData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.connect() } throws IOException(ERROR_RESPONSE)

        val result = webService.getData(ENDPOINT)

        assertFailure(result, ErrorStatus.ERROR, IOException(), ERROR_RESPONSE)
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 400, when getData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 400

        val result = webService.getData(ENDPOINT)

        assertFailure(result, ErrorStatus.BAD_REQUEST, IOException(), provideErrorMessage(400, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 404, when getData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 404

        val result = webService.getData(ENDPOINT)

        assertFailure(result, ErrorStatus.RESOURCE_NOT_FOUND, IOException(), provideErrorMessage(404, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 5XX, when getData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 500

        val result = webService.getData(ENDPOINT)

        assertFailure(result, ErrorStatus.RETRY_ABLE, IOException(), provideErrorMessage(500, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 429, when getData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 429

        val result = webService.getData(ENDPOINT)

        assertFailure(result, ErrorStatus.RETRY_ABLE, IOException(), provideErrorMessage(429, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 4XX, when getData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 450

        val result = webService.getData(ENDPOINT)

        assertFailure(result, ErrorStatus.ERROR, IOException(), provideErrorMessage(450, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given invalid write key, when getData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 401

        val result = webService.getData(ENDPOINT)

        assertFailure(result, ErrorStatus.INVALID_WRITE_KEY, IOException(), provideErrorMessage(401, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is successful, when sendData is called, then return Success`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 200

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertSuccess(result)
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is successful and it returns 201, when sendData is called, then return Success`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 201

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertSuccess(result)
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful, when sendData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.connect() } throws IOException(ERROR_RESPONSE)

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertFailure(result, ErrorStatus.ERROR, IOException(), ERROR_RESPONSE)
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 400, when sendData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 400

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertFailure(result, ErrorStatus.BAD_REQUEST, IOException(), provideErrorMessage(400, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 404, when sendData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 404

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertFailure(result, ErrorStatus.RESOURCE_NOT_FOUND, IOException(), provideErrorMessage(404, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 5XX, when sendData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 500

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertFailure(result, ErrorStatus.RETRY_ABLE, IOException(), provideErrorMessage(500, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 429, when sendData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 429

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertFailure(result, ErrorStatus.RETRY_ABLE, IOException(), provideErrorMessage(429, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given connection is unsuccessful and it returns 4XX, when sendData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 450

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertFailure(result, ErrorStatus.ERROR, IOException(), provideErrorMessage(450, mockConnection))
        verify { mockConnection.connect() }
    }

    @Test
    fun `given invalid write key, when sendData is called, then return Failure`() {
        webService = provideWebServiceImpl(connectionFactory = mockConnectionFactory)
        every { mockConnection.responseCode } returns 401

        val result = webService.sendData(ENDPOINT, REQUEST_BODY)

        assertFailure(result, ErrorStatus.INVALID_WRITE_KEY, IOException(), provideErrorMessage(401, mockConnection))
        verify { mockConnection.connect() }
    }




    /**
     * post -> success
     * post -> status code 400, 500
     * post -> exception
     *
     */


    private fun assertSuccess(result: Result<String>) {
        assertTrue(result is Success)
        verify { mockConnection.disconnect() }
    }

    private fun assertFailure(result: Result<String>, expectedStatus: ErrorStatus, expectedException: Exception, expectedErrorMessage: String) {
        assertTrue(result is Failure)
        verify { mockConnection.disconnect() }

        result as Failure // Safe cast after assertTrue

        assertEquals(expectedStatus, result.status)
        assertEquals(expectedErrorMessage, result.error.message)
        assertThrows(expectedException::class.java) { throw result.error }
    }
}
