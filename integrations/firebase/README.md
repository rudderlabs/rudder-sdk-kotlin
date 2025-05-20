# Firebase Integration

The Firebase integration allows you to send your event data from RudderStack to Google Firebase Analytics.

## Installation

Add the Firebase integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the Firebase integration
    implementation("com.rudderstack.sdk.kotlin:firebase:<latest_version>")
    
    // Required Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:<latest_version>"))
    implementation("com.google.firebase:firebase-analytics")
}
```

## Supported Native Firebase Version

This integration supports Google Firebase Analytics via the following Firebase BoM versions:

```
[32.2.2, 33.8.0)
```

## Usage

Initialize the RudderStack SDK and add the Firebase integration:

```kotlin
import com.rudderstack.integration.kotlin.firebase.FirebaseIntegration
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
        
        // Add Firebase integration
        analytics.addIntegration(FirebaseIntegration())
    }
}
```
