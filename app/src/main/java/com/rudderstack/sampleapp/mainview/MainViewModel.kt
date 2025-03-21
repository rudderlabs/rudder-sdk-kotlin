package com.rudderstack.sampleapp.mainview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.Properties
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

    internal fun onMessageClicked(analytics: MainViewModelState.AnalyticsState) {
        val log = when (analytics) {
            MainViewModelState.AnalyticsState.TrackMessage -> {
                RudderAnalyticsUtils.analytics.track(
                    name = "Track at ${Date()}",
                    properties = Properties(emptyMap()),
                    options = RudderOption()
                )
                "Track event sent"
            }

            MainViewModelState.AnalyticsState.ScreenMessage -> {
                RudderAnalyticsUtils.analytics.screen(
                    screenName = "Screen at ${Date()}",
                    category = "Main",
                    properties = buildJsonObject {
                        put("key-1", "value-1")
                    }
                )
                "Screen event sent"
            }

            MainViewModelState.AnalyticsState.GroupMessage -> {
                RudderAnalyticsUtils.analytics.group(
                    groupId = "Group at ${Date()}",
                    traits = buildJsonObject {
                        put("key-1", "value-1")
                    },
                    options = RudderOption()
                )
                "Group event sent"
            }

            MainViewModelState.AnalyticsState.IdentifyMessage -> {
                RudderAnalyticsUtils.analytics.identify(
                    userId = "User 1",
                    traits = buildJsonObject {
                        put("key-1", "value-1")
                    },
                    options = RudderOption(
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
                )
                "Identify event sent"
            }

            MainViewModelState.AnalyticsState.AliasMessage -> {
                RudderAnalyticsUtils.analytics.alias(
                    newId = "Alias ID 1",
                    previousId = "Explicit Previous User ID 1",
                    options = RudderOption()
                )
                "Alias event sent"
            }

            MainViewModelState.AnalyticsState.ForceFlush -> {
                RudderAnalyticsUtils.analytics.flush()
                "Flushing the event pipeline has"
            }

            MainViewModelState.AnalyticsState.Shutdown -> {
                RudderAnalyticsUtils.analytics.shutdown()
                "Shutting down the SDK"
            }

            MainViewModelState.AnalyticsState.Reset -> {
                RudderAnalyticsUtils.analytics.reset()
                "Resetting the userId, traits, and externalIds"
            }

            MainViewModelState.AnalyticsState.StartSession -> {
                RudderAnalyticsUtils.analytics.startSession()
                "Manual Session started"
            }

            MainViewModelState.AnalyticsState.StartSessionWithCustomId -> {
                RudderAnalyticsUtils.analytics.startSession(1000000001)
                "Session started with custom id"
            }

            MainViewModelState.AnalyticsState.EndSession -> {
                RudderAnalyticsUtils.analytics.endSession()
                "Session ended"
            }
        }
        if (log.isNotEmpty()) addLogData(MainViewModelState.LogData(Date(), log))
    }

    private fun addLogData(logData: MainViewModelState.LogData) {
        _state.update { state ->
            state.copy(
                logDataList = state.logDataList + logData
            )
        }
    }

    fun onBackClicked() {

    }
}
