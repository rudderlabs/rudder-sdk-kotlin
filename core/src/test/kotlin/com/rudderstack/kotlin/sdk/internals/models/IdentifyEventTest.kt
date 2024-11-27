package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.applyMockedValues
import com.rudderstack.kotlin.sdk.internals.models.provider.provideSampleExternalIdsPayload
import com.rudderstack.kotlin.sdk.internals.models.provider.provideSampleIntegrationsPayload
import com.rudderstack.kotlin.sdk.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.kotlin.sdk.internals.models.provider.provideUserIdentityState
import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.utils.encodeToString
import com.rudderstack.kotlin.sdk.readFileTrimmed
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val identifyEventsWithOnlyUserId = "message/identify/identify_events_with_only_user_id.json"
private const val identifyEventsWithOnlyTraits = "message/identify/identify_events_with_only_traits.json"
private const val identifyEventsWithOnlyOptions = "message/identify/identify_events_with_only_options.json"
private const val identityEventsWithOnlyUserIdAndTraits =
    "message/identify/identify_events_with_only_user_id_and_traits.json"
private const val identifyEventsWithOnlyUserIdAndOptions =
    "message/identify/identify_events_with_only_user_id_and_options.json"
private const val identifyEventsWithOnlyTraitsAndOptions =
    "message/identify/identify_events_with_only_traits_and_options.json"
private const val identifyEventsWithAllArguments = "message/identify/identify_events_with_all_arguments.json"
private const val identifyEventsWithAllArgumentsFromServer =
    "message/identify/identify_events_with_all_arguments_from_server.json"

private const val USER_ID = "User Id 1"

class IdentifyEventTest {

    @Test
    fun `given only user id is passed, when identify event is made, then only user id is set`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithOnlyUserId)
        val userIdentityState: UserIdentity = provideUserIdentityState(userId = USER_ID)
        val identifyEvent = IdentifyEvent(
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only traits is passed, when identify event is made, then only traits is set`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithOnlyTraits)
        val userIdentityState: UserIdentity = provideUserIdentityState(traits = provideSampleJsonPayload())
        val identifyEvent = IdentifyEvent(
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only options is passed, when identify event is made, then only options is set`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithOnlyOptions)
        val userIdentityState: UserIdentity = provideUserIdentityState(externalIds = provideSampleExternalIdsPayload())
        val identifyEvent = IdentifyEvent(
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContext = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only userId and traits are passed, when identify event is made, then only userId and traits are set`() {
        val expectedJsonString = readFileTrimmed(identityEventsWithOnlyUserIdAndTraits)
        val userIdentityState: UserIdentity = provideUserIdentityState(
            userId = USER_ID,
            traits = provideSampleJsonPayload()
        )
        val identifyEvent = IdentifyEvent(
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only userId and options are passed, when identify event is made, then only userId and options are set`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithOnlyUserIdAndOptions)
        val userIdentityState: UserIdentity = provideUserIdentityState(
            userId = USER_ID,
            externalIds = provideSampleExternalIdsPayload()
        )
        val identifyEvent = IdentifyEvent(
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContext = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only traits and options are passed, when identify event is made, then only traits and options are set`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithOnlyTraitsAndOptions)
        val userIdentityState: UserIdentity = provideUserIdentityState(
            traits = provideSampleJsonPayload(),
            externalIds = provideSampleExternalIdsPayload()
        )
        val identifyEvent = IdentifyEvent(
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContext = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given all arguments are passed, when identify event is made, then all arguments are set`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithAllArguments)
        val userIdentityState: UserIdentity = provideUserIdentityState(
            userId = USER_ID,
            traits = provideSampleJsonPayload(),
            externalIds = provideSampleExternalIdsPayload()
        )
        val identifyEvent = IdentifyEvent(
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContext = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given all arguments are passed from server, when identify event is made, then all arguments are set`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithAllArgumentsFromServer)
        val userIdentityState: UserIdentity = provideUserIdentityState(
            userId = USER_ID,
            traits = provideSampleJsonPayload(),
            externalIds = provideSampleExternalIdsPayload()
        )
        val identifyEvent = IdentifyEvent(
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContext = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Server)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given some overlapped keys are passed in traits, when identify event with user id is made, then traits should contain user id values and not the overlapped values`() {
        val expectedJsonString = readFileTrimmed(identifyEventsWithOnlyUserId)
        val overlappedTraits = buildJsonObject {
            put("userId", "User Id 2") // If userId is passed then that should overwrite this value
            put("id", "User Id 2") // If userId is passed then that should overwrite this value
        }
        val userIdentityState: UserIdentity = provideUserIdentityState(
            userId = USER_ID,
            traits = overlappedTraits
        )
        val identifyEvent = IdentifyEvent(
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = identifyEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }
}

