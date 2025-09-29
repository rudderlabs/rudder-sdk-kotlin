package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Activity
import androidx.navigation.NavController
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.models.reset.ResetOptions
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val SESSION_ID = 1234567890L

class JavaAnalyticsTest {

    @MockK
    private lateinit var mockConfiguration: Configuration

    @MockK
    private lateinit var mockAnalytics: Analytics

    private lateinit var javaAnalytics: JavaAnalytics

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockkStatic(::provideAnalyticsInstance)
        every { provideAnalyticsInstance(any()) } returns mockAnalytics

        javaAnalytics = JavaAnalytics(mockConfiguration)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::provideAnalyticsInstance)
    }

    @Test
    fun `when session is fetched, then it should return the value`() {
        every { mockAnalytics.sessionId } returns SESSION_ID

        val actualSessionId = javaAnalytics.sessionId

        assertEquals(SESSION_ID, actualSessionId)
    }

    @Test
    fun `when session is started, then it should call the corresponding method on Analytics`() {
        javaAnalytics.startSession()
        javaAnalytics.startSession(sessionId = SESSION_ID)

        verify {
            mockAnalytics.startSession()
            mockAnalytics.startSession(sessionId = SESSION_ID)
        }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when session is ended, then it should call the corresponding method on Analytics`() {
        javaAnalytics.endSession()

        verify { mockAnalytics.endSession() }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when reset is called, then it should call the corresponding method on Analytics`() {
        javaAnalytics.reset()

        verify { mockAnalytics.reset(any()) }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when reset is called with options, then it should call the corresponding method on Analytics`() {
        val options = ResetOptions()

        javaAnalytics.reset(options)

        verify { mockAnalytics.reset(options) }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when flush is called, then it should call the corresponding method on Analytics`() {
        javaAnalytics.flush()

        verify { mockAnalytics.flush() }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when setNavigationDestinationsTracking is called, then it should call the corresponding method on Analytics`() {
        val navController = mockk<NavController>(relaxed = true)
        val activity = mockk<Activity>(relaxed = true)

        javaAnalytics.setNavigationDestinationsTracking(navController, activity)

        verify { mockAnalytics.setNavigationDestinationsTracking(navController, activity) }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when plugin is added, then it should call the corresponding method on Analytics`() {
        val plugin = mockk<Plugin>(relaxed = true)

        javaAnalytics.add(plugin)

        verify { mockAnalytics.add(plugin) }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when plugin is removed, then it should call the corresponding method on Analytics`() {
        val plugin = mockk<Plugin>(relaxed = true)

        javaAnalytics.remove(plugin)

        verify { mockAnalytics.remove(plugin) }
        confirmVerified(mockAnalytics)
    }
}
