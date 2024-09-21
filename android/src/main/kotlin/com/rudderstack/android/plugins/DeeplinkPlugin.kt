package com.rudderstack.android.plugins

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Bundle
import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.properties.Delegates
import com.rudderstack.android.Configuration as AndroidConfiguration

private const val REFERRING_APPLICATION_KEY = "referring_application"
private const val URL_KEY = "url"
private const val DEEPLINK_OPENED_KEY = "Deep Link Opened"

internal class DeeplinkPlugin : Plugin, Application.ActivityLifecycleCallbacks {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual

    override lateinit var analytics: Analytics

    private lateinit var application: Application
    private var trackDeepLinks by Delegates.notNull<Boolean>()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            trackDeepLinks = config.trackDeeplinks
            if (trackDeepLinks) {
                application = config.application
                application.registerActivityLifecycleCallbacks(this)
            }
        }
    }

    override fun teardown() {
        super.teardown()
        if (trackDeepLinks) {
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        trackDeepLink(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        // NO-OP
    }

    override fun onActivityResumed(activity: Activity) {
        // NO-OP
    }

    override fun onActivityPaused(activity: Activity) {
        // NO-OP
    }

    override fun onActivityStopped(activity: Activity) {
        // NO-OP
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // NO-OP
    }

    override fun onActivityDestroyed(activity: Activity) {
        // NO-OP
    }

    private fun trackDeepLink(activity: Activity) {
        val intent = activity.intent
        if (intent == null || intent.data == null) {
            return
        }

        val properties = buildJsonObject {
            activity.getReferrerString()?.let {
                put(REFERRING_APPLICATION_KEY, it)
            }

            intent.data?.let { uri ->
                putUriParams(uri)
            }
        }
        analytics.track(DEEPLINK_OPENED_KEY, properties)
    }

    private fun Activity.getReferrerString(): String? {
        return this.referrer?.toString()
    }

    private fun JsonObjectBuilder.putUriParams(uri: Uri) {
        if (uri.isHierarchical) {
            for (parameter in uri.queryParameterNames) {
                val value = uri.getQueryParameter(parameter)
                if (value != null && value.trim().isNotEmpty()) {
                    put(parameter, value)
                }
            }
        }
        put(URL_KEY, uri.toString())
    }
}
