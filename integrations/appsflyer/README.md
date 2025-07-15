# AppsFlyer Integration for RudderStack Kotlin SDK

The AppsFlyer integration allows you to send your event data from RudderStack to AppsFlyer for mobile attribution and analytics. This integration supports all major RudderStack event types and automatically maps them to appropriate AppsFlyer events.

## Supported Native AppsFlyer Version

This integration supports AppsFlyer Android SDK versions:

```
[6.10.1, 7.0)
```

## Installation and Usage

### 1. Add Dependency

Add the AppsFlyer integration dependency and AppsFlyer dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the AppsFlyer integration
    implementation("com.rudderstack.sdk.kotlin:appsflyer:<latest_version>")

    implementation ("com.appsflyer:af-android-sdk:<latest_version>")
}
```

### 2. Initialise AppsFlyer SDK in Application Class

Initialize Appsflyer SDK in the Application class before initializing the Rudder SDK as shown below

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
        //            ...

        // Add AppsFlyer integration
        //            ...
    }
}


```

### 2. Initialize the Integration

Initialize the RudderStack SDK and add the AppsFlyer integration:

```kotlin
import com.rudderstack.integration.kotlin.appsflyer.AppsFlyerIntegration
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration

class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()

        // Initialize AppsFlyer SDK
        // ... (as shown above)
        
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
