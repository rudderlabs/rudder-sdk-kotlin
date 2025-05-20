# Braze Integration

The Braze integration allows you to send your event data from RudderStack to Braze for customer engagement.

## Requirements

- Android SDK version 25 or higher

## Installation

Add the Braze integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the Braze integration
    implementation("com.rudderstack.sdk.kotlin:braze:<latest_version>")
}
```

## Usage

Initialize the RudderStack SDK and add the Braze integration:

```kotlin
import com.rudderstack.integration.kotlin.braze.BrazeIntegration
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
        
        // Add Braze integration
        analytics.addIntegration(BrazeIntegration())
    }
}
```
