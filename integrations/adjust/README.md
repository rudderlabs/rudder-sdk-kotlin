# Adjust Integration

The Adjust integration allows you to track and attribute your mobile app installs and in-app events with Adjust through RudderStack.

## Installation

Add the Adjust integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the Adjust integration
    implementation("com.rudderstack.sdk.kotlin:adjust:<latest_version>")
}
```

## Supported Native Adjust Version

This integration supports Adjust Android SDK version:

```
5.1.0
```

## Usage

Initialize the RudderStack SDK and add the Adjust integration:

```kotlin
import com.rudderstack.integration.kotlin.adjust.AdjustIntegration
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration

class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()
        
        // Initialize RudderStack SDK
        analytics = Analytics(
            configuration = Configuration(
                writeKey = "<WRITE_KEY>",
                application = this,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )
        
        // Add Adjust integration
        analytics.add(AdjustIntegration())
    }
}
```

## Lite mode

In lite mode, your app initialises Adjust and passes the instance to the integration. The integration then only maps
events to Adjust event tokens and sends them.

1. Initialise Adjust with your own `AdjustConfig`: app token, environment, listeners, and the attribution callback.
2. Pass the Adjust instance to the integration:

```kotlin
analytics.add(AdjustIntegration(Adjust.getDefaultInstance()))
```

In lite mode, the integration:

- Does not initialise Adjust, set listeners, or forward the activity resume and pause calls to Adjust.
- Sends no "Install Attributed" event. If you need it, send it with your own `track` call from your attribution
  callback. The dashboard setting `enableInstallAttributionTracking` has no effect.
- Uses the event-to-token mapping from the dashboard, and adds the `anonymousId` and `userId` partner parameters, as
  in the standard mode. `reset` removes all global partner parameters, also the ones that your app added.
- Waits for the source config before it sends events. The dashboard settings still need valid values, but lite mode
  does not use the app token.
