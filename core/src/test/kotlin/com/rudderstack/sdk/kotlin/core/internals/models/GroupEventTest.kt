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

private const val groupWithDefaultArguments = "message/group/group_with_default_arguments.json"
private const val groupWithTraits = "message/group/group_with_traits.json"
private const val groupWithIntegrationsOption = "message/group/group_with_integrations_option.json"
private const val groupWithCustomContextsOption = "message/group/group_with_custom_contexts_option.json"
private const val groupWithAllArguments = "message/group/group_with_all_arguments.json"
private const val groupWithAllArgumentsFromServer = "message/group/group_with_all_arguments_from_server.json"

private const val GROUP_ID = "Group Id 1"

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
            traits = provideSampleJsonPayload(),
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
            traits = provideSampleJsonPayload(),
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
            traits = provideSampleJsonPayload(),
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
