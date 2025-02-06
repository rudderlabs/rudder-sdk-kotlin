package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import kotlinx.serialization.json.JsonObject

internal const val FIREBASE_KEY = "Firebase"

class FirebaseIntegration : IntegrationPlugin() {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override val key: String
        get() = FIREBASE_KEY

    override fun create(destinationConfig: JsonObject) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(analytics.application)
        }
    }

    override fun getDestinationInstance(): Any? {
        return firebaseAnalytics
    }

    override fun identify(payload: IdentifyEvent): Event? {
        if (payload.userId.isNotEmpty()) {
            firebaseAnalytics?.setUserId(payload.userId)
        }

        val traits = analytics.traits

        traits?.keys?.forEach { key ->
            val firebaseKey = getTrimKey(key)
            traits[firebaseKey]?.toString()?.let { trait ->
                if (!IDENTIFY_RESERVED_KEYWORDS.contains(firebaseKey) && firebaseKey != "userId") {
                    firebaseAnalytics?.setUserProperty(key, trait)
                }
            }
        }

        return payload
    }

    override fun screen(payload: ScreenEvent): Event? {
        val screenName = payload.screenName

        if (screenName.isEmpty()) {
            return payload
        }
        val params = Bundle()
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        attachAllCustomProperties(params, payload.properties)

        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
        return payload
    }

    override fun reset() {
        firebaseAnalytics?.setUserId(null)
    }
}
