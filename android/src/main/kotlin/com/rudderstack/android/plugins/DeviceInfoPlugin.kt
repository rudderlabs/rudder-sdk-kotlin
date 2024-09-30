package com.rudderstack.android.plugins

import android.app.Application
import android.os.Build
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.utils.UniqueIdProvider
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.putAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.util.UUID

private const val DEVICE = "device"
private const val ID = "id"
private const val MANUFACTURER = "manufacturer"
private const val MODEL = "model"
private const val NAME = "name"
private const val TYPE = "type"
private const val ANDROID = "Android"

internal class DeviceInfoPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics
    private lateinit var application: Application
    private var collectDeviceId = false

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? Configuration)?.let { config ->
            application = config.application
            collectDeviceId = config.collectDeviceId
        }
    }

    private fun attachDeviceInfo(messagePayload: Message): Message {
        val updatedDeviceID = buildJsonObject {
            messagePayload.context[DEVICE]?.jsonObject?.let {
                putAll(it)
            }
            put(ID, retrieveDeviceId())
            put(MANUFACTURER, Build.MANUFACTURER)
            put(MODEL, Build.MODEL)
            put(NAME, Build.DEVICE)
            put(TYPE, ANDROID)
        }
        messagePayload.context = buildJsonObject {
            putAll(messagePayload.context)
            put(DEVICE, updatedDeviceID)
        }
        return messagePayload
    }

    override fun execute(message: Message): Message = attachDeviceInfo(message)

    private fun retrieveDeviceId(): String {
        return if (collectDeviceId) {
            retrieveOrGenerateStoredId(::generateId)
        } else {
            analytics.configuration.storage.readString(StorageKeys.ANONYMOUS_ID, UUID.randomUUID().toString())
        }
    }

    private fun generateId(): String {
        return UniqueIdProvider.getDeviceId(application) ?: UniqueIdProvider.getUniqueID() ?: UUID.randomUUID().toString()
    }

    private fun retrieveOrGenerateStoredId(generateId: () -> String): String {
        val storedId = analytics.configuration.storage.readString(StorageKeys.DEVICE_ID, String.empty())
        return storedId.ifBlank { generateAndStoreId(generateId()) }
    }

    private fun generateAndStoreId(newId: String): String {
        analytics.analyticsScope.launch(analytics.storageDispatcher) {
            analytics.configuration.storage.write(StorageKeys.DEVICE_ID, newId)
        }
        return newId
    }
}
