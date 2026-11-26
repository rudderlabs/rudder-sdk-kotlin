# Firebase Integration

The Firebase integration allows you to send your event data from RudderStack to Google Firebase Analytics.

## Installation

Add the Firebase integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    
    // Add the Firebase integration
    implementation("com.rudderstack.integration.kotlin:firebase:<latest_version>")
    
    // Required Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:<latest_version>"))
    implementation("com.google.firebase:firebase-analytics")
}
```

## Requirements

- Android SDK version 24 or higher
- Kotlin 2.3 or higher in your app. Firebase BoM 35 libraries carry Kotlin 2.4 metadata, which older Kotlin compilers cannot read, even when your code makes no Firebase calls.

## Supported Native Firebase Version

This integration supports Google Firebase Analytics via the following Firebase BoM versions:

```
[35.0.0, 36.0.0)
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
        analytics.add(FirebaseIntegration())
    }
}
```
