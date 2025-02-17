package com.rudderstack.integration.kotlin.braze

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.braze.Braze
import com.braze.configuration.BrazeConfig
import com.braze.support.BrazeLogger
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonObject

/**
 * BrazeIntegration is a plugin that sends events to the Braze SDK.
 */
class BrazeIntegration : IntegrationPlugin() {

    override val key: String
        get() = "Braze"

    private var braze: Braze? = null

    // TODO("Add the way to update this value dynamically through `update` method.")
    private lateinit var brazeConfig: RudderBrazeConfig

    public override fun create(destinationConfig: JsonObject) {
        braze ?: run {
            destinationConfig.parse<RudderBrazeConfig>().let { config ->
                this.brazeConfig = config
                initBraze(analytics.application, config, analytics.configuration.logLevel).also {
                    braze = it
                }
                LoggerAnalytics.verbose("BrazeIntegration: Adjust SDK initialized. $config")
            }
        }
    }

    override fun getDestinationInstance(): Any? {
        return braze
    }

    override fun track(payload: TrackEvent): Event {
        println("BrazeIntegration: track called with payload: $payload")

        return payload
    }
}

private fun initBraze(application: Application, config: RudderBrazeConfig, logLevel: Logger.LogLevel): Braze {
    with(config) {
        val builder: BrazeConfig.Builder =
            BrazeConfig.Builder()
                .setApiKey(apiKey)
                .setCustomEndpoint(customEndpoint)
        setLogLevel(logLevel)
        Braze.configure(application, builder.build())
        return Braze.getInstance(application).also { braze ->
            application.registerActivityLifecycleCallbacks(braze)
        }
    }
}

private fun setLogLevel(rudderLogLevel: Logger.LogLevel) {
    when (rudderLogLevel) {
        Logger.LogLevel.VERBOSE -> Log.VERBOSE
        Logger.LogLevel.DEBUG -> Log.DEBUG
        Logger.LogLevel.INFO -> Log.INFO
        Logger.LogLevel.WARN -> Log.WARN
        Logger.LogLevel.ERROR -> Log.ERROR
        Logger.LogLevel.NONE -> BrazeLogger.SUPPRESS
    }.also {
        BrazeLogger.logLevel = it
    }
}

private fun Application.registerActivityLifecycleCallbacks(braze: Braze) {
    this.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // No implementation needed
        }

        override fun onActivityStarted(activity: Activity) {
            braze.openSession(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            // No implementation needed
        }

        override fun onActivityPaused(activity: Activity) {
            // No implementation needed
        }

        override fun onActivityStopped(activity: Activity) {
            braze.closeSession(activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            // No implementation needed
        }

        override fun onActivityDestroyed(activity: Activity) {
            // No implementation needed
        }
    })
}
