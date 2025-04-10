package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.network.ErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import com.rudderstack.sdk.kotlin.core.setupLogger
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileNotFoundException
import java.io.IOException

class EventUploadTest {
    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockHttpClient: HttpClient

    @MockK
    private lateinit var mockKotlinLogger: KotlinLogger

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var eventUpload: EventUpload

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        setupLogger(mockKotlinLogger)

        every { mockAnalytics.storage } returns mockStorage

        coEvery { mockStorage.close() } just runs
        coEvery { mockStorage.write(StorageKeys.EVENT, any<String>()) } just runs
        every { mockAnalytics.sourceConfigState } returns State(SourceConfig.initialState())
        every { mockAnalytics.sourceConfigState } returns State(
            SourceConfig(
                source = SourceConfig.initialState().source.copy(
                    isSourceEnabled = true
                )
            )
        )

        mockkStatic(::readFileAsString, ::doesFileExist)

        eventUpload = spyk(
            EventUpload(
                analytics = mockAnalytics,
                httpClientFactory = mockHttpClient,
            )
        )
    }

    @Test
    fun `test readFileAsString reads file correctly`() {
        val filePath = "test_file_path"
        val fileContent = "test content"

        every { readFileAsString(filePath) } returns fileContent

        val result = readFileAsString(filePath)

        assertEquals(fileContent, result)
    }

    @Test
    fun `given multiple batch is ready to be sent to the server and server returns success, when flush is called, then all the batches are sent to the server and removed from the storage`() =
        runTest {
            val storage = mockAnalytics.storage
            // Two batch files are ready to be sent
            val filePaths = listOf(
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
                "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
            )
            val fileUrlList = filePaths.joinToString(",")

            // Mock storage read
            coEvery {
                storage.readString(StorageKeys.EVENT, String.empty())
            } returns fileUrlList

            // Mock file existence check
            every { doesFileExist(any()) } returns true

            val batchPayload = "test content"

            // Mock messageQueue file reading
            filePaths.forEach { path ->
                every { readFileAsString(path) } returns batchPayload
            }

            // Mock the behavior for HttpClient
            every { mockHttpClient.sendData(batchPayload) } returns Result.Success("Ok")

            // Execute messageQueue actions
            eventUpload.start()
            eventUpload.flush()
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify the expected behavior
            filePaths.forEach { path ->
                verify(exactly = 1) { storage.remove(path) }
            }
        }

    @Test
    fun `given batches of events with different anonymousIds, when they are uploaded, then header is updated for each batch with different anonymousId`() {
        val storage = mockAnalytics.storage

        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        val batchPayload1 = "test content 1"
        val batchPayload2 = "test content 2"
        val anonymousId1 = "anonymousId1"
        val anonymousId2 = "anonymousId2"

        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList
        every { doesFileExist(any()) } returns true
        every { readFileAsString(filePaths[0]) } returns batchPayload1
        every { readFileAsString(filePaths[1]) } returns batchPayload2

        every { eventUpload.getAnonymousIdFromBatch(batchPayload1) } returns anonymousId1
        every { eventUpload.getAnonymousIdFromBatch(batchPayload2) } returns anonymousId2

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload1) } returns Result.Success("Ok")
        every { mockHttpClient.sendData(batchPayload2) } returns Result.Success("Ok")

        // Execute messageQueue actions
        eventUpload.start()
        eventUpload.flush()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockHttpClient.updateAnonymousIdHeaderString(anonymousId1.encodeToBase64())
            mockHttpClient.updateAnonymousIdHeaderString(anonymousId2.encodeToBase64())
        }
    }

    @ParameterizedTest
    @MethodSource("batchAnonymousIdTestProvider")
    fun `given a batch with some anonymousId, when it is uploaded, then header is updated with correct anonymousId`(
        batchPayload: String,
        anonymousIdFromBatch: String
    ) = runTest(testDispatcher) {
        val storage = mockAnalytics.storage

        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0"
        )
        val fileUrlList = filePaths.joinToString(",")

        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList
        every { doesFileExist(any()) } returns true
        every { readFileAsString(filePaths[0]) } returns batchPayload

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Success("Ok")

        val randomUUID = "some_random_id"
        mockkStatic(::generateUUID)
        every { generateUUID() } returns randomUUID

        // Execute messageQueue actions
        eventUpload.start()
        eventUpload.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        val encodedAnonymousId = anonymousIdFromBatch.encodeToBase64()
        coVerify(atLeast = 1) {
            mockHttpClient.updateAnonymousIdHeaderString(encodedAnonymousId)
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and server returns error, when flush is called, then the batch is not removed from storage`() {
        val storage = mockAnalytics.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { doesFileExist(any()) } returns true

        val batchPayload = "test content"

        // Mock messageQueue file reading
        filePaths.forEach { path ->
            every { readFileAsString(path) } returns batchPayload
        }

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Failure(
            ErrorStatus.ERROR_UNKNOWN,
            IOException("Internal Server Error")
        )

        // Execute messageQueue actions
        eventUpload.start()
        eventUpload.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the expected behavior
        filePaths.forEach { path ->
            verify(exactly = 0) { storage.remove(path) }
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and some exception occurs while reading the file, when flush is called, then the exception is thrown and handled`() {
        val storage = mockAnalytics.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { doesFileExist(any()) } returns true

        // Throw generic exception while reading the file
        val exception = Exception("File not found")
        filePaths.forEach { path ->
            every { readFileAsString(path) } throws exception
        }

        // Execute messageQueue actions
        eventUpload.start()
        eventUpload.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = filePaths.size) {
            mockKotlinLogger.error("Error when uploading event", exception)
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and file is not found, when flush is called, then the exception is thrown and handled`() {
        val storage = mockAnalytics.storage
        // Two batch files are ready to be sent
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
        )
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            storage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { doesFileExist(any()) } returns true

        // Throw file not found exception while reading the file
        val exception = FileNotFoundException("File not found")
        filePaths.forEach { path ->
            every { readFileAsString(path) } throws exception
        }

        // Execute messageQueue actions
        eventUpload.start()
        eventUpload.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = filePaths.size) {
            mockKotlinLogger.error("Message storage file not found", exception)
        }
    }

    companion object {

        @JvmStatic
        fun batchAnonymousIdTestProvider() = listOf(
            Arguments.of(
                """{"userId": "12345", "anonymousId": "abc-123", "event": "test"}""",
                "abc-123"
            ),
            Arguments.of(
                """{"userId": "12345", "event": "test", "anonymousId":"xyz-456"}""",
                "xyz-456"
            ),
            Arguments.of(
                """{"anonymousId": "lmn-789"}""",
                "lmn-789"
            ),
            Arguments.of(
                """{"userId": "12345", "event": "test"}""",
                "some_random_id"
            )
        )
    }
}
