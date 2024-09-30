package com.rudderstack.sampleapp.analytics.customPlugins

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.rudderstack.android.Configuration
import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.logger.TAG
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.utils.empty
import com.rudderstack.core.internals.utils.putAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

private const val DEVICE = "device"
private const val DEVICE_ADVERTISING_ID_KEY = "advertisingId"
private const val DEVICE_AD_TRACKING_ENABLED_KEY = "adTrackingEnabled"
private const val CLASS_FOR_NAME = "com.google.android.gms.ads.identifier.AdvertisingIdClient"
private const val FIRE_LIMIT_AD_TRACKING = "limit_ad_tracking"
private const val FIRE_ADVERTISING_ID = "advertising_id"

class AndroidAdvertisingIdPlugin : Plugin {

    override val pluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics
    private lateinit var application: Application
    private var advertisingId = String.empty()
    private var adTrackingEnabled = true

    companion object {
        fun isAdvertisingLibraryAvailable(): Boolean {
            return try {
                Class.forName(CLASS_FOR_NAME)
                true
            } catch (ignored: ClassNotFoundException) {
                false
            }
        }
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? Configuration)?.let { config ->
            application = config.application
        }
        CoroutineScope(Dispatchers.IO).launch {
            updateAdvertisingId()
        }
    }

    private fun updateAdvertisingId() {
        val context = application.applicationContext

        when (val result = getAdvertisingId(context)) {
            is Result.Success -> {
                adTrackingEnabled = true
                advertisingId = result.value
                analytics.configuration.logger.debug(log = "Collected advertising ID: $advertisingId")
            }

            is Result.Error -> {
                adTrackingEnabled = false
                advertisingId = String.empty()
                analytics.configuration.logger.error(log = "Failed to collect advertising ID: ${result.error.message}")
            }
        }
    }

    private fun getAdvertisingId(context: Context): Result<String, Exception> {
        return getGooglePlayServicesAdvertisingID(context).orElse { getAmazonFireAdvertisingID(context) } as Result<String, Exception>
    }

    private fun getGooglePlayServicesAdvertisingID(context: Context): Result<String, Exception> {
        return try {
            val advertisingInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (advertisingInfo.isLimitAdTrackingEnabled) {
                analytics.configuration.logger.warn(
                    tag = TAG,
                    log = "Error collecting play services ad id."
                )
                Result.Error(Exception("Error collecting play services ad id."))
            } else {
                Result.Success(advertisingInfo.id!!)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun getAmazonFireAdvertisingID(context: Context): Result<String, Exception> {
        return try {
            val contentResolver = context.contentResolver
            if (android.provider.Settings.Secure.getInt(contentResolver, FIRE_LIMIT_AD_TRACKING) != 0) {
                analytics.configuration.logger.warn(
                    tag = TAG,
                    log = "Not collecting advertising ID because limit_ad_tracking (Amazon Fire OS) is true."
                )
                Result.Error(Exception("Not collecting advertising ID because limit_ad_tracking (Amazon Fire OS) is true."))
            } else {
                val advertisingId = android.provider.Settings.Secure.getString(contentResolver, FIRE_ADVERTISING_ID)
                Result.Success(advertisingId)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun attachAdvertisingId(messagePayload: Message): Message {
        val updatedDevice = buildJsonObject {
            messagePayload.context[DEVICE]?.jsonObject?.let {
                putAll(it)
            }
            if (adTrackingEnabled && advertisingId.isNotBlank()) {
                put(DEVICE_ADVERTISING_ID_KEY, advertisingId)
            }
            put(DEVICE_AD_TRACKING_ENABLED_KEY, adTrackingEnabled)
        }
        messagePayload.context = buildJsonObject {
            putAll(messagePayload.context)
            put(DEVICE, updatedDevice)
        }
        return messagePayload
    }

    override fun execute(message: Message): Message {
        return attachAdvertisingId(message)
    }

    private sealed class Result<out T, out E> {
        class Success<out T>(val value: T) : Result<T, Nothing>()
        class Error<out E>(val error: E) : Result<Nothing, E>()

        fun <R> orElse(fallback: () -> Result<R, @UnsafeVariance E>): Result<Any?, E> {
            return when (this) {
                is Success -> this
                is Error -> fallback()
            }
        }
    }
}
