package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger.LogLevel
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import io.mockk.MockKAnnotations
import io.mockk.MockKVerificationScope
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.skyscreamer.jsonassert.JSONAssert
import java.util.stream.Stream

private val TRAITS: JsonObject = buildJsonObject { put("key-1", "value-1") }
private const val trackPayloadPath = "messageWitContextObject/track_with_all_arguments_from_server.json"
private const val screenPayloadPath = "messageWitContextObject/screen_with_all_arguments_from_server.json"
private const val groupPayloadPath = "messageWitContextObject/group_with_all_arguments_from_server.json"
private const val identifyPayloadPath = "messageWitContextObject/identify_events_with_all_arguments_from_server.json"
private const val aliasPayloadPath = "messageWitContextObject/alias_events_with_all_arguments_from_server.json"

private const val TRACK_EVENT_NAME = "Track event 1"
private const val SCREEN_EVENT_NAME = "Test Screen 1"
private const val SCREEN_CATEGORY = "Main"
private const val GROUP_ID = "Group Id 1"
private const val USER_ID = "User Id 1"
private const val ALIAS_ID = "Alias Id 1"
private const val PREVIOUS_ID = "Previous Id 1"

class AnalyticsTest {

    @MockK
    private lateinit var mockSourceConfigManager: SourceConfigManager

    @MockK
    private lateinit var mockAnalyticsConfiguration: AnalyticsConfiguration

    @MockK
    private lateinit var mockStorage: Storage

    private val mockCurrentTime = "<original-timestamp>"
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val configuration = provideConfiguration()
    private lateinit var analytics: Analytics

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock LoggerAnalytics
        mockkObject(LoggerAnalytics)

        // Mock Analytics Configuration
        mockkStatic(::provideAnalyticsConfiguration)
        every { provideAnalyticsConfiguration(any()) } returns mockAnalyticsConfiguration
        mockAnalyticsConfiguration.apply {
            every { analyticsScope } returns testScope
            every { analyticsDispatcher } returns testDispatcher
            every { storageDispatcher } returns testDispatcher
            every { networkDispatcher } returns testDispatcher

            // Mock SourceConfig
            mockkStatic(::provideSourceConfigManager)
            every { provideSourceConfigManager(any(), any()) } returns mockSourceConfigManager
            every { sourceConfigManager } returns mockSourceConfigManager

            every { storage } returns mockStorage
        }

        // Mocking persisted values and assigning default values
        every { mockStorage.readString(StorageKeys.ANONYMOUS_ID, defaultVal = any()) } returns ANONYMOUS_ID
        every { mockStorage.getLibraryVersion() } returns provideLibraryVersion()

        // Mock util method
        mockkStatic(::generateUUID)
        every { generateUUID() } returns MESSAGE_ID
        mockkObject(DateTimeUtils)
        every { DateTimeUtils.now() } returns mockCurrentTime

        // Analytics instance
        analytics = spyk(Analytics(configuration = configuration))
    }

    @Test
    fun `when anonymousId is fetched, then it should return UUID as the anonymousId`() {
        every { mockStorage.readString(StorageKeys.ANONYMOUS_ID, defaultVal = any()) } returns UUID
        analytics = spyk(Analytics(configuration = configuration))

        val anonymousId = analytics.anonymousId

        // This pattern ensures the string follows the UUID v4 format.
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertTrue(anonymousId?.matches(uuidRegex) == true)
    }

    @Test
    fun `given sdk is shutdown, when anonymousId is fetched, then it should return null`() {
        analytics.shutdown()

        val anonymousId = analytics.anonymousId

        assertNull(anonymousId)
    }

    @Test
    fun `given userId and traits are set, when they are fetched, then the set values are returned`() {
        // userId and traits can be set only through identify api
        analytics.identify(userId = USER_ID, traits = TRAITS)

        val userId = analytics.userId
        val traits = analytics.traits

        assertEquals(USER_ID, userId)
        assertEquals(TRAITS, traits)
    }

    @Test
    fun `when identify is called on an anonymous user, then reset is not called`() {
        analytics.identify(userId = USER_ID, traits = TRAITS)

        verify(exactly = 0) { analytics.reset() }
    }

    @Test
    fun `when identify is called with same userId on an identified user, then reset is not called`() {
        analytics.identify(userId = USER_ID, traits = TRAITS)
        analytics.identify(userId = USER_ID, traits = TRAITS)

        verify(exactly = 0) { analytics.reset() }
    }

    @Test
    fun `when identify is called with different userId on an identified user, then reset is called`() {
        analytics.identify(userId = USER_ID, traits = TRAITS)
        analytics.identify(userId = "new-user-id", traits = TRAITS)

        verify(exactly = 1) { analytics.reset() }
    }

    @Test
    fun `given userId and traits are not set, when they are fetched, then empty values are returned`() {
        val userId = analytics.userId
        val traits = analytics.traits

        assertEquals(String.empty(), userId)
        assertEquals(emptyJsonObject, traits)
    }

    @Test
    fun `given sdk is shutdown, when userId and traits are fetched, then it should return null`() {
        analytics.shutdown()

        val userId = analytics.userId
        val traits = analytics.traits

        assertNull(userId)
        assertNull(traits)
    }

    @Test
    fun `when SDK is initialised, then KotlinLogger and default log level should be set`() {
        verify(exactly = 1) {
            LoggerAnalytics.setup(any<KotlinLogger>(), LogLevel.NONE)
        }
    }

    @Test
    fun `when SDK is initialised, then SourceConfigManager should be initialised and source config observers should be notified`() {
        assertNotNull(analytics.sourceConfigManager)
        assertEquals(mockSourceConfigManager, analytics.sourceConfigManager)
        verify(exactly = 1) {
            mockSourceConfigManager.fetchCachedSourceConfigAndNotifyObservers()
            mockSourceConfigManager.refreshSourceConfigAndNotifyObservers()
        }
    }

    // Events with all the parameters

    @Test
    fun `given SDK is ready to process any new events, when a track event is made, then it should be stored in the storage`() =
        runTest(testDispatcher) {
            val expectedJsonString = readFileTrimmed(trackPayloadPath)

            analytics.track(
                name = TRACK_EVENT_NAME,
                properties = provideSampleJsonPayload(),
                options = provideRudderOption(),
            )
            testDispatcher.scheduler.runCurrent()

            assertEquals(mockStorage, analytics.storage)

            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, matchJsonString(expectedJsonString))
            }
        }

    @Test
    fun `given SDK is ready to process any new events, when a screen event is made, then it should be stored in the storage`() =
        runTest(testDispatcher) {
            val expectedJsonString = readFileTrimmed(screenPayloadPath)

            analytics.screen(
                screenName = SCREEN_EVENT_NAME,
                category = SCREEN_CATEGORY,
                properties = provideSampleJsonPayload(),
                options = provideRudderOption(),
            )
            testDispatcher.scheduler.runCurrent()

            assertEquals(mockStorage, analytics.storage)
            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, matchJsonString(expectedJsonString))
            }
        }

    @Test
    fun `given SDK is ready to process any new events, when a group event is made, then it should be stored in the storage`() =
        runTest(testDispatcher) {
            val expectedJsonString = readFileTrimmed(groupPayloadPath)

            analytics.group(
                groupId = GROUP_ID,
                traits = provideSampleJsonPayload(),
                options = provideRudderOption(),
            )
            testDispatcher.scheduler.runCurrent()

            assertEquals(mockStorage, analytics.storage)
            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, matchJsonString(expectedJsonString))
            }
        }

    @Test
    fun `given SDK is ready to process any new events, when an identify event is made, then it should be stored in the storage`() =
        runTest(testDispatcher) {
            val expectedJsonString = readFileTrimmed(identifyPayloadPath)

            analytics.identify(
                userId = USER_ID,
                traits = provideSampleJsonPayload(),
                options = provideRudderOption(),
            )
            testDispatcher.scheduler.runCurrent()

            assertEquals(mockStorage, analytics.storage)
            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, matchJsonString(expectedJsonString))
            }
        }

    @Test
    fun `given SDK is ready to process any new events, when an alias event is made, then it should be stored in the storage`() =
        runTest(testDispatcher) {
            val expectedJsonString = readFileTrimmed(aliasPayloadPath)

            analytics.alias(
                newId = ALIAS_ID,
                previousId = PREVIOUS_ID,
                options = provideRudderOption(),
            )
            testDispatcher.scheduler.runCurrent()

            assertEquals(mockStorage, analytics.storage)
            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, matchJsonString(expectedJsonString))
            }
        }

    // Track events with different arguments

    @ParameterizedTest
    @MethodSource("trackEventTestCases")
    fun `given SDK is ready to process any new events, when track events are made, then event is stored in storage`(
        name: String,
        properties: JsonObject,
        options: RudderOption,
    ) = runTest(testDispatcher) {
        analytics.track(
            name = name,
            properties = properties,
            options = options,
        )
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    // Screen events with different arguments

    @Test
    fun `given SDK is ready to process any new events, when screen is called with only screen name, then event is stored in storage`() = runTest(testDispatcher) {
        analytics.screen(SCREEN_EVENT_NAME)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when screen is called with screen name and category, then event with category is stored in storage`() = runTest(testDispatcher) {
        analytics.screen(SCREEN_EVENT_NAME, SCREEN_CATEGORY)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when screen is called with screen name, category and properties, then event with category and properties is stored in storage`() = runTest(testDispatcher) {
        analytics.screen(SCREEN_EVENT_NAME, SCREEN_CATEGORY, provideSampleJsonPayload())
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when screen is called with screen name and options, then event with options is stored in storage`() = runTest(testDispatcher) {
        analytics.screen(SCREEN_EVENT_NAME, options = provideRudderOption())
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    // Group events with different arguments

    @Test
    fun `given SDK is ready to process any new events, when group is called with only groupId, then event is stored in storage`() = runTest(testDispatcher) {
        analytics.group(GROUP_ID)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when group is called with groupId and traits, then event with traits is stored in storage`() = runTest(testDispatcher) {
        analytics.group(GROUP_ID, provideSampleJsonPayload())
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when group is called with groupId and options, then event with options is stored in storage`() = runTest(testDispatcher) {
        analytics.group(GROUP_ID, options = provideRudderOption())
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    // Identify events with different arguments

    @Test
    fun `given SDK is ready to process any new events, when identify is called without params, then event is stored in storage`() = runTest(testDispatcher) {
        analytics.identify()
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when identify is called with only userId, then event with userId is stored in storage`() = runTest(testDispatcher) {
        analytics.identify(USER_ID)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when identify is called with userId and traits, then event with traits is stored in storage`() = runTest(testDispatcher) {
        analytics.identify(USER_ID, provideSampleJsonPayload())
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when identify is called with userId and options, then event with options is stored in storage`() = runTest(testDispatcher) {
        analytics.identify(USER_ID, options = provideRudderOption())
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    // Alias events with different arguments

    @Test
    fun `given SDK is ready to process any new events, when alias is called with required newId, then event is stored in storage`() = runTest(testDispatcher) {
        analytics.alias(ALIAS_ID)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when alias is called with newId and previousId, then event with both IDs is stored in storage`() = runTest(testDispatcher) {
        analytics.alias(ALIAS_ID, PREVIOUS_ID)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when alias is called with newId and options, then event with options is stored in storage`() = runTest(testDispatcher) {
        analytics.alias(ALIAS_ID, options = provideRudderOption())
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

//    @Test
//    fun `given analytics is shutdown, when alias is called, then no event is stored in storage`() = runTest(testDispatcher) {
//        analytics.shutdown()
//
//        analytics.alias(ALIAS_ID)
//        testDispatcher.scheduler.runCurrent()
//
//        coVerify(exactly = 0) {
//            mockStorage.write(any(), any<String>())
//        }
//    }

    private fun MockKVerificationScope.matchJsonString(expectedJsonString: String) =
        withArg<String> { actualJsonString ->
            JSONAssert.assertEquals(expectedJsonString, actualJsonString, true)
        }

    companion object {
        @JvmStatic
        fun trackEventTestCases() = Stream.of(
            Arguments.of(TRACK_EVENT_NAME, emptyJsonObject, RudderOption()),
            Arguments.of(TRACK_EVENT_NAME, provideSampleJsonPayload(), RudderOption()),
            Arguments.of(TRACK_EVENT_NAME, emptyJsonObject, provideRudderOption()),
        )
    }
}

private fun provideConfiguration() =
    Configuration(
        writeKey = "<writeKey>",
        dataPlaneUrl = "<data_plane_url>",
    )

private fun provideLibraryVersion(): LibraryVersion {
    return object : LibraryVersion {
        override fun getPackageName(): String = "com.rudderstack.kotlin.sdk"
        override fun getVersionName(): String = "1.0.0"
    }
}

