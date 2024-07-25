package com.rudderstack.sampleapp.mainview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rudderstack.core.internals.models.Properties
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.sampleapp.analytics.RudderAnalyticsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MainViewModelState())
    val state = _state.asStateFlow()

    internal fun onEventClicked(analytics: AnalyticsState) {
        val log = when (analytics) {
            AnalyticsState.ShutDownAnalytics -> {
                //RudderAnalyticsUtils.analytics.shutdown()
                "Rudder Analytics is shutting down. Init again if needed. This might take a second"
            }

            AnalyticsState.TrackEvent -> {
                RudderAnalyticsUtils.analytics.track(
                    name = "Track at ${Date()}",
                    properties = Properties(emptyMap()),
                    options = RudderOption()
                )
                "Track message sent"
            }

            AnalyticsState.AliasEvent -> TODO()
            AnalyticsState.ClearAnalytics -> TODO()
            AnalyticsState.DisableAutoTracking -> TODO()
            AnalyticsState.EnableAutoTracking -> TODO()
            AnalyticsState.EndSession -> TODO()
            AnalyticsState.ForceFlush -> TODO()
            AnalyticsState.GroupEvent -> TODO()
            AnalyticsState.IdentifyEvent -> TODO()
            AnalyticsState.OptInAnalytics -> TODO()
            AnalyticsState.ScreenEvent -> TODO()
            AnalyticsState.SendError -> TODO()
            AnalyticsState.StartManualSession -> TODO()
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

    override fun onCleared() {
        super.onCleared()
    }
}
