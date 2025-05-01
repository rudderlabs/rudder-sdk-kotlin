package com.rudderstack.sampleapp.analytics.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.android.javacompat.JavaAnalytics
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.verify
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

    @Test
    fun `when track event is made, then it should be tracked`() {
        javaCompat.track()

        verify(exactly = 1) {
            mockJavaAnalytics.track(name = any<String>())
            mockJavaAnalytics.track(name = any<String>(), properties = any<Map<String, Any>>())
            mockJavaAnalytics.track(name = any<String>(), options = any<RudderOption>())
            mockJavaAnalytics.track(name = any<String>(), properties = any<Map<String, Any>>(), options = any<RudderOption>())
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
            mockJavaAnalytics.screen(screenName = any<String>(), category = any<String>(), properties = any<Map<String, Any>>())
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
            mockJavaAnalytics.identify(userId = any<String>(), traits = any<Map<String, Any>>(), options = any<RudderOption>())
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
}
