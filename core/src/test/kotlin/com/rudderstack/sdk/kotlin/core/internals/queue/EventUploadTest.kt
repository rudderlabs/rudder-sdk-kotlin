package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.MaxAttemptsWithBackoff
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.JsonSentAtUpdater
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.internals.utils.handleInvalidWriteKey
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import com.rudderstack.sdk.kotlin.core.readFileTrimmed
import com.rudderstack.sdk.kotlin.core.setupLogger
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

private const val batchPayload = "test content"
private const val batchPayload1 = "test content 1"
private const val batchPayload2 = "test content 2"
private const val anonymousId1 = "anonymousId1"
private const val anonymousId2 = "anonymousId2"

private const val mockCurrentTime = "<original-timestamp>"
private const val unprocessedBatchWithTwoEvents = "message/batch/unprocessed_batch_with_two_events.json"
private const val processedBatchWithTwoEvents = "message/batch/processed_batch_with_two_events.json"

private const val MAX_ATTEMPT = 5

class EventUploadTest {

    // Two batch files are ready to be sent
    private val filePaths = listOf(
        "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0",
        "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-1"
    )
    private val singleFilePath = filePaths[0]

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockHttpClient: HttpClient

    @MockK
    private lateinit var mockKotlinLogger: KotlinLogger

    @MockK
    private lateinit var mockMaxAttemptsWithBackoff: MaxAttemptsWithBackoff

    private val listOfTimeStamp = listOf(
        "1970-01-01T00:00:00Z",
        "1970-01-01T00:00:01Z",
        "1970-01-01T00:00:02Z",
        "1970-01-01T00:00:03Z",
        "1970-01-01T00:00:04Z",
        "1970-01-01T00:00:05Z"
    )

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

        mockkObject(DateTimeUtils)
        every { DateTimeUtils.now() } returns mockCurrentTime

        eventUpload = spyk(
            EventUpload(
                analytics = mockAnalytics,
                httpClientFactory = mockHttpClient,
                maxAttemptsWithBackoff = mockMaxAttemptsWithBackoff,
            )
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(DateTimeUtils)
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
            val unprocessedBatch = readFileTrimmed(unprocessedBatchWithTwoEvents)
            val processedBatch = readFileTrimmed(processedBatchWithTwoEvents)
            prepareMultipleBatch()
            // Mock messageQueue file reading
            filePaths.forEach { path ->
                every { readFileAsString(path) } returns unprocessedBatch
            }
            // Mock the behavior for HttpClient
            every { mockHttpClient.sendData(any()) } returns Result.Success("Ok")

            processMessage()

            // Verify the expected behavior
            filePaths.forEach { path ->
                verify(exactly = 1) { mockStorage.remove(path) }
            }
            verify(exactly = 2) {
                mockHttpClient.sendData(processedBatch)
            }
        }

    @Test
    fun `given batches of events with different anonymousIds, when they are uploaded, then header is updated for each batch with different anonymousId`() {
        prepareMultipleBatch()
        every { readFileAsString(filePaths[0]) } returns batchPayload1
        every { readFileAsString(filePaths[1]) } returns batchPayload2
        every { eventUpload.getAnonymousIdFromBatch(batchPayload1) } returns anonymousId1
        every { eventUpload.getAnonymousIdFromBatch(batchPayload2) } returns anonymousId2
        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload1) } returns Result.Success("Ok")
        every { mockHttpClient.sendData(batchPayload2) } returns Result.Success("Ok")

        processMessage()

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
        val filePaths = listOf(
            "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/<WRITE_KEY>-0"
        )
        val fileUrlList = filePaths.joinToString(",")

        coEvery {
            mockStorage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList
        every { doesFileExist(any()) } returns true
        every { readFileAsString(filePaths[0]) } returns batchPayload

        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Success("Ok")

        val randomUUID = "some_random_id"
        mockkStatic(::generateUUID)
        every { generateUUID() } returns randomUUID

        processMessage()

        val encodedAnonymousId = anonymousIdFromBatch.encodeToBase64()
        coVerify(atLeast = 1) {
            mockHttpClient.updateAnonymousIdHeaderString(encodedAnonymousId)
        }
    }

    @Test
    fun `given batch is ready to be sent to the server and server initially returns retry able error, when flush is called, then the batch is removed at the last after success is received`() =
        runTest {
            prepareSingleBatch(batchPayload)
            simulateRetryAbleError()

            processMessage()

            // Once the batch is sent successfully, the file should be removed from storage
            verify(exactly = 1) { mockStorage.remove(singleFilePath) }
            coVerify(exactly = MAX_ATTEMPT) {
                mockMaxAttemptsWithBackoff.delayWithBackoff()
            }
            // There's one explicit `reset` call after the success
            verify(exactly = 1) {
                mockMaxAttemptsWithBackoff.reset()
            }
        }

    @Test
    fun `given retry attempt is made multiple times, when flush is called, then the sentAt is updated`() {
        val unprocessedBatch = readFileTrimmed(unprocessedBatchWithTwoEvents)
        prepareSingleBatch(batchPayload = unprocessedBatch)
        val updatedBatchList: MutableList<String> = getBatchWithUpdatedSentAtTimeStamp(
            unprocessedBatch = unprocessedBatch,
            listOfTimeStamp = listOfTimeStamp,
        )
        // Reset the timestamp mock
        every { DateTimeUtils.now() } returnsMany listOfTimeStamp
        simulateRetryAbleError(maxAttempt = MAX_ATTEMPT)

        processMessage()

        // The batch should be sent with the updated `sentAt` timestamp
        updatedBatchList.forEach { payload ->
            verify(exactly = 1) { mockHttpClient.sendData(payload) }
        }
        // Once the batch is sent successfully, the file should be removed from storage
        verify(exactly = 1) { mockStorage.remove(singleFilePath) }
        // There's one explicit `reset` call after the success
        verify(exactly = 1) { mockMaxAttemptsWithBackoff.reset() }
    }

    @Test
    fun `given some exception is thrown while updating sentAt, when flush is called, file is removed but without any upload attempt`() {
        val unprocessedBatch = readFileTrimmed(unprocessedBatchWithTwoEvents)
        prepareSingleBatch(batchPayload = unprocessedBatch)
        every { DateTimeUtils.now() } throws Exception()
        simulateRetryAbleError(maxAttempt = MAX_ATTEMPT)

        processMessage()

        // Once the batch is sent successfully, the file should be removed from storage
        verify(exactly = 1) { mockStorage.remove(singleFilePath) }
        verify(exactly = 0) { mockHttpClient.sendData(any()) }
    }

    @Test
    fun `given batch is ready to be sent to the server and some exception occurs while reading the file, when flush is called, then the exception handled and file gets removed from the storage`() {
        prepareMultipleBatch()
        val exception = Exception("File not found")
        filePaths.forEach { path ->
            every { readFileAsString(path) } throws exception
        }

        processMessage()

        filePaths.forEach { path ->
            verify(exactly = 1) { mockStorage.remove(path) }
        }
    }

    @Test
    fun `given server returns 400, when flush is called, then the batch is removed from the storage`() = runTest {
        val unprocessedBatch = readFileTrimmed(unprocessedBatchWithTwoEvents)
        every { mockStorage.readString(StorageKeys.EVENT, String.empty()) } returns singleFilePath
        every { doesFileExist(singleFilePath) } returns true
        every { readFileAsString(singleFilePath) } returns unprocessedBatch
        every { mockHttpClient.sendData(any()) } returns Result.Failure(
            error = NetworkErrorStatus.Error400,
        )

        processMessage()

        verify(exactly = 1) { mockStorage.remove(singleFilePath) }
    }

    @Test
    fun `given server returns source is disabled as error, when flush is called, then the upload queue is stopped`() {
        prepareMultipleBatch()
        // Mock messageQueue file reading
        filePaths.forEach { path ->
            every { readFileAsString(path) } returns batchPayload
        }
        // Mock the behavior for HttpClient
        every { mockHttpClient.sendData(batchPayload) } returns Result.Failure(
            error = NetworkErrorStatus.Error404,
        )

        processMessage()

        // Verify network attempt is made and event is not removed from storage
        verify(exactly = 1) { mockHttpClient.sendData(batchPayload) }
        filePaths.forEach { path ->
            verify(exactly = 0) { mockStorage.remove(path) }
        }

        clearMocks(mockHttpClient)

        // Re-attempting to flush the same batch
        eventUpload.flush()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify this time network attempt is not made again
        verify(exactly = 0) { mockHttpClient.sendData(batchPayload) }
    }

    @Test
    fun `given server returns 413, when flush is called, then the batch is removed from the storage`() = runTest {
        val unprocessedBatch = readFileTrimmed(unprocessedBatchWithTwoEvents)
        every { mockStorage.readString(StorageKeys.EVENT, String.empty()) } returns singleFilePath
        every { doesFileExist(singleFilePath) } returns true
        every { readFileAsString(singleFilePath) } returns unprocessedBatch
        every { mockHttpClient.sendData(any()) } returns Result.Failure(
            error = NetworkErrorStatus.Error413,
        )

        processMessage()

        verify(exactly = 1) { mockStorage.remove(singleFilePath) }
    }

    @OptIn(UseWithCaution::class)
    @Test
    fun `given server returns 401, when flush is called, then the invalid write key process is initiated`() = runTest {
        val unprocessedBatch = readFileTrimmed(unprocessedBatchWithTwoEvents)
        every { mockStorage.readString(StorageKeys.EVENT, String.empty()) } returns singleFilePath
        every { doesFileExist(singleFilePath) } returns true
        every { readFileAsString(singleFilePath) } returns unprocessedBatch
        every { mockHttpClient.sendData(any()) } returns Result.Failure(
            error = NetworkErrorStatus.Error401,
        )

        processMessage()

        verify(exactly = 1) {
            eventUpload.cancel()
            mockAnalytics.handleInvalidWriteKey()
        }
    }

    private fun prepareMultipleBatch() {
        val fileUrlList = filePaths.joinToString(",")

        // Mock storage read
        coEvery {
            mockStorage.readString(StorageKeys.EVENT, String.empty())
        } returns fileUrlList

        // Mock file existence check
        every { doesFileExist(any()) } returns true
    }

    private fun prepareSingleBatch(batchPayload: String) {
        coEvery {
            mockStorage.readString(StorageKeys.EVENT, String.empty())
        } returns singleFilePath
        every { doesFileExist(any()) } returns true
        every { readFileAsString(singleFilePath) } returns batchPayload
    }

    private fun simulateRetryAbleError(maxAttempt: Int = MAX_ATTEMPT) {
        var callCount = 0
        every { mockHttpClient.sendData(any()) } answers {
            callCount++
            when {
                callCount <= maxAttempt -> Result.Failure(NetworkErrorStatus.ErrorUnknown)
                // This else block is needed to stop the infinite loop
                else -> Result.Success("Ok")
            }
        }
    }

    private fun processMessage() {
        // Execute messageQueue actions
        eventUpload.start()
        eventUpload.flush()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun getBatchWithUpdatedSentAtTimeStamp(
        unprocessedBatch: String,
        listOfTimeStamp: List<String>
    ): MutableList<String> {
        val totalAttempts: Int = listOfTimeStamp.size
        every { DateTimeUtils.now() } returnsMany listOfTimeStamp
        return mutableListOf<String>().apply {
            repeat(totalAttempts) {
                add(JsonSentAtUpdater.updateSentAt(unprocessedBatch))
            }
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
