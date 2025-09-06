package com.rudderstack.sampleapp.analytics.javacompat

import android.app.Activity
import androidx.navigation.NavController
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.android.javacompat.JavaAnalytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.android.models.reset.ResetOptions
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JavaCompatTest {

    @MockK
    private lateinit var mockJavaAnalytics: JavaAnalytics

    private lateinit var javaCompat: JavaCompat

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        javaCompat = JavaCompat(mockJavaAnalytics)
    }

    // Android specific tests

    @Test
    fun `when startSession is called, then it should be tracked`() {
        javaCompat.startSession()
        javaCompat.startSession(1234567890L)

        verify(exactly = 1) {
            mockJavaAnalytics.startSession()
            mockJavaAnalytics.startSession(sessionId = any<Long>())
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when endSession is called, then it should be tracked`() {
        javaCompat.endSession()

        verify(exactly = 1) {
            mockJavaAnalytics.endSession()
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when reset is called, then it should be tracked`() {
        javaCompat.reset()

        verify(exactly = 1) {
            mockJavaAnalytics.reset()
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when resetWithOptions is called with all true, then it should call reset with correct options`() {
        val resetOptionsSlot = slot<ResetOptions>()
        
        javaCompat.resetWithOptions(true, true, true, true)

        verify(exactly = 1) {
            mockJavaAnalytics.reset(capture(resetOptionsSlot))
        }
        
        val capturedOptions = resetOptionsSlot.captured
        assertEquals(true, capturedOptions.entries.userId)
        assertEquals(true, capturedOptions.entries.anonymousId)
        assertEquals(true, capturedOptions.entries.traits)
        assertEquals(true, capturedOptions.entries.session)
        
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when resetWithOptions is called with all false, then it should call reset with correct options`() {
        val resetOptionsSlot = slot<ResetOptions>()
        
        javaCompat.resetWithOptions(false, false, false, false)

        verify(exactly = 1) {
            mockJavaAnalytics.reset(capture(resetOptionsSlot))
        }
        
        val capturedOptions = resetOptionsSlot.captured
        assertEquals(false, capturedOptions.entries.userId)
        assertEquals(false, capturedOptions.entries.anonymousId)
        assertEquals(false, capturedOptions.entries.traits)
        assertEquals(false, capturedOptions.entries.session)
        
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when resetWithOptions is called with mixed values, then it should call reset with correct options`() {
        val resetOptionsSlot = slot<ResetOptions>()
        
        javaCompat.resetWithOptions(true, false, true, false)

        verify(exactly = 1) {
            mockJavaAnalytics.reset(capture(resetOptionsSlot))
        }
        
        val capturedOptions = resetOptionsSlot.captured
        assertEquals(true, capturedOptions.entries.userId)
        assertEquals(false, capturedOptions.entries.anonymousId)
        assertEquals(true, capturedOptions.entries.traits)
        assertEquals(false, capturedOptions.entries.session)
        
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when flush is called, then it should be tracked`() {
        javaCompat.flush()

        verify(exactly = 1) {
            mockJavaAnalytics.flush()
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when plugin is added, then it should be tracked`() {
        javaCompat.add()

        verify(exactly = 1) {
            mockJavaAnalytics.add(plugin = any<Plugin>())
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when plugin is removed, then it should be tracked`() {
        javaCompat.remove()

        verify(exactly = 1) {
            mockJavaAnalytics.add(plugin = any<Plugin>())
            mockJavaAnalytics.remove(plugin = any<Plugin>())
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when setNavigationDestinationsTracking is called, then it should be tracked`() {
        val mockNavController = mockk<NavController>(relaxed = true)
        val mockActivity = mockk<Activity>(relaxed = true)
        javaCompat.setNavigationDestinationsTracking(mockNavController, mockActivity)

        verify(exactly = 1) {
            mockJavaAnalytics.setNavigationDestinationsTracking(navController = mockNavController, activity = mockActivity)
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when sessionId is fetched, then it should be returned`() {
        val sessionId = 1234567890L
        every { mockJavaAnalytics.sessionId } returns sessionId

        assertEquals(sessionId, javaCompat.sessionId)
    }

    // Core specific tests

    @Test
    fun `when track event is made, then it should be tracked`() {
        javaCompat.track()

        verify(exactly = 1) {
            mockJavaAnalytics.track(name = any<String>())
            mockJavaAnalytics.track(name = any<String>(), properties = any<Map<String, Any>>())
            mockJavaAnalytics.track(name = any<String>(), options = any<RudderOption>())
            mockJavaAnalytics.track(
                name = any<String>(),
                properties = any<Map<String, Any>>(),
                options = any<RudderOption>()
            )
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when screen event is made, then it should be tracked`() {
        javaCompat.screen()

        verify(exactly = 1) {
            mockJavaAnalytics.screen(screenName = any<String>())
            mockJavaAnalytics.screen(screenName = any<String>(), category = any<String>())
            mockJavaAnalytics.screen(screenName = any<String>(), properties = any<Map<String, Any>>())
            mockJavaAnalytics.screen(screenName = any<String>(), options = any<RudderOption>())
            mockJavaAnalytics.screen(
                screenName = any<String>(),
                properties = any<Map<String, Any>>(),
                options = any<RudderOption>()
            )
            mockJavaAnalytics.screen(
                screenName = any<String>(),
                category = any<String>(),
                properties = any<Map<String, Any>>()
            )
            mockJavaAnalytics.screen(screenName = any<String>(), category = any<String>(), options = any<RudderOption>())
            mockJavaAnalytics.screen(
                screenName = any<String>(),
                category = any<String>(),
                properties = any<Map<String, Any>>(),
                options = any<RudderOption>()
            )
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when group event is made, then it should be tracked`() {
        javaCompat.group()

        verify(exactly = 1) {
            mockJavaAnalytics.group(groupId = any<String>())
            mockJavaAnalytics.group(groupId = any<String>(), traits = any<Map<String, Any>>())
            mockJavaAnalytics.group(groupId = any<String>(), options = any<RudderOption>())
            mockJavaAnalytics.group(groupId = any<String>(), traits = any<Map<String, Any>>(), options = any<RudderOption>())
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when identify event is made, then it should be tracked`() {
        javaCompat.identify()

        verify(exactly = 1) {
            mockJavaAnalytics.identify(userId = any<String>())
            mockJavaAnalytics.identify(traits = any<Map<String, Any>>())
            mockJavaAnalytics.identify(userId = any<String>(), traits = any<Map<String, Any>>())
            mockJavaAnalytics.identify(userId = any<String>(), options = any<RudderOption>())
            mockJavaAnalytics.identify(traits = any<Map<String, Any>>(), options = any<RudderOption>())
            mockJavaAnalytics.identify(
                userId = any<String>(),
                traits = any<Map<String, Any>>(),
                options = any<RudderOption>()
            )
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when alias event is made, then it should be tracked`() {
        javaCompat.alias()

        verify(exactly = 1) {
            mockJavaAnalytics.alias(newId = any<String>())
            mockJavaAnalytics.alias(newId = any<String>(), options = any<RudderOption>())
            mockJavaAnalytics.alias(newId = any<String>(), previousId = any<String>())
            mockJavaAnalytics.alias(newId = any<String>(), previousId = any<String>(), options = any<RudderOption>())
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when shutdown is called, then it should be tracked`() {
        javaCompat.shutdown()

        verify(exactly = 1) {
            mockJavaAnalytics.shutdown()
        }
        confirmVerified(mockJavaAnalytics)
    }

    @Test
    fun `when anonymousId is fetched, then it should be returned`() {
        val anonymousId = "anonymousId"
        every { mockJavaAnalytics.anonymousId } returns anonymousId

        val actualAnonymousId = javaCompat.getAnonymousId()

        assertEquals(anonymousId, actualAnonymousId)
    }

    @Test
    fun `when userId is fetched, then it should be returned`() {
        val userId = "userId"
        every { mockJavaAnalytics.userId } returns userId

        val actualUserId = javaCompat.getUserId()

        assertEquals(userId, actualUserId)
    }

    @Test
    fun `when traits is fetched, then it should be returned`() {
        val traits: Map<String, Any> = mapOf("key-1" to "value-1")
        every { mockJavaAnalytics.traits } returns traits

        val actualTraits = javaCompat.getTraits()

        assertEquals(traits, actualTraits)
    }

    @Test
    fun `given log level is verbose, when custom logger is set, then all logs should be tracked using custom logger`() {
        LoggerAnalytics.logLevel = Logger.LogLevel.VERBOSE
        val customJavaLogger = spyk(JavaCustomLogger())
        val msg = "Test message"
        
        javaCompat.setCustomLogger(customJavaLogger)
        LoggerAnalytics.verbose(msg)
        LoggerAnalytics.debug(msg)
        LoggerAnalytics.info(msg)
        LoggerAnalytics.warn(msg)
        LoggerAnalytics.error(msg)

        verify(exactly = 1) {
            customJavaLogger.verbose(msg)
            customJavaLogger.debug(msg)
            customJavaLogger.info(msg)
            customJavaLogger.warn(msg)
            customJavaLogger.error(msg)
        }
    }
}
