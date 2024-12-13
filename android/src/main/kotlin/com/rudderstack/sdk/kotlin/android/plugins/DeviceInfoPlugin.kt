package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Application
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.UniqueIdProvider
import com.rudderstack.sdk.kotlin.android.utils.putIfNotNull
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Message
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.putAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

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

    @VisibleForTesting
    internal fun attachDeviceInfo(messagePayload: Message): Message {
        val updatedDeviceID = buildJsonObject {
            messagePayload.context[DEVICE]?.jsonObject?.let {
                putAll(it)
            }
            putIfNotNull(ID, retrieveDeviceId())
            put(MANUFACTURER, BuildInfo.getManufacturer())
            put(MODEL, BuildInfo.getModel())
            put(NAME, BuildInfo.getDevice())
            put(TYPE, ANDROID)
        }
        messagePayload.context = buildJsonObject {
            putAll(messagePayload.context)
            put(DEVICE, updatedDeviceID)
        }
        return messagePayload
    }

    override suspend fun execute(message: Message): Message = attachDeviceInfo(message)

    @VisibleForTesting
    internal fun retrieveDeviceId(): String? {
        return if (collectDeviceId) {
            UniqueIdProvider.getDeviceId(application)
        } else {
            null
        }
    }
}

internal object BuildInfo {

    internal fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    internal fun getModel(): String {
        return Build.MODEL
    }

    internal fun getDevice(): String {
        return Build.DEVICE
    }
}
