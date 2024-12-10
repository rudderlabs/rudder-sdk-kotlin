package com.rudderstack.sampleapp.analytics.customplugins

import com.rudderstack.kotlin.core.internals.models.Message
import com.rudderstack.kotlin.core.internals.models.RudderOption
import com.rudderstack.kotlin.core.internals.models.TrackEvent
import com.rudderstack.kotlin.core.internals.models.emptyJsonObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val KEY_1 = "key1"
private const val KEY_2 = "key2"
private const val VALUE_1 = "value1"
private const val VALUE_2 = "value2"
private const val EVENT_NAME = "Sample Event"
private val DEFAULT_INTEGRATION_ENABLED = buildJsonObject { put("All", true) }
private val AMPLITUDE_INTEGRATION_ENABLED = buildJsonObject { put("Amplitude", true) }

class OptionPluginTest {

    @Test
    fun `given an empty option object is passed, when the option plugin is executed, then message contains empty context and default integrations`() =
        runTest {
            val optionPlugin = OptionPlugin()
            val message = provideDefaultEvent().apply {
                configureDefaultIntegration(this)
            }

            optionPlugin.execute(message)

            verifyResult(
                expected = emptyJsonObject.toString(),
                actual = message.context.toString()
            )
            verifyResult(
                expected = DEFAULT_INTEGRATION_ENABLED.toString(),
                actual = message.integrations.toString()
            )
        }

    @Test
    fun `given an option with distinct key is passed, when the option plugin is executed, then message contains both key-value pair`() =
        runTest {
            val optionPlugin = OptionPlugin(
                option = RudderOption(
                    customContext = buildJsonObject {
                        put(KEY_1, VALUE_1)
                    },
                    integrations = AMPLITUDE_INTEGRATION_ENABLED
                )
            )
            val message = provideDefaultEvent().apply {
                configureDefaultIntegration(this)
                context = buildJsonObject { put(KEY_2, VALUE_2) }
            }

            optionPlugin.execute(message)

            verifyResult(
                expected = buildJsonObject {
                    put(KEY_1, VALUE_1)
                    put(KEY_2, VALUE_2)
                }.toString(),
                actual = message.context.toString()
            )
            verifyResult(
                expected = buildJsonObject {
                    put("All", true)
                    put("Amplitude", true)
                }.toString(),
                actual = message.integrations.toString()
            )
        }

    @Test
    fun `given an option with same key but different value is passed, when the option plugin is executed, then message contains the updated key-value pair`() =
        runTest {
            val higherPreferenceOption = RudderOption(
                customContext = buildJsonObject {
                    put(KEY_1, VALUE_1)
                },
                integrations = buildJsonObject {
                    put("All", false)
                    put("Amplitude", true)
                }
            )
            val optionPlugin = OptionPlugin(option = higherPreferenceOption)
            val message = provideDefaultEvent().apply {
                configureDefaultIntegration(this)
                context = buildJsonObject { put(KEY_1, VALUE_2) }
            }

            optionPlugin.execute(message)

            verifyResult(
                expected = higherPreferenceOption.customContext.toString(),
                actual = message.context.toString()
            )
            verifyResult(
                expected = higherPreferenceOption.integrations.toString(),
                actual = message.integrations.toString()
            )
        }

    private fun verifyResult(expected: String, actual: String) {
        JSONAssert.assertEquals(expected, actual, true)
    }
}

private fun provideDefaultEvent(): Message = TrackEvent(
    event = EVENT_NAME,
    properties = emptyJsonObject,
)

private fun configureDefaultIntegration(message: Message) {
    message.integrations = DEFAULT_INTEGRATION_ENABLED
}
