package com.rudderstack.sdk.kotlin.core.server

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

private const val USER_1 = "user-1"
private const val USER_2 = "user-2"
private const val ANONYMOUS_2 = "anonymous-2"
private const val BLANK = " "
private val TRAITS = buildJsonObject { put("plan", "enterprise") }

class ServerAnalyticsTest {

    private val mockDropListener: DropListener = mockk(relaxed = true)
    private val mockAnalytics: Analytics = mockk(relaxed = true)
    private val queuedEvents = mutableListOf<Event>()

    private lateinit var serverAnalytics: ServerAnalytics

    @BeforeEach
    fun setUp() {
        every { mockAnalytics.configuration } returns ServerConfiguration(
            writeKey = "<write-key>",
            dataPlaneUrl = "https://test.dataplane.com",
            dropListener = mockDropListener,
        )
        every { mockAnalytics.enqueue(capture(queuedEvents)) } just runs

        serverAnalytics = ServerAnalytics(mockAnalytics)
    }

    @Nested
    inner class Identity {

        @Test
        fun `given each event call has its own user, when the calls are made in sequence, then each event carries only the identity of its call`() {
            serverAnalytics.identify(userId = USER_1, traits = TRAITS)
            serverAnalytics.track(name = "Order Completed", userId = USER_1)
            serverAnalytics.screen(screenName = "Home", anonymousId = ANONYMOUS_2, category = "Main")
            serverAnalytics.group(groupId = "group-1", userId = USER_2, anonymousId = ANONYMOUS_2)
            serverAnalytics.alias(userId = USER_2, previousId = USER_1)

            val expectedIdentities = listOf(
                IdentifyEvent::class to UserIdentity(anonymousId = "", userId = USER_1, traits = TRAITS),
                TrackEvent::class to UserIdentity(anonymousId = "", userId = USER_1, traits = emptyJsonObject),
                ScreenEvent::class to UserIdentity(anonymousId = ANONYMOUS_2, userId = "", traits = emptyJsonObject),
                GroupEvent::class to UserIdentity(anonymousId = ANONYMOUS_2, userId = USER_2, traits = emptyJsonObject),
                AliasEvent::class to UserIdentity(anonymousId = "", userId = USER_2, traits = emptyJsonObject),
            )
            assertEquals(expectedIdentities, queuedEvents.map { it::class to it.userIdentityState })
            assertEquals(USER_1, (queuedEvents.last() as AliasEvent).previousId)
        }

        @Test
        fun `given a call with a blank anonymousId, when the event is encoded for the server, then the JSON has the userId and no anonymousId`() {
            serverAnalytics.track(name = "Order Completed", userId = USER_1, anonymousId = BLANK)

            val event = queuedEvents.single().also { it.updateData(PlatformType.Server) }
            val json = LenientJson.parseToJsonElement(event.encodeToString(omitBlankAnonymousId = true)).jsonObject

            assertEquals(USER_1, json["userId"]?.jsonPrimitive?.content)
            assertFalse(json.containsKey("anonymousId"))
        }

        @Test
        fun `given calls with no user identity, when the calls are made, then each event is dropped and reported`() {
            serverAnalytics.track(name = "Order Completed")
            serverAnalytics.identify(userId = BLANK, anonymousId = BLANK)
            serverAnalytics.alias(userId = USER_1, previousId = BLANK)

            assertTrue(queuedEvents.isEmpty())
            verify(exactly = 3) { mockDropListener.onDrop(DropReason.MISSING_IDENTITY, 1) }
        }
    }

    @Nested
    inner class Drain {

        @Test
        fun `given the upload completes, when flushAndWait is called, then it returns the result of the upload`() = runTest {
            coEvery { mockAnalytics.drain() } returns true andThen false

            assertTrue(serverAnalytics.flushAndWait())
            assertFalse(serverAnalytics.flushAndWait())
        }

        @Test
        fun `given the upload does not complete, when flushAndWait is called, then it returns false after the timeout`() = runTest {
            coEvery { mockAnalytics.drain() } coAnswers { awaitCancellation() }

            assertFalse(serverAnalytics.flushAndWait(timeout = 1.seconds))
        }

        @Test
        fun `given shutdown is called, when later calls are made, then the SDK drains one time and rejects each later event and flush`() =
            runTest {
                coEvery { mockAnalytics.drain() } returns true

                val isDrained = serverAnalytics.shutdown()
                serverAnalytics.track(name = "Order Completed", userId = USER_1)

                assertTrue(isDrained)
                assertFalse(serverAnalytics.shutdown())
                assertFalse(serverAnalytics.flushAndWait())
                assertTrue(queuedEvents.isEmpty())
                coVerifyOrder {
                    mockAnalytics.drain()
                    mockAnalytics.shutdown()
                }
                coVerify(exactly = 1) { mockAnalytics.drain() }
                verify(exactly = 1) { mockAnalytics.shutdown() }
            }
    }
}
