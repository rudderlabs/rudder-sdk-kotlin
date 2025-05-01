package com.rudderstack.sampleapp.analytics.javacompat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.rudderstack.sdk.kotlin.android.Configuration;
import com.rudderstack.sdk.kotlin.android.SessionConfiguration;
import com.rudderstack.sdk.kotlin.android.javacompat.ConfigurationBuilder;
import com.rudderstack.sdk.kotlin.android.javacompat.SessionConfigurationBuilder;
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId;
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption;
import com.rudderstack.sdk.kotlin.android.javacompat.JavaAnalytics;
import com.rudderstack.sdk.kotlin.core.javacompat.RudderOptionBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides a compatibility layer for Java analytics events.
 * <p>
 *
 * Sample code:
 * <pre>{@code
 * JavaCompat javaCompat = new JavaCompat(application, writeKey, dataPlaneUrl);
 * }</pre>
 */
public class JavaCompat {

    private final JavaAnalytics analytics;

    /**
     * Constructor to initialize the JavaCompat instance with the given parameters.
     *
     * @param application  The application context.
     * @param writeKey     The write key for the analytics service.
     * @param dataPlaneUrl The data plane URL for the analytics service.
     */
    public JavaCompat(@NonNull Application application, @NonNull String writeKey, @NonNull String dataPlaneUrl) {
        analytics = analyticsFactory(application, writeKey, dataPlaneUrl);
    }

    @VisibleForTesting
    public JavaCompat(JavaAnalytics analytics) {
        this.analytics = analytics;
    }

    @NonNull
    private static JavaAnalytics analyticsFactory(@NonNull Application application, @NonNull String writeKey, @NonNull String dataPlaneUrl) {
        SessionConfiguration sessionConfiguration = new SessionConfigurationBuilder()
                .setAutomaticSessionTracking(true)
                .setSessionTimeoutInMillis(30)
                .build();

        Configuration configuration = (Configuration) new ConfigurationBuilder(application, writeKey, dataPlaneUrl)
                .setTrackApplicationLifecycleEvents(true)
                .setSessionConfiguration(sessionConfiguration)
                .setGzipEnabled(true)
                .build();

        return new JavaAnalytics(configuration);
    }

    /**
     * Make a track event.
     * <p>
     * Sample code:
     *
     * <pre>{@code
     * javaCompat.track();
     * }</pre>
     */
    public void track() {
        String name = "Sample track event";
        Map<String, Object> properties = getMap();
        RudderOption option = getRudderOption();

        analytics.track(name);
        analytics.track(name, properties);
        analytics.track(name, option);
        analytics.track(name, properties, option);
    }

    /**
     * Make a screen event.
     * <p>
     * Sample code:
     *
     * <pre>{@code
     * javaCompat.screen();
     * }</pre>
     */
    public void screen() {
        String screenName = "Sample screen event";
        String category = "Sample screen category";
        Map<String, Object> properties = getMap();
        RudderOption option = getRudderOption();

        analytics.screen(screenName);
        analytics.screen(screenName, category);
        analytics.screen(screenName, properties);
        analytics.screen(screenName, option);
        analytics.screen(screenName, properties, option);
        analytics.screen(screenName, category, properties);
        analytics.screen(screenName, category, option);
        analytics.screen(screenName, category, properties, option);
    }

    /**
     * Make a group event.
     * <p>
     * Sample code:
     *
     * <pre>{@code
     * javaCompat.group();
     * }</pre>
     */
    public void group() {
        String groupId = "Sample group ID";
        Map<String, Object> traits = getMap();
        RudderOption option = getRudderOption();

        analytics.group(groupId);
        analytics.group(groupId, traits);
        analytics.group(groupId, option);
        analytics.group(groupId, traits, option);
    }

    /**
     * Make an identify event.
     * <p>
     * Sample code:
     *
     * <pre>{@code
     * javaCompat.identify();
     * }</pre>
     */
    public void identify() {
        String userId = "Sample user ID";
        Map<String, Object> traits = getMap();
        RudderOption option = getRudderOption();

        analytics.identify(userId);
        analytics.identify(traits);
        analytics.identify(userId, traits);
        analytics.identify(userId, option);
        analytics.identify(traits, option);
        analytics.identify(userId, traits, option);
    }

    /**
     * Make an alias event.
     * <p>
     * Sample code:
     *
     * <pre>{@code
     * javaCompat.alias();
     * }</pre>
     */
    public void alias() {
        String newId = "Sample alias";
        String previousId = "Sample previous ID";
        RudderOption option = getRudderOption();

        analytics.alias(newId);
        analytics.alias(newId, option);
        analytics.alias(newId, previousId);
        analytics.alias(newId, previousId, option);
    }

    @NonNull
    private static Map<String, Object> getMap() {
        Map<String, Object> value = new HashMap<>();
        value.put("key-1", "value-1");
        value.put("age", 30);
        value.put("isActive", true);
        value.put("scores", Arrays.asList(10, 20, 30));
        value.put("details", Map.of("city", "Delhi", "zip", 12345));

        return value;
    }

    @NonNull
    private static RudderOption getRudderOption() {
        Map<String, Object> integrations = getIntegrations();
        List<ExternalId> externalIds = getExternalIds();
        Map<String, Object> customContext = getMap();
        return new RudderOptionBuilder()
                .setIntegrations(integrations)
                .setExternalId(externalIds)
                .setCustomContext(customContext)
                .build();
    }

    @NonNull
    private static Map<String, Object> getIntegrations() {
        Map<String, Object> integrations = new LinkedHashMap<>();
        integrations.put("All", true);
        integrations.put("Google Analytics", false);
        return integrations;
    }

    @NonNull
    private static List<ExternalId> getExternalIds() {
        ExternalId externalId1 = new ExternalId("externalId1", "value1");
        ExternalId externalId2 = new ExternalId("externalId2", "value2");
        return Arrays.asList(externalId1, externalId2);
    }
}
