package com.rudderstack.sdk.kotlin.android.plugins

import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting
import java.util.Locale

private const val LOCALE_KEY = "locale"

/**
 * Plugin to attach locale info to the event context payload
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

    override suspend fun intercept(event: Event): Event = attachLocaleInfo(event)

    private fun attachLocaleInfo(event: Event): Event {
        LoggerAnalytics.debug("Attaching locale info to the event payload")

        event.context = event.context mergeWithHigherPriorityTo localeContext

        return event
    }
}
