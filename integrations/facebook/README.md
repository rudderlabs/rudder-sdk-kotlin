# Facebook Integration

The Facebook integration allows you to send your event data from RudderStack to Facebook for analytics and advertising.

## Installation

Add the Facebook integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the Facebook integration
    implementation("com.rudderstack.sdk.kotlin:facebook:<latest_version>")
}
```

## Usage

Initialize the RudderStack SDK and add the Facebook integration:

```kotlin
import com.rudderstack.integration.kotlin.facebook.FacebookIntegration
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
        
        // Add Facebook integration
        analytics.addIntegration(FacebookIntegration())
    }
}
```
