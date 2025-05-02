package com.rudderstack.sampleapp.analytics.javacompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rudderstack.sdk.kotlin.core.Analytics;
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics;
import com.rudderstack.sdk.kotlin.core.internals.models.Event;
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent;
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin;

import java.util.List;

import kotlin.coroutines.Continuation;

/**
 * A custom Java Plugin demonstrating how to perform event filtering in Java.
 */
public class JavaEventFilteringPlugin implements Plugin {
    private Analytics analytics;
    private static final Plugin.PluginType pluginType = Plugin.PluginType.OnProcess;

    List<String> listOfEventsToBeFiltered;

    @Override
    public void setAnalytics(@NonNull Analytics analytics) {
        this.analytics = analytics;
    }

    @Override
    public void setup(@NonNull Analytics analytics) {
        this.analytics = analytics;
        listOfEventsToBeFiltered = List.of("Application Opened", "Application Backgrounded");
    }

    @Nullable
    @Override
    public Object intercept(@NonNull Event event, @NonNull Continuation<? super Event> $completion) {
        if (event instanceof TrackEvent && listOfEventsToBeFiltered.contains(((TrackEvent) event).getEvent())) {
            LoggerAnalytics.INSTANCE.verbose("EventFilteringPlugin: Event " + ((TrackEvent) event).getEvent() + " is filtered out");
            return null;
        }

        return event;
    }

    @NonNull
    @Override
    public Analytics getAnalytics() {
        return this.analytics;
    }

    @NonNull
    @Override
    public Plugin.PluginType getPluginType() {
        return pluginType;
    }

    @Override
    public void teardown() {
        listOfEventsToBeFiltered = null;
    }
}
