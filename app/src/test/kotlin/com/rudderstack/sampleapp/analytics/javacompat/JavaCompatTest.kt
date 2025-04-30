package com.rudderstack.sampleapp.analytics.javacompat

import android.app.Application
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.javacompat.JavaAnalytics
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val WRITE_KEY = "test_write_key"
private const val DATA_PLANE_URL = "https://test.rudderstack.com"

class JavaCompatTest {

    @MockK
    private lateinit var application: Application

    private lateinit var javaAnalytics: JavaAnalytics

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        javaAnalytics = spyk(JavaCompat.initAnalytics(application, WRITE_KEY, DATA_PLANE_URL))
    }

    @Test
    fun `when track event is made, then it should be tracked`() {
        JavaCompat.track(javaAnalytics)

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
        JavaCompat.screen(javaAnalytics)

        verify(exactly = 1) {
            javaAnalytics.screen(screenName = any<String>())
            javaAnalytics.screen(screenName = any<String>(), category = any<String>())
            javaAnalytics.screen(screenName = any<String>(), properties = any<Map<String, Any>>())
            javaAnalytics.screen(screenName = any<String>(), options = any<RudderOption>())
            javaAnalytics.screen(screenName = any<String>(), properties = any<Map<String, Any>>(), options = any<RudderOption>())
            javaAnalytics.screen(screenName = any<String>(), category = any<String>(), properties = any<Map<String, Any>>())
            javaAnalytics.screen(screenName = any<String>(), category = any<String>(), options = any<RudderOption>())
            javaAnalytics.screen(screenName = any<String>(), category = any<String>(), properties = any<Map<String, Any>>(), options = any<RudderOption>())
        }
        confirmVerified(javaAnalytics)
    }
}
