package com.rudderstack.sampleapp.analytics.customplugins;

import static com.rudderstack.sdk.kotlin.core.javacompat.JsonInteropHelper.fromMap;

import android.app.Application;

import androidx.annotation.NonNull;

import com.rudderstack.sdk.kotlin.android.Configuration;
import com.rudderstack.sdk.kotlin.android.SessionConfiguration;
import com.rudderstack.sdk.kotlin.android.javacompat.ConfigurationBuilder;
import com.rudderstack.sdk.kotlin.android.javacompat.SessionConfigurationBuilder;
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId;
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption;
import com.rudderstack.sdk.kotlin.core.javacompat.JavaAnalytics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kotlinx.serialization.json.JsonObject;

public class JavaCompat {

    private JavaCompat() {
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
    @NonNull
    public static JavaAnalytics initAnalytics(@NonNull Application application, @NonNull String writeKey, @NonNull String dataPlaneUrl) {
        SessionConfiguration sessionConfiguration = new SessionConfigurationBuilder()
                .withAutomaticSessionTracking(true)
                .withSessionTimeoutInMillis(30)
                .build();

        Configuration configuration = new ConfigurationBuilder(application, writeKey, dataPlaneUrl)
                .withTrackApplicationLifecycleEvents(true)
                .withSessionConfiguration(sessionConfiguration)
                .withGzipEnabled(true)
                .build();

        JavaAnalytics javaAnalytics = new JavaAnalytics(configuration);
        JavaCompat.track(javaAnalytics);
        return javaAnalytics;
    }

    /**
     * Track an event using the provided JavaAnalytics instance.
     * <p>
     * This method demonstrates how to track events with different parameters.
     * Code:
     *
     * <pre>{@code
     * JavaCompat.track(analytics);
     * }</pre>
     *
     * @param analytics The JavaAnalytics instance to use for tracking events.
     */
    public static void track(@NonNull JavaAnalytics analytics) {
        String name = "Sample track event";
        JsonObject properties = getJsonObject();
        RudderOption option = getRudderOption();

        analytics.track(name);
        analytics.track(name, properties);
        analytics.track(name, option);
        analytics.track(name, properties, option);
    }

    @NonNull
    private static JsonObject getJsonObject() {
        Map<String, Object> value = new HashMap<>();
        value.put("key-1", "value-1");
        value.put("age", 30);
        value.put("isActive", true);
        value.put("scores", Arrays.asList(10, 20, 30));
        value.put("details", Map.of("city", "Delhi", "zip", 12345));

        return fromMap(value);
    }

    @NonNull
    private static RudderOption getRudderOption() {
        JsonObject integrations = getIntegrations();
        List<ExternalId> externalIds = getExternalIds();
        JsonObject customContext = getJsonObject();
        return new RudderOption(integrations, externalIds, customContext);
    }

    @NonNull
    private static JsonObject getIntegrations() {
        Map<String, Object> integrations = new LinkedHashMap<>();
        integrations.put("All", true);
        integrations.put("Google Analytics", false);
        return fromMap(integrations);
    }

    @NonNull
    private static List<ExternalId> getExternalIds() {
        ExternalId externalId1 = new ExternalId("externalId1", "value1");
        ExternalId externalId2 = new ExternalId("externalId2", "value2");
        return Arrays.asList(externalId1, externalId2);
    }
}
