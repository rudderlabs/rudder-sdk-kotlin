package com.rudderstack.sampleapp.mainview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rudderstack.kotlin.sdk.internals.models.ExternalId
import com.rudderstack.kotlin.sdk.internals.models.Properties
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
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
                    properties = Properties(emptyMap()),
                    options = RudderOption()
                )
                "Track message sent"
            }

            AnalyticsState.ScreenMessage -> {
                RudderAnalyticsUtils.analytics.screen(
                    screenName = "Screen at ${Date()}",
                    category = "Main",
                    properties = buildJsonObject {
                        put("key-1", "value-1")
                    }
                )
                "Screen message sent"
            }

            AnalyticsState.GroupMessage -> {
                RudderAnalyticsUtils.analytics.group(
                    groupId = "Group at ${Date()}",
                    traits = buildJsonObject {
                        put("key-1", "value-1")
                    },
                    options = RudderOption()
                )
                "Group message sent"
            }

            AnalyticsState.IdentifyMessage -> {
                RudderAnalyticsUtils.analytics.identify(
                    userId = "User 2",
                    traits = buildJsonObject {
                        put("key-1", "value-1")
                    },
                    options = RudderOption(
                        customContexts = buildJsonObject {
                            put("key-1", "value-1")
                        },
                        integrations = mapOf(
                            "Amplitude" to true
                        ),
                        externalIds = listOf(
                            ExternalId("brazeExternalId", "value1234"),
                        )
                    )
                )
                "Identify message sent"
            }

            AnalyticsState.ForceFlush -> {
                RudderAnalyticsUtils.analytics.flush()
                "Flushing the message pipeline has"
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
}
