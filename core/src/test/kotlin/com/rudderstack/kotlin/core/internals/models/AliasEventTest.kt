package com.rudderstack.kotlin.core.internals.models

import com.rudderstack.kotlin.core.applyMockedValues
import com.rudderstack.kotlin.core.internals.models.provider.provideSampleExternalIdsPayload
import com.rudderstack.kotlin.core.internals.models.provider.provideSampleIntegrationsPayload
import com.rudderstack.kotlin.core.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.kotlin.core.internals.models.provider.provideUserIdentityState
import com.rudderstack.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.kotlin.core.internals.platform.PlatformType
import com.rudderstack.kotlin.core.internals.utils.encodeToString
import com.rudderstack.kotlin.core.readFileTrimmed
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val aliasEventsWithOnlyUserId = "message/alias/alias_events_with_only_user_id.json"
private const val aliasEventsWithOnlyUserIdAndPreviousId = "message/alias/alias_events_with_only_user_id_and_previous_id.json"
private const val aliasEventsWithOnlyUserIdAndOptions = "message/alias/alias_events_with_only_user_id_and_options.json"
private const val aliasEventsWithOnlyUserIdAndCustomContexts = "message/alias/alias_events_with_only_user_id_and_custom_contexts.json"
private const val aliasEventsWithAllArguments = "message/alias/alias_events_with_all_arguments.json"
private const val aliasEventsWithAllArgumentsFromServer = "message/alias/alias_events_with_all_arguments_from_server.json"

private const val PREVIOUS_USER_ID = "Previous User Id 1"
private const val ALIAS_ID = "Alias Id 1"
private const val PREVIOUS_ID = "Previous Id 1"

class AliasEventTest {

    @Test
    fun `given only user id is passed, when alias event is made, then both user id and previousId is set`() {
        val expectedJsonString = readFileTrimmed(aliasEventsWithOnlyUserId)
        val userIdentityState: UserIdentity = provideUserIdentityState(userId = ALIAS_ID)

        val aliasEvent = AliasEvent(
            previousId = PREVIOUS_USER_ID,
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = aliasEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only user id and previousId are passed, when alias event is made, then both user id and previousId are set`() {
        val expectedJsonString = readFileTrimmed(aliasEventsWithOnlyUserIdAndPreviousId)
        val userIdentityState: UserIdentity = provideUserIdentityState(userId = ALIAS_ID)

        val aliasEvent = AliasEvent(
            previousId = PREVIOUS_ID,
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = aliasEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only user id and options are passed, when alias event is made, then both user id and options are set`() {
        val expectedJsonString = readFileTrimmed(aliasEventsWithOnlyUserIdAndOptions)
        val userIdentityState: UserIdentity = provideUserIdentityState(userId = ALIAS_ID)

        val aliasEvent = AliasEvent(
            previousId = PREVIOUS_USER_ID,
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
            ),
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = aliasEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given only user id and custom contexts are passed, when alias event is made, then both user id and custom contexts are set`() {
        val expectedJsonString = readFileTrimmed(aliasEventsWithOnlyUserIdAndCustomContexts)
        val userIdentityState: UserIdentity = provideUserIdentityState(userId = ALIAS_ID)

        val aliasEvent = AliasEvent(
            previousId = PREVIOUS_USER_ID,
            options = RudderOption(
                customContext = provideSampleJsonPayload(),
            ),
            userIdentityState = userIdentityState,
        ).also {
            it.applyMockedValues()
            it.updateData(PlatformType.Mobile)
        }

        val actualPayloadString = aliasEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given all arguments are passed, when alias event is made, then all arguments are set`() {
        val expectedJsonString = readFileTrimmed(aliasEventsWithAllArguments)
        val userIdentityState: UserIdentity = provideUserIdentityState(userId = ALIAS_ID)

        val aliasEvent = AliasEvent(
            previousId = PREVIOUS_ID,
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

        val actualPayloadString = aliasEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }

    @Test
    fun `given all arguments are passed from server, when alias event is made, then all arguments are set`() {
        val expectedJsonString = readFileTrimmed(aliasEventsWithAllArgumentsFromServer)
        val userIdentityState: UserIdentity = provideUserIdentityState(userId = ALIAS_ID)

        val aliasEvent = AliasEvent(
            previousId = PREVIOUS_ID,
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

        val actualPayloadString = aliasEvent.encodeToString()

        JSONAssert.assertEquals(expectedJsonString, actualPayloadString, true)
    }
}
