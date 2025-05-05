package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Activity
import android.content.Intent
import android.net.ParseException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.storage.CheckBuildVersionUseCase
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.android.Configuration as AndroidConfiguration

internal const val REFERRING_APPLICATION_KEY = "referring_application"
internal const val URL_KEY = "url"
internal const val DEEPLINK_OPENED_KEY = "Deep Link Opened"

internal class DeeplinkPlugin : Plugin, ActivityLifecycleObserver {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility

    override lateinit var analytics: Analytics

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        (analytics.configuration as? AndroidConfiguration)?.let { config ->
            if (config.trackDeepLinks) {
                (analytics as? AndroidAnalytics)?.addLifecycleObserver(this)
            }
        }
    }

    override fun teardown() {
        (analytics as? AndroidAnalytics)?.removeLifecycleObserver(this)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        trackDeepLink(activity)
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
        return if (CheckBuildVersionUseCase.isAndroidVersionAtLeast(Build.VERSION_CODES.LOLLIPOP_MR1)) {
            this.referrer?.toString()
        } else {
            getReferrerCompatible(this)?.toString()
        }
    }

    private fun getReferrerCompatible(activity: Activity): Uri? {
        var referrerUri: Uri?
        val intent = activity.intent

        referrerUri = if (CheckBuildVersionUseCase.isAndroidVersionAtLeast(Build.VERSION_CODES.TIRAMISU)) {
            intent.getParcelableExtra(Intent.EXTRA_REFERRER, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_REFERRER)
        }

        if (referrerUri == null) {
            // Intent.EXTRA_REFERRER_NAME
            referrerUri = intent.getStringExtra("android.intent.extra.REFERRER_NAME")?.let {
                // Try parsing the referrer URL; if it's invalid, return null
                try {
                    it.toUri()
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
