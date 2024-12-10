package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.applyMockedValues
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleExternalIdsPayload
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleIntegrationsPayload
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import com.rudderstack.sdk.kotlin.core.provideOnlyAnonymousIdState
import com.rudderstack.sdk.kotlin.core.readFileTrimmed
import org.junit.Assert.assertEquals
import org.junit.Test

private const val trackWithDefaultArguments = "message/track/track_with_default_arguments.json"
private const val trackWithProperties = "message/track/track_with_properties.json"
private const val trackWithIntegrationsOption = "message/track/track_with_integrations_option.json"
private const val trackWithCustomContextsOption = "message/track/track_with_custom_contexts_option.json"
private const val trackWithAllArguments = "message/track/track_with_all_arguments.json"
private const val trackWithAllArgumentsFromServer = "message/track/track_with_all_arguments_from_server.json"

private const val EVENT_NAME = "Track event 1"

class TrackEventTest {

    @Test
    fun `given track event with default arguments, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(trackWithDefaultArguments)
        val trackEvent = TrackEvent(
            event = EVENT_NAME,
            properties = emptyJsonObject,
            options = RudderOption(),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
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
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
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
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
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
                customContext = provideSampleJsonPayload(),
            ),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
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
            ),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
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
                customContext = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
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
                customContext = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Server)
        }

        val actualPayloadString = trackEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }
}
