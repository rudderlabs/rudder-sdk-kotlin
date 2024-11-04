package com.rudderstack.android.sdk.plugins

import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.LoggerAnalytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting
import java.util.Locale

private const val LOCALE_KEY = "locale"

/**
 * Plugin to attach locale info to the message context payload
 */
internal class LocaleInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private lateinit var localeContext: JsonObject

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        localeContext = constructLocaleContext()
    }

    @VisibleForTesting
    internal fun constructLocaleContext(): JsonObject = buildJsonObject {
        put(LOCALE_KEY, Locale.getDefault().language + "-" + Locale.getDefault().country)
    }

    override fun execute(message: Message): Message = attachLocaleInfo(message)

    private fun attachLocaleInfo(message: Message): Message {
        LoggerAnalytics.debug("Attaching locale info to the message payload")

        message.context = message.context mergeWithHigherPriorityTo localeContext

        return message
    }
}
