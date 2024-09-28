package com.rudderstack.kotlin.internals.models

import com.rudderstack.kotlin.internals.models.provider.provideSampleExternalIdsPayload
import com.rudderstack.kotlin.internals.models.provider.provideSampleIntegrationsPayload
import com.rudderstack.kotlin.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.kotlin.internals.utils.ANONYMOUS_ID
import com.rudderstack.kotlin.internals.utils.applyMockedValues
import com.rudderstack.kotlin.readFileTrimmed
import com.rudderstack.kotlin.sdk.internals.models.Properties
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.kotlin.sdk.internals.models.ScreenEvent
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.utils.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

private const val screenWithDefaultArguments = "message/screen/screen_with_default_arguments.json"
private const val screenWithCategoryProperty = "message/screen/screen_with_category_property.json"
private const val screenWithProperties = "message/screen/screen_with_properties.json"
private const val screenWithIntegrationsOption = "message/screen/screen_with_integrations_option.json"
private const val screenWithCustomContextsOption = "message/screen/screen_with_custom_contexts_option.json"
private const val screenWithAllArguments = "message/screen/screen_with_all_arguments.json"
private const val screenWithAllArgumentsFromServer = "message/screen/screen_with_all_arguments_from_server.json"

private const val SCREEN_NAME = "Test Screen 1"
private const val NAME = "name"
private const val CATEGORY = "category"
private const val MAIN = "Main"

class ScreenEventTest {

    @Test
    fun `given screen event with default arguments, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithDefaultArguments)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideDefaultScreenProperties(),
            options = RudderOption(),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given screen event with category, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithCategoryProperty)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideScreenPropertiesWithOnlyCategory(),
            options = RudderOption(),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given screen event with properties, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithProperties)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideAllScreenProperties(),
            options = RudderOption(),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given screen event with integrations option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithIntegrationsOption)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideDefaultScreenProperties(),
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
            ),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given screen event with custom contexts option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithCustomContextsOption)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideDefaultScreenProperties(),
            options = RudderOption(
                customContexts = provideSampleJsonPayload(),
            ),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given screen event with externalIds option, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithDefaultArguments)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideDefaultScreenProperties(),
            options = RudderOption(
                externalIds = provideSampleExternalIdsPayload(),
            ),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given screen event with all arguments, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithAllArguments)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideAllScreenProperties(),
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContexts = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Mobile)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }

    @Test
    fun `given screen event with all arguments sent from server, when serialized, then it matches expected JSON`() {
        val expectedJsonString = readFileTrimmed(screenWithAllArgumentsFromServer)
        val screenEvent = ScreenEvent(
            screenName = SCREEN_NAME,
            properties = provideAllScreenProperties(),
            options = RudderOption(
                integrations = provideSampleIntegrationsPayload(),
                customContexts = provideSampleJsonPayload(),
                externalIds = provideSampleExternalIdsPayload(),
            ),
        ).also {
            it.applyMockedValues()
            it.updateData(ANONYMOUS_ID, PlatformType.Server)
        }

        val actualPayloadString = screenEvent.encodeToString()

        assertEquals(expectedJsonString, actualPayloadString)
    }
}

private fun provideDefaultScreenProperties(): Properties {
    return buildJsonObject {
        put(NAME, SCREEN_NAME)
    }
}

private fun provideScreenPropertiesWithOnlyCategory(): Properties {
    return buildJsonObject {
        put(NAME, SCREEN_NAME)
        put(CATEGORY, MAIN)
    }
}

private fun provideAllScreenProperties(): Properties {
    return buildJsonObject {
        provideSampleJsonPayload().forEach { (key, value) ->
            put(key, value)
        }
        put(NAME, SCREEN_NAME)
        put(CATEGORY, MAIN)
    }
}

