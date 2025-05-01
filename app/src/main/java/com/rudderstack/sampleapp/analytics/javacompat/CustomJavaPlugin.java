package com.rudderstack.sampleapp.analytics.javacompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rudderstack.sdk.kotlin.core.Analytics;
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics;
import com.rudderstack.sdk.kotlin.core.internals.models.Event;
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin;

import kotlin.coroutines.Continuation;

/**
 * A custom Java Plugin demonstrating how to implement a plugin in Java.
 */
public class CustomJavaPlugin implements Plugin {
    private Analytics analytics;
    private static final Plugin.PluginType pluginType = Plugin.PluginType.OnProcess;

    @Override
    public void setAnalytics(@NonNull Analytics analytics) {
        this.analytics = analytics;
    }

    @Override
    public void setup(@NonNull Analytics analytics) {
        this.analytics = analytics;
    }

    @Nullable
    @Override
    public Object intercept(@NonNull Event event, @NonNull Continuation<? super Event> $completion) {
        LoggerAnalytics.INSTANCE.verbose("CustomJavaPlugin: Intercepting event: " + event);
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
        // Perform any necessary cleanup here
    }
}
