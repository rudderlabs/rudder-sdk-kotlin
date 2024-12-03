package com.rudderstack.sampleapp.analytics.customplugins

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.utils.Result
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.putAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
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

class AndroidAdvertisingIdPlugin @OptIn(DelicateCoroutinesApi::class) constructor(private val scope: CoroutineScope = GlobalScope) : Plugin {

    override val pluginType = Plugin.PluginType.OnProcess

    override lateinit var analytics: Analytics
    private lateinit var application: Application
    internal var advertisingId = String.empty()
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
                    advertisingId = String.empty()
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

    internal fun attachAdvertisingId(messagePayload: Message): Message {
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

    override suspend fun execute(message: Message): Message {
        return attachAdvertisingId(message)
    }
}
