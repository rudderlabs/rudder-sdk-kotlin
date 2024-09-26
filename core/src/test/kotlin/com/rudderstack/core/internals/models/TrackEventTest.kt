package com.rudderstack.core.internals.models

import com.rudderstack.core.internals.models.provider.provideSampleExternalIdsPayload
import com.rudderstack.core.internals.models.provider.provideSampleIntegrationsPayload
import com.rudderstack.core.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.core.internals.platform.PlatformType
import com.rudderstack.core.internals.utils.ANONYMOUS_ID
import com.rudderstack.core.internals.utils.applyMockedValues
import com.rudderstack.core.internals.utils.encodeToString
import com.rudderstack.core.readFileTrimmed
import org.junit.Assert.assertEquals
import org.junit.Test

private const val trackWithDefaultArguments = "message/track/track_with_default_arguments.json"
private const val trackWithProperties = "message/track/track_with_properties.json"
private const val trackWithIntegrationsOption = "message/track/track_with_integrations_option.json"
private const val trackWithCustomContextsOption =
    "message/track/track_with_custom_contexts_option.json"
private const val trackWithAllArguments = "message/track/track_with_all_arguments.json"
private const val trackWithAllArgumentsFromServer =
    "message/track/track_with_all_arguments_from_server.json"

private const val EVENT_NAME = "Track event 1"

class TrackEventTest {

    @Test
    fun `given track event with default arguments, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithDefaultArguments)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = emptyJsonObject,
            options = RudderOption(),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given track event with properties, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithProperties)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = provideSampleJsonPayload(),
            options = RudderOption(),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given track event with integrations option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithIntegrationsOption)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = emptyJsonObject,
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
            ),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given track event with custom contexts option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithCustomContextsOption)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = emptyJsonObject,
            options = RudderOption(
                customContexts = provideSampleJsonPayload(),
            )
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given track event with externalIds option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithDefaultArguments)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = emptyJsonObject,
            options = RudderOption(
                externalIds = provideSampleExternalIdsPayload(),
            )
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given track event with all arguments, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithAllArguments)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = provideSampleJsonPayload(),
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContexts = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            )
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given track event with all arguments sent from server, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithAllArgumentsFromServer)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = provideSampleJsonPayload(),
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContexts = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            )
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Server)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }
}
