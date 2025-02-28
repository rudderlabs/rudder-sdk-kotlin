package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private const val EVENT_NAME = "Sample Event"

class SetAnonymousIdPluginTest {

    @Test
    fun `given an anonymousId, when it is set using SetAnonymousIdPlugin, then it is present in payload`() = runTest {
        val anonymousId = "someAnonymousId"
        val plugin = SetAnonymousIdPlugin(anonymousId)

        val event = TrackEvent(
            event = EVENT_NAME,
            properties = emptyJsonObject,
        )
        plugin.intercept(event)

        assert(event.anonymousId == anonymousId)
    }
}
