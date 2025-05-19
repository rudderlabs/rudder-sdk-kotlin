package com.rudderstack.sampleapp.analytics.javacompat;

import androidx.annotation.NonNull;

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger;

/**
 * Custom logger implementation for Java analytics.
 * This class implements the Logger interface and provides custom logging methods.
 */
public class JavaCustomLogger implements Logger {
    private final String TAG = "MyCustomTag";

    @Override
    public void verbose(@NonNull String log) {
        System.out.println(TAG + ": Verbose: " + log);
    }

    @Override
    public void debug(@NonNull String log) {
        System.out.println(TAG + ": Debug: " + log);
    }

    @Override
    public void info(@NonNull String log) {
        System.out.println(TAG + ": Info: " + log);
    }

    @Override
    public void warn(@NonNull String log) {
        System.out.println(TAG + ": Warn: " + log);
    }

    @Override
    public void error(@NonNull String log, Throwable throwable) {
        System.out.println(TAG + ": Error: " + log);
    }
}
