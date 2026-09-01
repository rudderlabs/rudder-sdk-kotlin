package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val EVENT_NAME = "Sample Event"
private val emptyJsonObject = JsonObject(emptyMap())

class ConsentPluginTest {

    private val mockAnalytics: Analytics = mockk(relaxed = true)

    @Test
    fun `given a plugin backed by a CMP, when it is set up, then the current consent is pushed once`() {
        val provider = SpyConsentProvider()
        val plugin = ConsentPlugin(provider)

        plugin.setup(mockAnalytics)

        assertEquals(Plugin.PluginType.Utility, plugin.pluginType)
        assertNotNull(provider.onConsentChanged)
        verify(exactly = 1) {
            mockAnalytics.setConsent(
                match { it.allowedConsentIds == listOf("marketing") && it.deniedConsentIds == listOf("advertising") }
            )
        }
    }

    @Test
    fun `given a plugin already set up, when the CMP changes, then the new consent is pushed`() {
        val provider = SpyConsentProvider()
        ConsentPlugin(provider).setup(mockAnalytics)

        provider.simulateConsentChange(allowed = listOf("analytics"), denied = listOf("marketing"))

        verify(exactly = 1) {
            mockAnalytics.setConsent(
                match { it.allowedConsentIds == listOf("analytics") && it.deniedConsentIds == listOf("marketing") }
            )
        }
    }

    @Test
    fun `given a plugin already set up, when it is torn down, then later CMP changes are ignored`() {
        val provider = SpyConsentProvider()
        val plugin = ConsentPlugin(provider)
        plugin.setup(mockAnalytics)

        plugin.teardown()
        provider.simulateConsentChange(allowed = listOf("analytics"), denied = emptyList())

        assertNull(provider.onConsentChanged)
        verify(exactly = 1) { mockAnalytics.setConsent(any()) }
    }

    @Test
    fun `given an event carrying its own context, when the plugin intercepts it, then the event is unchanged`() =
        runTest {
            val plugin = ConsentPlugin(SpyConsentProvider())
            plugin.setup(mockAnalytics)
            val event = TrackEvent(EVENT_NAME, emptyJsonObject).apply {
                context = buildJsonObject { put("existing", "value") }
            }
            val originalContext = event.context.toString()

            val returned = plugin.intercept(event)

            assertEquals(event, returned)
            JSONAssert.assertEquals(originalContext, event.context.toString(), true)
        }
}

private class SpyConsentProvider : ConsentCategoryProvider {

    override var allowedConsentIds: List<String> = listOf("marketing")
        private set

    override var deniedConsentIds: List<String> = listOf("advertising")
        private set

    override var onConsentChanged: (() -> Unit)? = null

    fun simulateConsentChange(allowed: List<String>, denied: List<String>) {
        allowedConsentIds = allowed
        deniedConsentIds = denied
        onConsentChanged?.invoke()
    }
}
