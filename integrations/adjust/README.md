# Adjust Integration

The Adjust integration allows you to track and attribute your mobile app installs and in-app events with Adjust through RudderStack.

## Installation

Add the Adjust integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the Adjust integration
    implementation("com.rudderstack.integration.kotlin:adjust:<latest_version>")
}
```

## Supported Native Adjust Version

This integration supports Adjust Android SDK versions in the range:

```text
[5.1.0, 6.0.0)
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

## Use your own Adjust instance

In the default setup, the integration initializes Adjust from the destination configuration. Instead, your app can
initialize Adjust and pass that instance to the integration. The integration then only maps events to Adjust event
tokens and sends them. This needs integration version 2.1.0 or higher.

1. Add the Adjust Android SDK to your app dependencies. Use a version in the range `[5.1.0, 6.0.0)`. The integration
   adds the Adjust SDK at runtime scope only, so your code can't call Adjust without this dependency.

```kotlin
dependencies {
    implementation("com.adjust.sdk:adjust-android:<version>")
}
```

2. Initialize Adjust with your own `AdjustConfig`, for example, the app token, the environment, and your listeners.
3. Pass the Adjust instance to the integration:

```kotlin
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.rudderstack.integration.kotlin.adjust.AdjustIntegration

val adjustConfig = AdjustConfig(this, "<ADJUST_APP_TOKEN>", AdjustConfig.ENVIRONMENT_PRODUCTION)
adjustConfig.setOnAttributionChangedListener { attribution ->
    // Handle the attribution
}
Adjust.initSdk(adjustConfig)

analytics.add(AdjustIntegration(Adjust.getDefaultInstance()))
```

With your own instance, the integration:

- Doesn't initialize Adjust, set its log level, set listeners, or forward the activity resume and pause calls.
- Doesn't turn on sending in the background. The default setup turns it on. To keep it, call
  `adjustConfig.enableSendingInBackground()` before `Adjust.initSdk`.
- Doesn't send the "Install Attributed" event, even when the destination configuration turns it on. To send it, call
  `track` from your own attribution callback.
- Still maps events with the event-to-token mapping in the destination configuration, and adds the `anonymousId` and
  `userId` partner parameters.
- Waits for the source configuration before it sends events.

The **App Token** connection setting still needs a value. The integration doesn't use it.

In both setups, `reset` removes all Adjust global partner parameters, also the ones that your app added.
