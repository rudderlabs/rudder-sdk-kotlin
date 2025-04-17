package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger.LogLevel
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.Properties
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.connectivity.ConnectivityState
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
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
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
private const val NEW_EVENT_NAME = "New Event Name"

class AnalyticsTest {

    @MockK
    private lateinit var mockSourceConfigManager: SourceConfigManager

    @MockK
    private lateinit var mockAnalyticsConfiguration: AnalyticsConfiguration

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockConnectivityState: State<Boolean>

    private val mockCurrentTime = "<original-timestamp>"
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val configuration = provideConfiguration()
    private lateinit var mockAnalyticsJob: CompletableJob
    private lateinit var analytics: Analytics

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock LoggerAnalytics
        mockkObject(LoggerAnalytics)

        // This is needed to trigger analyticsJob.invokeOnCompletion()
        mockAnalyticsJob = SupervisorJob()

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
            every { connectivityState } returns mockConnectivityState
            every { analyticsJob } returns mockAnalyticsJob
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
    fun `when SDK is initialised, then connectivity state should be set to default state`() = runTest(testDispatcher) {
        disableSource()
        verify(exactly = 1) {
            mockConnectivityState.dispatch(match { action ->
                action is ConnectivityState.SetDefaultStateAction
            })
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
            disableSource()

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
            disableSource()

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
            disableSource()

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
            disableSource()

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
            disableSource()

            assertEquals(mockStorage, analytics.storage)
            coVerify(exactly = 1) {
                mockStorage.write(StorageKeys.EVENT, matchJsonString(expectedJsonString))
            }
        }

    private fun MockKVerificationScope.matchJsonString(expectedJsonString: String) =
        withArg<String> { actualJsonString ->
            JSONAssert.assertEquals(expectedJsonString, actualJsonString, true)
        }

    @ParameterizedTest
    @MethodSource("trackEventTestCases")
    fun `given SDK is ready to process any new events, when track events are made, then they are stored in storage`(
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
        disableSource()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @ParameterizedTest
    @MethodSource("screenEventTestCases")
    fun `given SDK is ready to process any new events, when screen events are made, then they are stored in storage`(
        screenName: String,
        category: String,
        properties: Properties,
        options: RudderOption,
    ) = runTest(testDispatcher) {
        analytics.screen(
            screenName = screenName,
            category = category,
            properties = properties,
            options = options,
        )
        testDispatcher.scheduler.runCurrent()
        disableSource()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @ParameterizedTest
    @MethodSource("groupEventTestCases")
    fun `given SDK is ready to process any new events, when group events are made, then they are stored in storage`(
        groupId: String,
        traits: JsonObject,
        options: RudderOption,
    ) = runTest(testDispatcher) {
        analytics.group(
            groupId = groupId,
            traits = traits,
            options = options,
        )
        testDispatcher.scheduler.runCurrent()
        disableSource()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @ParameterizedTest
    @MethodSource("identifyEventTestCases")
    fun `given SDK is ready to process any new events, when identify events are made, then they are stored in storage`(
        userId: String,
        traits: JsonObject,
        options: RudderOption,
    ) = runTest(testDispatcher) {
        analytics.identify(
            userId = userId,
            traits = traits,
            options = options,
        )
        testDispatcher.scheduler.runCurrent()
        disableSource()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @ParameterizedTest
    @MethodSource("aliasEventTestCases")
    fun `given SDK is ready to process any new events, when alias events are made, then they are stored in storage`(
        newId: String,
        previousId: String,
        options: RudderOption,
    ) = runTest(testDispatcher) {
        analytics.alias(
            newId = newId,
            previousId = previousId,
            options = options,
        )
        testDispatcher.scheduler.runCurrent()
        disableSource()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, any<String>())
        }
    }

    @Test
    fun `given SDK is ready to process any new events, when RESET call is made, then user details are reset`() {
        analytics.identify(userId = USER_ID, traits = TRAITS)
        analytics.reset()

        val userId = analytics.userId
        val traits = analytics.traits

        assertEquals(String.empty(), userId)
        assertEquals(emptyJsonObject, traits)
    }

    @Test
    fun `given analytics is shutdown, when events are called, then no event is stored in storage`() = runTest(testDispatcher) {
        analytics.shutdown()
        // Clear all mocks to avoid any previous calls
        clearMocks(mockStorage)

        analytics.track(TRACK_EVENT_NAME)
        analytics.screen(SCREEN_EVENT_NAME)
        analytics.group(GROUP_ID)
        analytics.identify(USER_ID)
        analytics.alias(ALIAS_ID)
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) {
            mockStorage.write(any(), any<String>())
        }
    }

    @Test
    fun `given there are few events that are yet to be processed, when shutdown is called, then all events in the queue are stored in storage then shutdown is completed`() =
        runTest(testDispatcher) {
            // Event pending to be processed
            analytics.track(TRACK_EVENT_NAME)
            analytics.screen(SCREEN_EVENT_NAME)
            analytics.group(GROUP_ID)
            analytics.identify(USER_ID)
            analytics.alias(ALIAS_ID)

            analytics.shutdown()
            // Process all the events
            testDispatcher.scheduler.runCurrent()
            disableSource()

            coVerify(exactly = 5) {
                mockStorage.write(StorageKeys.EVENT, any<String>())
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class, UseWithCaution::class)
    @Test
    fun `given writeKey is proper, when shutdown is called, then storage is closed but not deleted`() =
        runTest(testDispatcher) {
            every { mockAnalyticsConfiguration.isInvalidWriteKey } returns false

            analytics.shutdown()
            advanceUntilIdle()
            // Call this to execute invokeOnCompletion block
            mockAnalyticsJob.cancel()

            assertTrue(analytics.isAnalyticsShutdown)
            verify(exactly = 0) { mockStorage.delete() }
            verify(exactly = 1) { mockStorage.close() }
        }

    @OptIn(ExperimentalCoroutinesApi::class, UseWithCaution::class)
    @Test
    fun `given writeKey is not proper, when shutdown is called, then storage is deleted and closed`() =
        runTest(testDispatcher) {
            every { mockAnalyticsConfiguration.isInvalidWriteKey } returns true

            analytics.shutdown()
            advanceUntilIdle()
            // Call this to execute invokeOnCompletion block
            mockAnalyticsJob.cancel()

            assertTrue(analytics.isAnalyticsShutdown)
            verify(exactly = 1) {
                mockStorage.close()
                mockStorage.delete()
            }
            verifyOrder {
                mockStorage.close()
                mockStorage.delete()
            }
        }

    @Test
    fun `when custom plugin is dynamically added, then it should intercept the message and process event`() = runTest(testDispatcher) {
        val customPlugin = provideCustomPlugin()

        analytics.add(customPlugin)
        analytics.track(TRACK_EVENT_NAME)
        testDispatcher.scheduler.runCurrent()
        disableSource()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, withArg<String> { eventString ->
                assertTrue(eventString.contains(NEW_EVENT_NAME), "Event string should contain '$NEW_EVENT_NAME'")
            })
        }
    }

    @Test
    fun `when custom plugin is dynamically removed, then it shouldn't intercept the message and process event`() = runTest(testDispatcher) {
        val customPlugin = provideCustomPlugin()
        analytics.add(customPlugin)

        analytics.remove(customPlugin)
        analytics.track(TRACK_EVENT_NAME)
        testDispatcher.scheduler.runCurrent()
        disableSource()

        coVerify(exactly = 1) {
            mockStorage.write(StorageKeys.EVENT, withArg<String> { eventString ->
                assertTrue(eventString.contains(TRACK_EVENT_NAME))
            })
        }
    }

    private fun disableSource() {
        analytics.sourceConfigState.dispatch(
            SourceConfig.UpdateAction(
                SourceConfig(
                    source = SourceConfig.initialState().source.copy(isSourceEnabled = false)
                )
            )
        )
    }

    companion object {
        @JvmStatic
        fun trackEventTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(TRACK_EVENT_NAME, emptyJsonObject, RudderOption()),
            Arguments.of(TRACK_EVENT_NAME, provideSampleJsonPayload(), RudderOption()),
            Arguments.of(TRACK_EVENT_NAME, emptyJsonObject, provideRudderOption()),
        )

        @JvmStatic
        fun screenEventTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(SCREEN_EVENT_NAME, String.empty(), emptyJsonObject, RudderOption()),
            Arguments.of(SCREEN_EVENT_NAME, SCREEN_CATEGORY, emptyJsonObject, RudderOption()),
            Arguments.of(SCREEN_EVENT_NAME, SCREEN_CATEGORY, provideSampleJsonPayload(), RudderOption()),
            Arguments.of(SCREEN_EVENT_NAME, String.empty(), provideSampleJsonPayload(), RudderOption()),
            Arguments.of(SCREEN_EVENT_NAME, String.empty(), emptyJsonObject, provideRudderOption()),
        )

        @JvmStatic
        fun groupEventTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(GROUP_ID, emptyJsonObject, RudderOption()),
            Arguments.of(GROUP_ID, provideSampleJsonPayload(), RudderOption()),
            Arguments.of(GROUP_ID, emptyJsonObject, provideRudderOption()),
        )

        @JvmStatic
        fun identifyEventTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(String.empty(), emptyJsonObject, RudderOption()),
            Arguments.of(USER_ID, emptyJsonObject, RudderOption()),
            Arguments.of(USER_ID, provideSampleJsonPayload(), RudderOption()),
            Arguments.of(USER_ID, emptyJsonObject, provideRudderOption()),
        )

        @JvmStatic
        fun aliasEventTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(ALIAS_ID, String.empty(), RudderOption()),
            Arguments.of(ALIAS_ID, PREVIOUS_ID, RudderOption()),
            Arguments.of(ALIAS_ID, String.empty(), RudderOption()),
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

private fun provideCustomPlugin() = object : Plugin {
    override val pluginType: Plugin.PluginType = Plugin.PluginType.OnProcess
    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event? {
        if (event is TrackEvent) {
            event.event = NEW_EVENT_NAME
        }
        return super.intercept(event)
    }
}
