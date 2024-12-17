package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Application
import android.os.Build
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import androidx.annotation.VisibleForTesting
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.UniqueIdProvider
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.android.utils.putIfNotNull
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
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
    private lateinit var deviceContext: JsonObject
    private var collectDeviceId = false

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? Configuration)?.let { config ->
            application = config.application
            collectDeviceId = config.collectDeviceId
            deviceContext = getDeviceInfo()
        }
    }

    override suspend fun execute(message: Event): Event = attachDeviceInfo(message)

    @VisibleForTesting
    internal fun attachDeviceInfo(message: Event): Event {
        LoggerAnalytics.debug("Attaching device info to the message payload")
        message.context = message.context mergeWithHigherPriorityTo deviceContext
        return message
    }

    @VisibleForTesting
    internal fun getDeviceInfo(): JsonObject = buildJsonObject {
        put(
            DEVICE,
            buildJsonObject {
                putIfNotNull(ID, retrieveDeviceId())
                put(MANUFACTURER, BuildInfo.getManufacturer())
                put(MODEL, BuildInfo.getModel())
                put(NAME, BuildInfo.getDevice())
                put(TYPE, ANDROID)
            }
        )
    }

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
