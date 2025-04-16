package com.rudderstack.sampleapp.mainscreen

data class MainViewModelState(
    val log: String = "",
    val state: AnalyticsState? = null,
    val isAdvertisingIdEnabled: Boolean = false

) {
    sealed class AnalyticsState(val eventName: String? = "") {
        data object InitialState : AnalyticsState()
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
        data object NavigateToScreens : AnalyticsState("Navigate to screens")
    }
}

