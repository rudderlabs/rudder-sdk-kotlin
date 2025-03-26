package com.rudderstack.sampleapp.analytics.customplugins

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

private const val DEVICE = "device"
private const val DEVICE_ADVERTISING_ID_KEY = "advertisingId"
private const val DEVICE_AD_TRACKING_ENABLED_KEY = "adTrackingEnabled"
private const val CLASS_FOR_NAME = "com.google.android.gms.ads.identifier.AdvertisingIdClient"
private const val FIRE_LIMIT_AD_TRACKING = "limit_ad_tracking"
private const val FIRE_ADVERTISING_ID = "advertising_id"

/**
 * A plugin that collects the advertising ID and ad tracking status.
 *
 * Add this plugin just after the SDK initialization to collect the advertising ID and ad tracking status.
 *
 * Add the plugin like this:
 * ```
 * analytics.add(AndroidAdvertisingIdPlugin())
 * ```
 *
 * This will collect the advertising ID and ad tracking status and add it to the `event.context.device` payload of each events.
 *
 * @param scope The coroutine scope to run the async task to collect the advertising ID.
 */
class AndroidAdvertisingIdPlugin @OptIn(DelicateCoroutinesApi::class) constructor(private val scope: CoroutineScope = GlobalScope) : Plugin {

    override val pluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics
    private lateinit var application: Application
    internal var advertisingId = ""
    internal var adTrackingEnabled = true

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
            updateAdvertisingId()
        }
    }

    @VisibleForTesting
    internal fun updateAdvertisingId() {
        scope.launch {
            val context = application.applicationContext

            when (val result = getAdvertisingId(context)) {
                is Result.Success -> {
                    adTrackingEnabled = true
                    advertisingId = result.response
                    LoggerAnalytics.debug(log = "Collected advertising ID: $advertisingId")
                }

                is Result.Failure -> {
                    adTrackingEnabled = false
                    advertisingId = ""
                    LoggerAnalytics.error(log = "Failed to collect advertising ID: ${result.error.message}")
                }
            }
        }
    }

    @VisibleForTesting
    internal fun getAdvertisingId(context: Context): Result<String, Exception> {
        return getGooglePlayServicesAdvertisingID(context).orElse { getAmazonFireAdvertisingID(context) } as Result<String, Exception>
    }

    @VisibleForTesting
    internal fun getGooglePlayServicesAdvertisingID(context: Context): Result<String, Exception> {
        return try {
            val advertisingInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (advertisingInfo.isLimitAdTrackingEnabled) {
                LoggerAnalytics.warn(log = "Error collecting play services ad id.")
                Result.Failure(error = Exception("Error collecting play services ad id."))
            } else {
                Result.Success(advertisingInfo.id!!)
            }
        } catch (e: Exception) {
            Result.Failure(error = e)
        }
    }

    @VisibleForTesting
    internal fun getAmazonFireAdvertisingID(context: Context): Result<String, Exception> {
        return try {
            val contentResolver = context.contentResolver
            if (android.provider.Settings.Secure.getInt(contentResolver, FIRE_LIMIT_AD_TRACKING) != 0) {
                LoggerAnalytics.warn(log = "Not collecting advertising ID because limit_ad_tracking (Amazon Fire OS) is true.")
                Result.Failure(error = Exception("Not collecting advertising ID because limit_ad_tracking (Amazon Fire OS) is true."))
            } else {
                val advertisingId = android.provider.Settings.Secure.getString(contentResolver, FIRE_ADVERTISING_ID)
                Result.Success(advertisingId)
            }
        } catch (e: Exception) {
            Result.Failure(error = e)
        }
    }

    internal fun attachAdvertisingId(eventPayload: Event): Event {
        val updatedDevice = buildJsonObject {
            eventPayload.context[DEVICE]?.jsonObject?.let {
                putAll(it)
            }
            if (adTrackingEnabled && advertisingId.isNotBlank()) {
                put(DEVICE_ADVERTISING_ID_KEY, advertisingId)
            }
            put(DEVICE_AD_TRACKING_ENABLED_KEY, adTrackingEnabled)
        }
        eventPayload.context = buildJsonObject {
            putAll(eventPayload.context)
            put(DEVICE, updatedDevice)
        }
        return eventPayload
    }

    override suspend fun intercept(event: Event): Event {
        return attachAdvertisingId(event)
    }
}

private fun JsonObjectBuilder.putAll(jsonObject: JsonObject) {
    jsonObject.forEach { (key, value) ->
        put(key, value)
    }
}
