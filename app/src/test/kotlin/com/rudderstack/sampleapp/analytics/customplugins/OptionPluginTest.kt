package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val KEY_1 = "key1"
private const val KEY_2 = "key2"
private const val VALUE_1 = "value1"
private const val VALUE_2 = "value2"
private const val EVENT_NAME = "Sample Event"
private val DEFAULT_INTEGRATION_ENABLED = buildJsonObject { put("All", true) }
private val AMPLITUDE_INTEGRATION_ENABLED = buildJsonObject { put("Amplitude", true) }
private val EXTERNAL_ID = listOf(ExternalId(type = "globalExternalId", id = "someValue"))
private val emptyJsonObject = JsonObject(emptyMap())

class OptionPluginTest {

    @Test
    fun `given an empty option object is passed, when the option plugin is intercepted, then event contains empty context and default integrations`() =
        runTest {
            val optionPlugin = OptionPlugin()
            val event = provideDefaultEvent().apply {
                configureDefaultIntegration(this)
            }

            optionPlugin.intercept(event)

            verifyResult(
                expected = emptyJsonObject.toString(),
                actual = event.context.toString()
            )
            verifyResult(
                expected = DEFAULT_INTEGRATION_ENABLED.toString(),
                actual = event.integrations.toString()
            )
            verifyResult(
                expected = emptyJsonObject.toString(),
                actual = (event.context["external_ids"] as? JsonObject ?: emptyJsonObject).toString()
            )
        }

    @Test
    fun `given an option with distinct key is passed, when the option plugin is intercepted, then event contains both key-value pair`() =
        runTest {
            val optionPlugin = OptionPlugin(
                option = RudderOption(
                    customContext = buildJsonObject {
                        put(KEY_1, VALUE_1)
                    },
                    integrations = AMPLITUDE_INTEGRATION_ENABLED,
                    externalIds = EXTERNAL_ID,
                )
            )
            val event = provideDefaultEvent().apply {
                configureDefaultIntegration(this)
                context = buildJsonObject {
                    put(KEY_2, VALUE_2)
                    put("externalId", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "brazeExternalId")
                            put("id", "value1234")
                        })
                    })
                }
            }

            optionPlugin.intercept(event)

            verifyResult(
                expected = buildJsonObject {
                    put(KEY_1, VALUE_1)
                    put(KEY_2, VALUE_2)
                    // Verify both externalIds are added:
                    put("externalId", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "brazeExternalId")
                            put("id", "value1234")
                        })
                        add(buildJsonObject {
                            put("type", "globalExternalId")
                            put("id", "someValue")
                        })
                    })
                }.toString(),
                actual = event.context.toString()
            )
            verifyResult(
                expected = buildJsonObject {
                    put("All", true)
                    put("Amplitude", true)
                }.toString(),
                actual = event.integrations.toString()
            )
        }

    @Test
    fun `given an option with same key but different value is passed, when the option plugin is intercepted, then event contains the updated key-value pair`() =
        runTest {
            val higherPreferenceOption = RudderOption(
                customContext = buildJsonObject {
                    put(KEY_1, VALUE_1)
                },
                integrations = buildJsonObject {
                    put("All", false)
                    put("Amplitude", true)
                },
                externalIds = listOf(
                    ExternalId(type = "globalExternalId", id = "newValue"),
                )
            )
            val optionPlugin = OptionPlugin(option = higherPreferenceOption)
            val event = provideDefaultEvent().apply {
                configureDefaultIntegration(this)
                context = buildJsonObject {
                    put(KEY_1, VALUE_2)
                    put("externalId", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "globalExternalId")
                            put("id", "oldValue")
                        })
                    })
                }
            }

            optionPlugin.intercept(event)

            verifyResult(
                expected = buildJsonObject {
                    put(KEY_1, VALUE_1)
                    // Verify externalId is updated:
                    put("externalId", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "globalExternalId")
                            put("id", "newValue")
                        })
                    })
                }.toString(),
                actual = event.context.toString()
            )
            verifyResult(
                expected = higherPreferenceOption.integrations.toString(),
                actual = event.integrations.toString()
            )
        }

    private fun verifyResult(expected: String, actual: String) {
        JSONAssert.assertEquals(expected, actual, true)
    }
}

private fun provideDefaultEvent(): Event = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)

private fun configureDefaultIntegration(event: Event) {
    event.integrations = DEFAULT_INTEGRATION_ENABLED
}
