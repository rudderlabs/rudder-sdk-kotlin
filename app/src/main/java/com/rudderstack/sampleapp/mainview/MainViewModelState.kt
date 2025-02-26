package com.rudderstack.sampleapp.mainview

import java.util.Date

data class MainViewModelState(
    val logDataList: List<LogData> = emptyList(),
    val state: AnalyticsState? = null,
) {
    data class LogData(val time: Date, val log: String)

    sealed class AnalyticsState(val eventName: String) {
        data object TrackMessage : AnalyticsState("Track")
        data object ScreenMessage : AnalyticsState("Screen")
        data object GroupMessage : AnalyticsState("Group")
        data object IdentifyMessage : AnalyticsState("Identify")
        data object AliasMessage : AnalyticsState("Alias")
        data object ForceFlush : AnalyticsState("Flush")
        data object Shutdown : AnalyticsState("Shutdown")
        data object Reset : AnalyticsState("Reset")
        data object StartSession : AnalyticsState("Start Session")
        data object StartSessionWithCustomId : AnalyticsState("Start Session with custom id")
        data object EndSession : AnalyticsState("End Session")
    }

}

