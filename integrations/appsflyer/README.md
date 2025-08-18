# AppsFlyer Integration

The AppsFlyer integration allows you to send your event data from RudderStack to AppsFlyer for mobile attribution and analytics. This integration supports all major RudderStack event types and automatically maps them to appropriate AppsFlyer events.

## Installation

Add the AppsFlyer Integration and AppsFlyer's dependencies to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the AppsFlyer integration
    implementation("com.rudderstack.integration.kotlin:appsflyer:<latest_version>")

    // AppsFlyer Android SDK
    implementation ("com.appsflyer:af-android-sdk:<latest_version>")
}
```

## Supported Native AppsFlyer Version

This integration supports AppsFlyer Android SDK version:

```
6.17.0
```

## Usage

Initialize Appsflyer SDK in the Application class before initializing the Rudder SDK as shown below. Then, initialize the RudderStack SDK and add the AppsFlyer integration.

```kotlin
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AFLogger

class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()

        // Initialize AppsFlyer SDK
        AppsFlyerLib.getInstance().init("<DEV_KEY>", null, this)
        AppsFlyerLib.getInstance().setLogLevel(AFLogger.LogLevel.DEBUG)
        AppsFlyerLib.getInstance().start(this)

        // Initialize RudderStack SDK
        analytics = Analytics(
            configuration = Configuration(
                writeKey = "<WRITE_KEY>",
                application = this,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )

        // Add AppsFlyer integration
        analytics.add(AppsFlyerIntegration())
    }
}


```
