package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.WrapperLibrary
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LIBRARY_KEY = "library"
private const val LIBRARY_NAME_KEY = "name"
private const val LIBRARY_VERSION_KEY = "version"
private const val WRAPPER_LIBRARY_KEY = "wrapperLibrary"

/**
 * Plugin to attach library info to the event context payload
 */
internal class LibraryInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private lateinit var libraryContext: JsonObject

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        analytics.storage.getLibraryVersion().let {
            val name = it.getLibraryName()
            val version = it.getVersionName()

            libraryContext = buildJsonObject {
                put(
                    LIBRARY_KEY,
                    buildJsonObject {
                        put(LIBRARY_NAME_KEY, name)
                        put(LIBRARY_VERSION_KEY, version)
                    }
                )
            }
        }
    }

    override suspend fun intercept(event: Event): Event = attachLibraryInfo(event)

    private fun attachLibraryInfo(event: Event): Event {
        analytics.logger.verbose(
            "LibraryInfoPlugin: Attaching library info to the event payload (messageId=${event.messageId})"
        )

        event.context = event.context mergeWithHigherPriorityTo libraryContext
        wrapperLibraryContext()?.let { event.context = event.context mergeWithHigherPriorityTo it }

        return event
    }

    @OptIn(InternalRudderApi::class)
    private fun wrapperLibraryContext(): JsonObject? =
        WrapperLibrary.info?.let { buildJsonObject { put(WRAPPER_LIBRARY_KEY, it) } }
}
