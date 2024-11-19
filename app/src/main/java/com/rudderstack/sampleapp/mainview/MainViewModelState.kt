package com.rudderstack.sampleapp.mainview

import java.util.Date

data class MainViewModelState(
    val logDataList: List<LogData> = emptyList(),
    val state: AnalyticsState? = null,
)

data class LogData(val time: Date, val log: String)

sealed class AnalyticsState(val eventName: String) {
    object TrackMessage : AnalyticsState("Track")
    object ScreenMessage : AnalyticsState("Screen")
    object GroupMessage : AnalyticsState("Group")
    object IdentifyMessage : AnalyticsState("Identify")
    object AliasMessage : AnalyticsState("Alias")
    object ForceFlush : AnalyticsState("Flush")
    object Reset : AnalyticsState("Reset")
    object StartSession: AnalyticsState("Start Session")
    object StartSessionWithCustomId: AnalyticsState("Start Session with custom id")
    object EndSession: AnalyticsState("End Session")
}
