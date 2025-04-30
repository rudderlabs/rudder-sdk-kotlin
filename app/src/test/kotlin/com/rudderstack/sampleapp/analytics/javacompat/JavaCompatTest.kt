package com.rudderstack.sampleapp.analytics.javacompat

import android.app.Application
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.javacompat.JavaAnalytics
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val WRITE_KEY = "test_write_key"
private const val DATA_PLANE_URL = "https://test.rudderstack.com"

class JavaCompatTest {

    @MockK
    private lateinit var mockApplication: Application

    private lateinit var javaAnalytics: JavaAnalytics
    private lateinit var javaCompat: JavaCompat

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        javaAnalytics = spyk(JavaCompat.analyticsFactory(mockApplication, WRITE_KEY, DATA_PLANE_URL))
        javaCompat = JavaCompat(javaAnalytics)
    }

    @Test
    fun `when analytics is initialized, then it should be created with the correct parameters`() {
        javaCompat = JavaCompat(mockApplication, WRITE_KEY, DATA_PLANE_URL)

        assertNotNull(javaCompat)
    }

    @Test
    fun `when track event is made, then it should be tracked`() {
        javaCompat.track()

        verify(exactly = 1) {
            javaAnalytics.track(name = any<String>())
            javaAnalytics.track(name = any<String>(), properties = any<Map<String, Any>>())
            javaAnalytics.track(name = any<String>(), options = any<RudderOption>())
            javaAnalytics.track(name = any<String>(), properties = any<Map<String, Any>>(), options = any<RudderOption>())
        }
        confirmVerified(javaAnalytics)
    }

    @Test
    fun `when screen event is made, then it should be tracked`() {
        javaCompat.screen()

        verify(exactly = 1) {
            javaAnalytics.screen(screenName = any<String>())
            javaAnalytics.screen(screenName = any<String>(), category = any<String>())
            javaAnalytics.screen(screenName = any<String>(), properties = any<Map<String, Any>>())
            javaAnalytics.screen(screenName = any<String>(), options = any<RudderOption>())
            javaAnalytics.screen(
                screenName = any<String>(),
                properties = any<Map<String, Any>>(),
                options = any<RudderOption>()
            )
            javaAnalytics.screen(screenName = any<String>(), category = any<String>(), properties = any<Map<String, Any>>())
            javaAnalytics.screen(screenName = any<String>(), category = any<String>(), options = any<RudderOption>())
            javaAnalytics.screen(
                screenName = any<String>(),
                category = any<String>(),
                properties = any<Map<String, Any>>(),
                options = any<RudderOption>()
            )
        }
        confirmVerified(javaAnalytics)
    }

    @Test
    fun `when group event is made, then it should be tracked`() {
        javaCompat.group()

        verify(exactly = 1) {
            javaAnalytics.group(groupId = any<String>())
            javaAnalytics.group(groupId = any<String>(), traits = any<Map<String, Any>>())
            javaAnalytics.group(groupId = any<String>(), options = any<RudderOption>())
            javaAnalytics.group(groupId = any<String>(), traits = any<Map<String, Any>>(), options = any<RudderOption>())
        }
        confirmVerified(javaAnalytics)
    }
}
