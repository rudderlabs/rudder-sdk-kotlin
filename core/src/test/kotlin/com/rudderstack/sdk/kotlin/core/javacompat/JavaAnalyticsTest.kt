package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.assertMapContents
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.provideJsonObject
import com.rudderstack.sdk.kotlin.core.provideMap
import com.rudderstack.sdk.kotlin.core.provideRudderOption
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val SOME_STRING = "some-string"

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

    @Test
    fun `when anonymousId is fetched, then it should return the value`() {
        val anonymousId = "anonymous-id"
        every { mockAnalytics.anonymousId } returns anonymousId

        val actualAnonymousId: String? = javaAnalytics.anonymousId

        assertEquals(anonymousId, actualAnonymousId)
    }

    @Test
    fun `when userId is fetched, then it should return the value`() {
        val userId = "user-id"
        every { mockAnalytics.userId } returns userId

        val actualUserId: String? = javaAnalytics.userId

        assertEquals(userId, actualUserId)
    }

    @Test
    fun `when traits is fetched, then it should return the traits of type map`() {
        val traits: JsonObject = provideJsonObject()
        every { mockAnalytics.traits } returns traits

        val actualTraits: Map<String, Any?>? = javaAnalytics.traits

        val expectedTraits = provideMap()
        assertMapContents(expectedTraits, actualTraits!!)
    }

    @Test
    fun `when track call is made, then it should call the track method on Analytics`() {
        val name = SOME_STRING
        val properties: Map<String, Any?> = provideMap()
        val options: RudderOption = provideRudderOption()

        javaAnalytics.apply {
            track(name = name)
            track(name = name, properties = properties)
            track(name = name, options = options)
            track(name = name, properties = properties, options = options)
        }

        mockAnalytics.apply {
            verify(exactly = 1) {
                track(name = SOME_STRING)
                track(name = SOME_STRING, properties = provideJsonObject())
                track(name = SOME_STRING, options = provideRudderOption())
                track(name = SOME_STRING, properties = provideJsonObject(), options = provideRudderOption())
            }
        }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when screen call is made, then it should call the screen method on Analytics`() {
        val name = SOME_STRING
        val category = SOME_STRING
        val properties: Map<String, Any?> = provideMap()
        val options: RudderOption = provideRudderOption()

        javaAnalytics.apply {
            screen(screenName = name)
            screen(screenName = name, category = category)
            screen(screenName = name, properties = properties)
            screen(screenName = name, options = options)
            screen(screenName = name, category = category, properties = properties)
            screen(screenName = name, category = category, options = options)
            screen(screenName = name, properties = properties, options = options)
            screen(screenName = name, category = category, properties = properties, options = options)
        }

        mockAnalytics.apply {
            verify(exactly = 1) {
                screen(screenName = SOME_STRING)
                screen(screenName = SOME_STRING, category = SOME_STRING)
                screen(screenName = SOME_STRING, properties = provideJsonObject())
                screen(screenName = SOME_STRING, options = provideRudderOption())
                screen(screenName = SOME_STRING, category = SOME_STRING, properties = provideJsonObject())
                screen(screenName = SOME_STRING, category = SOME_STRING, options = provideRudderOption())
                screen(screenName = SOME_STRING, properties = provideJsonObject(), options = provideRudderOption())
                screen(
                    screenName = SOME_STRING,
                    category = SOME_STRING,
                    properties = provideJsonObject(),
                    options = provideRudderOption()
                )
            }
        }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when group call is made, then it should call the method on Analytics`() {
        val groupId = SOME_STRING
        val traits: Map<String, Any?> = provideMap()
        val options: RudderOption = provideRudderOption()

        javaAnalytics.apply {
            group(groupId = groupId)
            group(groupId = groupId, traits = traits)
            group(groupId = groupId, options = options)
            group(groupId = groupId, traits = traits, options = options)
        }

        mockAnalytics.apply {
            verify(exactly = 1) {
                group(groupId = SOME_STRING)
                group(groupId = SOME_STRING, traits = provideJsonObject())
                group(groupId = SOME_STRING, options = provideRudderOption())
                group(groupId = SOME_STRING, traits = provideJsonObject(), options = provideRudderOption())
            }
        }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when identify call is made, then it should call the method on Analytics`() {
        val userId = SOME_STRING
        val traits: Map<String, Any?> = provideMap()
        val options: RudderOption = provideRudderOption()

        javaAnalytics.apply {
            identify(userId = userId)
            identify(traits = traits)
            identify(userId = userId, traits = traits)
            identify(userId = userId, options = options)
            identify(traits = traits, options = options)
            identify(userId = userId, traits = traits, options = options)
        }

        mockAnalytics.apply {
            verify(exactly = 1) {
                identify(userId = SOME_STRING)
                identify(traits = provideJsonObject())
                identify(userId = SOME_STRING, traits = provideJsonObject())
                identify(userId = SOME_STRING, options = provideRudderOption())
                identify(traits = provideJsonObject(), options = provideRudderOption())
                identify(userId = SOME_STRING, traits = provideJsonObject(), options = provideRudderOption())
            }
        }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when alias call is made, then it should call the method on Analytics`() {
        val newId = SOME_STRING
        val previousId = SOME_STRING
        val options: RudderOption = provideRudderOption()

        javaAnalytics.apply {
            alias(newId = newId)
            alias(newId = newId, options = options)
            alias(newId = newId, previousId = previousId)
            alias(newId = newId, previousId = previousId, options = options)
        }

        mockAnalytics.apply {
            verify(exactly = 1) {
                alias(newId = SOME_STRING)
                alias(newId = SOME_STRING, options = provideRudderOption())
                alias(newId = SOME_STRING, previousId = SOME_STRING)
                alias(newId = SOME_STRING, previousId = SOME_STRING, options = provideRudderOption())
            }
        }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when flush is called, then it should call the flush method on Analytics`() {
        javaAnalytics.flush()

        verify(exactly = 1) { mockAnalytics.flush() }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when shutdown is called, then it should call the shutdown method on Analytics`() {
        javaAnalytics.shutdown()

        verify(exactly = 1) { mockAnalytics.shutdown() }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when plugin is added, then it should call the add method on Analytics`() {
        val plugin = mockk<Plugin>(relaxed = true)

        javaAnalytics.add(plugin)

        verify(exactly = 1) { mockAnalytics.add(plugin) }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when plugin is removed, then it should call the remove method on Analytics`() {
        val plugin = mockk<Plugin>(relaxed = true)

        javaAnalytics.remove(plugin)

        verify(exactly = 1) { mockAnalytics.remove(plugin) }
        confirmVerified(mockAnalytics)
    }

    @Test
    fun `when reset is called, then it should call the reset method on Analytics`() {
        javaAnalytics.reset()

        verify(exactly = 1) { mockAnalytics.reset() }
        confirmVerified(mockAnalytics)
    }
}
