package com.rudderstack.sampleapp.mainview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sampleapp.analytics.RudderAnalyticsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Date

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MainViewModelState())
    val state = _state.asStateFlow()

    internal fun onMessageClicked(analytics: AnalyticsState) {
        val log = when (analytics) {
            AnalyticsState.TrackMessage -> {
                RudderAnalyticsUtils.analytics.track(
                    name = "Track at ${Date()}",
                    properties = getJsonObject(),
                    options = RudderOption()
                )
                "Track event sent"
            }

            AnalyticsState.ScreenMessage -> {
                RudderAnalyticsUtils.analytics.screen(
                    screenName = "Screen at ${Date()}",
                    category = "Main",
                    properties = getJsonObject(),
                )
                "Screen event sent"
            }

            AnalyticsState.GroupMessage -> {
                RudderAnalyticsUtils.analytics.group(
                    groupId = "Group at ${Date()}",
                    traits = getJsonObject(),
                    options = RudderOption()
                )
                "Group event sent"
            }

            AnalyticsState.IdentifyMessage -> {
                RudderAnalyticsUtils.analytics.identify(
                    userId = "User 1",
                    traits = getJsonObject(),
                    options = getRudderOption()
                )
                "Identify event sent"
            }

            AnalyticsState.AliasMessage -> {
                RudderAnalyticsUtils.analytics.alias(
                    newId = "Alias ID 1",
                    previousId = "Explicit Previous User ID 1",
                    options = RudderOption()
                )
                "Alias event sent"
            }

            AnalyticsState.ForceFlush -> {
                RudderAnalyticsUtils.analytics.flush()
                "Flushing the event pipeline has"
            }

            AnalyticsState.Shutdown -> {
                RudderAnalyticsUtils.analytics.shutdown()
                "Shutting down the SDK"
            }

            AnalyticsState.Reset -> {
                RudderAnalyticsUtils.analytics.reset(clearAnonymousId = false)
                "Resetting the userId, traits, and externalIds"
            }

            AnalyticsState.StartSession -> {
                RudderAnalyticsUtils.analytics.startSession()
                "Manual Session started"
            }

            AnalyticsState.StartSessionWithCustomId -> {
                RudderAnalyticsUtils.analytics.startSession(1000000001)
                "Session started with custom id"
            }

            AnalyticsState.EndSession -> {
                RudderAnalyticsUtils.analytics.endSession()
                "Session ended"
            }

            AnalyticsState.Initialize -> {
                RudderAnalyticsUtils.initialize(getApplication())
                "SDK initialized"
            }

            AnalyticsState.SetAnonymousId -> {
                RudderAnalyticsUtils.analytics.anonymousId = "Custom Anonymous ID"
                "Anonymous ID is set as: ${RudderAnalyticsUtils.analytics.anonymousId}"
            }

            AnalyticsState.GetAnonymousId -> {
                val anonymousId = RudderAnalyticsUtils.analytics.anonymousId
                "Anonymous ID: $anonymousId"
            }

            AnalyticsState.GetUserId -> {
                val userId = RudderAnalyticsUtils.analytics.userId
                "User ID: $userId"
            }

            AnalyticsState.GetTraits -> {
                val traits = RudderAnalyticsUtils.analytics.traits
                "Traits: $traits"
            }
        }
        if (log.isNotEmpty()) addLogData(LogData(Date(), log))
    }

    private fun addLogData(logData: LogData) {
        _state.update { state ->
            state.copy(
                logDataList = state.logDataList + logData
            )
        }
    }

    private fun getJsonObject() = buildJsonObject {
        put("key-1", "value-1")
    }

    private fun getRudderOption() = RudderOption(
        customContext = buildJsonObject {
            put("key-1", "value-1")
        },
        integrations = buildJsonObject {
            put("Amplitude", true)
            put("INTERCOM", buildJsonObject {
                put("lookup", "phone")
            })
        },
        externalIds = listOf(
            ExternalId(type = "brazeExternalId", id = "value1234"),
        )
    )
}
