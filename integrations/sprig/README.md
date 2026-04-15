# Sprig Integration

The Sprig integration allows you to send identify and track events to Sprig (formerly UserLeap) through RudderStack, enabling in-product surveys and user research.

## Installation

Add the Sprig integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the Sprig integration
    implementation("com.rudderstack.sdk.kotlin:sprig:<latest_version>")
}
```

## Supported Native Sprig Version

This integration supports Sprig (UserLeap) Android SDK version:

```
2.23.0
```

## Usage

Initialize the RudderStack SDK and add the Sprig integration:

```kotlin
import com.rudderstack.integration.kotlin.sprig.SprigIntegration
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
        
        // Add Sprig integration
        analytics.add(SprigIntegration())
    }
}
```
