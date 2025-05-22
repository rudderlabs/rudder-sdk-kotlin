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
        analytics.addIntegration(AdjustIntegration())
    }
}
```
