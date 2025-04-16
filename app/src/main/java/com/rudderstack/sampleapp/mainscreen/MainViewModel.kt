package com.rudderstack.sampleapp.mainscreen

import android.annotation.SuppressLint
import android.app.Application
import android.text.Html
import androidx.lifecycle.AndroidViewModel
import com.rudderstack.sampleapp.analytics.RudderAnalyticsUtils
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.EventType
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Properties
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MainViewModelState())
    private val analytics = RudderAnalyticsUtils.analytics
    val state = _state.asStateFlow()

    init {
        setupPayloadInterceptorPlugin()
    }

    /**
     * Sets up the analytics [Plugin] for intercepting events and updating logs.
     * This plugin captures all analytics events and processes them for display in the UI.
     */
    private fun setupPayloadInterceptorPlugin() {
        val plugin = object : Plugin {
            override val pluginType: Plugin.PluginType = Plugin.PluginType.Terminal
            override lateinit var analytics: com.rudderstack.sdk.kotlin.core.Analytics
            override suspend fun intercept(event: Event): Event? {
                processEvent(event)
                return event
            }
        }
        analytics.add(plugin)
    }

    /**
     * Processes an intercepted analytics [Event] and updates the log display.
     * Converts the event into a readable format and adds it to the log history.
     *
     * @param event The analytics event to be processed
     */
    @SuppressLint("NewApi")
    private fun processEvent(event: Event) {
        val eventStr = formatEventAsString(event)
        val styledLog = Html.fromHtml(colorFormat(eventStr), Html.FROM_HTML_MODE_LEGACY).toString()

        _state.update { currentState ->
            currentState.copy(log = styledLog)
        }
    }

    /**
     * Formats an analytics [Event] into a human-readable string representation.
     * Different event types (Track, Screen, Group, etc.) are formatted differently.
     *
     * @param event The event to be formatted
     * @return A formatted string representation of the event
     */
    private fun formatEventAsString(event: Event): String {
        return when (event.type) {
            EventType.Track -> eventStr(event as TrackEvent)
            EventType.Screen -> eventStr(event as ScreenEvent)
            EventType.Group -> eventStr(event as GroupEvent)
            EventType.Identify -> eventStr(event as IdentifyEvent)
            EventType.Alias -> eventStr(event as AliasEvent)
        }
    }

    /**
     * Handles user interactions with analytics message buttons.
     * Triggers appropriate analytics events based on the selected state.
     *
     * @param analyticsState The state representing which type of analytics event to send
     */
    internal fun onMessageClicked(analyticsState: MainViewModelState.AnalyticsState) {
        when (analyticsState) {
            is MainViewModelState.AnalyticsState.InitialState -> navigateToScreens()
            is MainViewModelState.AnalyticsState.TrackMessage -> sendTrackEvent()
            is MainViewModelState.AnalyticsState.ScreenMessage -> sendScreenEvent()
            is MainViewModelState.AnalyticsState.GroupMessage -> sendGroupEvent()
            is MainViewModelState.AnalyticsState.IdentifyMessage -> sendIdentifyEvent()
            is MainViewModelState.AnalyticsState.AliasMessage -> sendAliasEvent()
            is MainViewModelState.AnalyticsState.ForceFlush -> flushAnalytics()
            is MainViewModelState.AnalyticsState.Shutdown -> analytics.shutdown()
            is MainViewModelState.AnalyticsState.Reset -> analytics.reset()
            is MainViewModelState.AnalyticsState.StartSession -> analytics.startSession()
            is MainViewModelState.AnalyticsState.StartSessionWithCustomId -> analytics.startSession(sessionId = 1000000001)
            is MainViewModelState.AnalyticsState.EndSession -> analytics.endSession()
            is MainViewModelState.AnalyticsState.NavigateToScreens -> navigateToScreens()
        }
    }

    /**
     * Forces a flush of queued analytics events and clears the current log display.
     *
     * This function performs two operations:
     * 1. Triggers an immediate flush of any queued analytics events to the server
     * 2. Clears the current log display by setting it to an empty string
     */
    private fun flushAnalytics() {
        analytics.flush()
        _state.update { currentState ->
            currentState.copy(log = "")
        }
    }

    /**
     * Handles navigation between screens and updates the navigation state.
     */
    private fun navigateToScreens() {
        _state.update { currentState ->
            currentState.copy(state = MainViewModelState.AnalyticsState.NavigateToScreens)
        }
    }

    /**
     * Sends a Track event with sample properties.
     * Track events represent user actions or events that occurred in the app.
     */
    private fun sendTrackEvent() {
        analytics.track(
            name = "Track Example",
            properties = Properties(emptyMap()),
            options = RudderOption()
        )
    }

    /**
     * Sends a Screen event with screen viewing information.
     * Screen events represent user navigation to different screens in the app.
     */
    private fun sendScreenEvent() {
        analytics.screen(
            screenName = "Main Screen",
            properties = Properties(emptyMap()),
            options = RudderOption()
        )
    }

    /**
     * Sends a Group event with organization identification.
     * Group events associate a user with a group/organization.
     */
    private fun sendGroupEvent() {
        analytics.group(
            groupId = "Group ID",
            traits = RudderTraits(emptyMap()),
            options = RudderOption()
        )
    }

    /**
     * Sends an Identify event with user traits.
     * Identify events update user properties and characteristics.
     */
    private fun sendIdentifyEvent() {
        analytics.identify(
            userId = "User123",
            traits = RudderTraits(emptyMap()),
            options = RudderOption()
        )
    }

    /**
     * Sends an Alias event to connect different user identifiers.
     * Alias events connect anonymous users to identified users.
     */
    private fun sendAliasEvent() {
        analytics.alias(
            newId = "NewAlias",
            previousId = "OldAlias",
            options = RudderOption()
        )
    }

    /**
     * Toggles the Advertising ID plugin on/off.
     *
     * @param enable True to enable the plugin, false to disable
     */
    fun toggleAdvertisingIdPlugin(enable: Boolean) {
        if (enable) {
            RudderAnalyticsUtils.addAndroidAdvertisingIdPlugin()
        } else {
            RudderAnalyticsUtils.removeAndroidAdvertisingIdPlugin()
        }
        _state.update { currentState -> currentState.copy(isAdvertisingIdEnabled = enable) }
    }

    /**
     * Formats a log message with HTML color styling.
     *
     * @param text The text to be colored
     * @return HTML formatted string with color styling
     */
    private fun colorFormat(text: String): String {
        val spacer = fun(match: MatchResult): CharSequence {
            return "<br>" + "&nbsp;".repeat(match.value.length - 1)
        }

        val newString = text
            .replace("\".*\":".toRegex(), "<font color=#52BD94>$0</font>")
            .replace("\\n\\s*".toRegex(), spacer)
        return newString
    }

    /**
     * Converts an analytics Event object to its JSON string representation.
     *
     * @param event The event to be converted
     * @return JSON string representation of the event
     */
    private inline fun <reified T : Event> eventStr(event: T): String {
        return Json {
            prettyPrint = true
            encodeDefaults = true
        }.encodeToString(event)
    }

    /**
     * Resets the navigation state to initial values.
     * Called after navigation is completed to prepare for next navigation action.
     */
    fun resetNavigationState() {
        _state.update { currentState ->
            currentState.copy(state = MainViewModelState.AnalyticsState.InitialState)
        }
    }
}