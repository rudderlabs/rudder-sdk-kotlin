package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleExternalIdsPayload
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleIntegrationsPayload
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideSampleJsonPayload
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.policies.DEFAULT_FLUSH_INTERVAL_IN_MILLIS
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.BufferedReader

const val ANONYMOUS_ID = "<anonymous-id>"
internal const val UUID = "c323f9d5-aa04-4305-ba8d-1eff5e99f468"
internal const val MESSAGE_ID = "<message-id>"

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mock = mockk<Analytics>(relaxed = true)
    every { mock.analyticsScope } returns testScope
    every { mock.analyticsDispatcher } returns testDispatcher
    every { mock.storageDispatcher } returns testDispatcher
    every { mock.networkDispatcher } returns testDispatcher
    return mock
}

fun Event.applyMockedValues() {
    this.originalTimestamp = "<original-timestamp>"
    this.context = emptyJsonObject
    this.messageId = "<message-id>"
}

internal fun provideOnlyAnonymousIdState(): UserIdentity {
    return UserIdentity(
        anonymousId = ANONYMOUS_ID,
        userId = String.empty(),
        traits = emptyJsonObject,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
fun TestDispatcher.advanceTimeBy(timeInMillis: Long = DEFAULT_FLUSH_INTERVAL_IN_MILLIS) {
    this.scheduler.advanceTimeBy(timeInMillis)
    this.scheduler.runCurrent()
}

fun Any.readFileAsString(fileName: String): String {
    val inputStream = this::class.java.classLoader.getResourceAsStream(fileName)
    return inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
}

fun Any.readFileTrimmed(fileName: String) =
    this.readFileAsString(fileName).cleanJsonString()

private fun String.cleanJsonString(): String {
    val regex = Regex("""(?<!\\)"[^"]*?"|[\s\n\r]+""")
    return this.replace(regex) { matchResult ->
        val matchedText = matchResult.value
        if (matchedText.startsWith("\"") && matchedText.endsWith("\"")) {
            // Keep the quoted text as is
            matchedText
        } else {
            // Remove spaces and newlines
            ""
        }
    }
}

fun setupLogger(logger: Logger, level: Logger.LogLevel = Logger.LogLevel.VERBOSE) {
    LoggerAnalytics.setup(logger = logger, logLevel = level)
}

// As Mockk doesn't seems to support spying on lambda function, we need to create a class for the same.
internal class Block {

    fun execute() {
        // Do nothing
    }

    fun executeAndThrowException() {
        throw Exception("Exception occurred")
    }
}

internal fun provideSpyBlock(): Block {
    return spyk(Block())
}

internal fun provideRudderOption() = RudderOption(
    integrations = provideSampleIntegrationsPayload(),
    customContext = provideSampleJsonPayload(),
    externalIds = provideSampleExternalIdsPayload(),
)

@OptIn(ExperimentalSerializationApi::class)
internal fun provideJsonObject(): JsonObject = buildJsonObject {
    // Basic primitives
    put("stringKey", "stringValue")
    put("intKey", 42)
    put("doubleKey", 3.14)
    put("booleanKey", true)
    put("nullKey", null)

    // Empty values
    put("emptyString", "")
    put("emptyArray", buildJsonArray {})
    put("emptyObject", buildJsonObject {})

    // Existing nested structure
    put("level1", buildJsonObject {
        put("level2", buildJsonArray {
            add(buildJsonObject { put("name1", "item1") })
            add(buildJsonObject { put("name2", "item2") })
            add(buildJsonObject { put("name3", null) })
        })
    })

    // Additional complex structures
    put("arrayOfPrimitives", buildJsonArray {
        add("string")
        add(123)
        add(false)
        add(null)
    })

    // Deeply nested structure
    put("deep", buildJsonObject {
        put("level1", buildJsonObject {
            put("level2", buildJsonObject {
                put("level3", buildJsonArray {
                    add(buildJsonObject {
                        put("key", "value")
                        put("number", 999)
                    })
                })
            })
        })
    })
}

internal fun provideMap() = mapOf(
    // Basic primitives
    "stringKey" to "stringValue",
    "intKey" to 42,
    "doubleKey" to 3.14,
    "booleanKey" to true,
    "nullKey" to null,

    // Empty values
    "emptyString" to "",
    "emptyArray" to emptyList<Any>(),
    "emptyObject" to emptyMap<String, Any>(),

    // Existing nested structure
    "level1" to mapOf(
        "level2" to listOf(
            mapOf("name1" to "item1"),
            mapOf("name2" to "item2"),
            mapOf("name3" to null)
        )
    ),

    // Additional complex structures
    "arrayOfPrimitives" to listOf("string", 123, false, null),

    // Deeply nested structure
    "deep" to mapOf(
        "level1" to mapOf(
            "level2" to mapOf(
                "level3" to listOf(
                    mapOf(
                        "key" to "value",
                        "number" to 999
                    )
                )
            )
        )
    )
)
