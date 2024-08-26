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
            AnalyticsState.TrackEvent -> {
                RudderAnalyticsUtils.analytics.track(
                    name = "Track at ${Date()}",
                    properties = Properties(emptyMap()),
                    options = RudderOption()
                )
                "Track message sent"
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
