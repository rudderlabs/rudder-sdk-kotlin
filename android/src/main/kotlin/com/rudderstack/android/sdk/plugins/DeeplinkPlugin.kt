package com.rudderstack.android.sdk.plugins

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.ParseException
import android.net.Uri
import android.os.Bundle
import com.rudderstack.android.sdk.storage.CheckBuildVersionUseCase
import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.properties.Delegates
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

internal const val REFERRING_APPLICATION_KEY = "referring_application"
internal const val URL_KEY = "url"
internal const val DEEPLINK_OPENED_KEY = "Deep Link Opened"

internal class DeeplinkPlugin(
    private val checkBuildVersionUseCase: CheckBuildVersionUseCase = CheckBuildVersionUseCase()
) : Plugin, Application.ActivityLifecycleCallbacks {

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
        return if (checkBuildVersionUseCase.isAndroidVersionLollipopAndAbove()) {
            this.referrer?.toString()
        } else {
            getReferrerCompatible(this)?.toString()
        }
    }

    private fun getReferrerCompatible(activity: Activity): Uri? {
        var referrerUri: Uri?
        val intent = activity.intent
        referrerUri = intent.getParcelableExtra(Intent.EXTRA_REFERRER)

        if (referrerUri == null) {
            // Intent.EXTRA_REFERRER_NAME
            referrerUri = intent.getStringExtra("android.intent.extra.REFERRER_NAME")?.let {
                // Try parsing the referrer URL; if it's invalid, return null
                try {
                    Uri.parse(it)
                } catch (ignored: ParseException) {
                    null
                }
            }
        }
        return referrerUri
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