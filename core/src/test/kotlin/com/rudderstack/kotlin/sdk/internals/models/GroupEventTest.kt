package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.internals.models.provider.provideSampleExternalIdsPayload
import com.rudderstack.kotlin.sdk.internals.models.provider.provideSampleIntegrationsPayload
import com.rudderstack.kotlin.sdk.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.utils.ANONYMOUS_ID
import com.rudderstack.kotlin.sdk.internals.utils.applyMockedValues
import com.rudderstack.kotlin.sdk.internals.utils.encodeToString
import com.rudderstack.kotlin.sdk.internals.utils.provideOnlyAnonymousIdState
import com.rudderstack.kotlin.sdk.readFileTrimmed
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

private const val groupWithDefaultArguments = "message/group/group_with_default_arguments.json"
private const val groupWithTraits = "message/group/group_with_traits.json"
private const val groupWithIntegrationsOption = "message/group/group_with_integrations_option.json"
private const val groupWithCustomContextsOption = "message/group/group_with_custom_contexts_option.json"
private const val groupWithAllArguments = "message/group/group_with_all_arguments.json"
private const val groupWithAllArgumentsFromServer = "message/group/group_with_all_arguments_from_server.json"

private const val GROUP_ID = "Group Id 1"
private const val ANONYMOUS_ID_KEY = "anonymousId"

class GroupEventTest {

    @Test
    fun `given group event with default arguments, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(groupWithDefaultArguments)
        val groupEvent = GroupEvent(
            groupId = GROUP_ID,
            traits = emptyJsonObject,
            options = RudderOption(),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = groupEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given group event with traits, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(groupWithTraits)
        val groupEvent = GroupEvent(
            groupId = GROUP_ID,
            traits = provideAllTraits(),
            options = RudderOption(),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = groupEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given group event with integrations option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(groupWithIntegrationsOption)
        val groupEvent = GroupEvent(
            groupId = GROUP_ID,
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
            ),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = groupEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given group event with custom contexts option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(groupWithCustomContextsOption)
        val groupEvent = GroupEvent(
            groupId = GROUP_ID,
            options = RudderOption(
                customContext = provideSampleJsonPayload(),
            ),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = groupEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given group  event with externalIds option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(groupWithDefaultArguments)
        val groupEvent = GroupEvent(
            groupId = GROUP_ID,
            options = RudderOption(
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = provideOnlyAnonymousIdState()
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = groupEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given group event with all arguments, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(groupWithAllArguments)
        val groupEvent = GroupEvent(
            groupId = GROUP_ID,
            traits = provideAllTraits(),
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

        val actualPayloadString = groupEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given group event with all arguments sent from server, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(groupWithAllArgumentsFromServer)
        val groupEvent = GroupEvent(
            groupId = GROUP_ID,
            traits = provideAllTraits(),
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

        val actualPayloadString = groupEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }
}

private fun provideAllTraits(): RudderTraits {
    return buildJsonObject {
        provideSampleJsonPayload().forEach { (key, value) ->
            put(key, value)
        }
        put(ANONYMOUS_ID_KEY, ANONYMOUS_ID)
    }
}
