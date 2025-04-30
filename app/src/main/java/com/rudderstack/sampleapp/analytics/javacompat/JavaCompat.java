package com.rudderstack.sampleapp.analytics.javacompat;

import static com.rudderstack.sdk.kotlin.core.javacompat.JsonInteropHelper.fromMap;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.rudderstack.sdk.kotlin.android.Configuration;
import com.rudderstack.sdk.kotlin.android.SessionConfiguration;
import com.rudderstack.sdk.kotlin.android.javacompat.ConfigurationBuilder;
import com.rudderstack.sdk.kotlin.android.javacompat.SessionConfigurationBuilder;
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId;
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption;
import com.rudderstack.sdk.kotlin.core.javacompat.JavaAnalytics;
import com.rudderstack.sdk.kotlin.core.javacompat.RudderOptionBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kotlinx.serialization.json.JsonObject;

public class JavaCompat {

    private final JavaAnalytics analytics;

    public JavaCompat(@NonNull Application application, @NonNull String writeKey, @NonNull String dataPlaneUrl) {
        analytics = analyticsFactory(application, writeKey, dataPlaneUrl);
    }

    @VisibleForTesting
    public JavaCompat(JavaAnalytics analytics) {
        this.analytics = analytics;
    }

    /**
     * Initialize the JavaAnalytics instance with the given parameters.
     * <p>
     * This method sets up the analytics instance with the provided write key and data plane URL.
     * Code:
     *
     * <pre>{@code
     * JavaAnalytics analytics = JavaCompat.initAnalytics(application, writeKey, dataPlaneUrl);
     * }</pre>
     *
     * @param application  The application context.
     * @param writeKey     The write key for the analytics service.
     * @param dataPlaneUrl The data plane URL for the analytics service.
     * @return A JavaAnalytics instance configured with the provided parameters.
     */
    @VisibleForTesting
    @NonNull
    public static JavaAnalytics analyticsFactory(@NonNull Application application, @NonNull String writeKey, @NonNull String dataPlaneUrl) {
        SessionConfiguration sessionConfiguration = new SessionConfigurationBuilder()
                .withAutomaticSessionTracking(true)
                .withSessionTimeoutInMillis(30)
                .build();

        Configuration configuration = new ConfigurationBuilder(application, writeKey, dataPlaneUrl)
                .withTrackApplicationLifecycleEvents(true)
                .withSessionConfiguration(sessionConfiguration)
                .withGzipEnabled(true)
                .build();

        return new JavaAnalytics(configuration);
    }

    /**
     * Make a track event.
     * <p>
     * Sample code:
     *
     * <pre>{@code
     * JavaCompat.track();
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
     * JavaCompat.screen();
     * }</pre>
     */
    public void screen() {
        String name = "Sample screen event";
        String category = "Sample screen category";
        Map<String, Object> properties = getMap();
        RudderOption option = getRudderOption();

        analytics.screen(name);
        analytics.screen(name, category);
        analytics.screen(name, properties);
        analytics.screen(name, option);
        analytics.screen(name, properties, option);
        analytics.screen(name, category, properties);
        analytics.screen(name, category, option);
        analytics.screen(name, category, properties, option);
    }

    /**
     * Make a group event.
     * <p>
     * Sample code:
     *
     * <pre>{@code
     * JavaCompat.group();
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
     * JavaCompat.identify();
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

    @NonNull
    private static JsonObject getJsonObject() {
        Map<String, Object> value = getMap();
        return fromMap(value);
    }
}
