package com.rudderstack.sampleapp.mainview

import java.util.Date

data class MainViewModelState(
    val logDataList: List<LogData> = emptyList(),
    val state: AnalyticsState? = null,
)

data class LogData(val time: Date, val log: String)

sealed class AnalyticsState(val messageName: String) {
    object TrackMessage : AnalyticsState("Track")
}
