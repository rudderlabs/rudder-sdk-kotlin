package com.rudderstack.kotlin.sdk.plugins

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.logger.TAG
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.utils.mergeWithHigherPriorityTo
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LIBRARY_KEY = "library"
private const val LIBRARY_NAME_KEY = "name"
private const val LIBRARY_VERSION_KEY = "version"

internal class LibraryInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics
    private lateinit var name: String
    private lateinit var version: String

    private val libraryContext by lazy {
        buildJsonObject {
            put(
                LIBRARY_KEY,
                buildJsonObject {
                    put(LIBRARY_NAME_KEY, name)
                    put(LIBRARY_VERSION_KEY, version)
                }
            )
        }
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        analytics.configuration.storage.getLibraryVersion().let {
            name = it.getPackageName()
            version = it.getVersionName()
        }
    }

    override fun execute(message: Message): Message = attachLibraryInfo(message)

    private fun attachLibraryInfo(message: Message): Message {
        analytics.configuration.logger.debug(TAG, "Attaching library info to the message payload")

        message.context = message.context mergeWithHigherPriorityTo libraryContext

        return message
    }
}
