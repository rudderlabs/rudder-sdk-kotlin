package com.rudderstack.sdk.kotlin.android.plugins

import android.util.DisplayMetrics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting

private const val SCREEN_KEY = "screen"
private const val SCREEN_DENSITY_KEY = "density"
private const val SCREEN_HEIGHT_KEY = "height"
private const val SCREEN_WIDTH_KEY = "width"

/**
 * Plugin to attach screen info to the event context payload
 */
internal class ScreenInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private lateinit var screenContext: JsonObject

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as Configuration).let {
            val displayMetrics = it.application.resources.displayMetrics
            screenContext = constructScreenContext(displayMetrics)
        }
    }

    @VisibleForTesting
    internal fun constructScreenContext(displayMetrics: DisplayMetrics): JsonObject = buildJsonObject {
        put(
            SCREEN_KEY,
            buildJsonObject {
                put(SCREEN_DENSITY_KEY, displayMetrics.densityDpi)
                put(SCREEN_HEIGHT_KEY, displayMetrics.heightPixels)
                put(SCREEN_WIDTH_KEY, displayMetrics.widthPixels)
            }
        )
    }

    override suspend fun intercept(event: Event): Event = attachScreenInfo(event)

    private fun attachScreenInfo(event: Event): Event {
        LoggerAnalytics.debug("Attaching screen info to the event payload")

        event.context = event.context mergeWithHigherPriorityTo screenContext

        return event
    }
}
