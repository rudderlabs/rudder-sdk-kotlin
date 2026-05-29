# Sprig Integration

The Sprig integration allows you to send identify and track events to Sprig (formerly UserLeap) through RudderStack, enabling in-product surveys and user research.

## Installation

Add the Sprig integration to your project:

```kotlin
dependencies {
    // Add the RudderStack Android SDK
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")

    // Add the Sprig integration
    implementation("com.rudderstack.integration.kotlin:sprig:<latest_version>")
}
```

## Supported Native Sprig Version

This integration supports Sprig (UserLeap) Android SDK versions in the range:

```
[2.23.0, 3.0.0)
```

The integration pulls in `2.23.0` by default; your app can override to any release that satisfies the range above.

## Usage

Initialize the RudderStack SDK and add the Sprig integration. Keep a reference to the `SprigIntegration` instance so it can be reused by your activities (see [Presenting in-app surveys](#presenting-in-app-surveys)):

```kotlin
import android.app.Application
import com.rudderstack.integration.kotlin.sprig.SprigIntegration
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics
    val sprigIntegration = SprigIntegration()

    fun initialize(application: Application) {
        analytics = Analytics(
            configuration = Configuration(
                writeKey = "<WRITE_KEY>",
                application = application,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )
        analytics.add(sprigIntegration)
    }
}
```

Call `RudderAnalyticsUtils.initialize(this)` from your `Application.onCreate()`:

```kotlin
import android.app.Application

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        RudderAnalyticsUtils.initialize(this)
    }
}
```

### Presenting in-app surveys

Sprig presents in-app surveys (via `trackAndPresent`) on a `FragmentActivity`. For surveys to render, the host app must tell the integration which activity is currently in the foreground by calling `setFragmentActivity` from the activity's lifecycle callbacks:

```kotlin
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    override fun onResume() {
        super.onResume()
        RudderAnalyticsUtils.sprigIntegration.setFragmentActivity(this)
    }

    override fun onPause() {
        RudderAnalyticsUtils.sprigIntegration.setFragmentActivity(null)
        super.onPause()
    }
}
```

Clearing the reference in `onPause` prevents Sprig from attempting a fragment transaction on an activity that has already saved its instance state. When no activity is set (or the stored activity is not in at least the `STARTED` state), the integration falls back to `Sprig.track` and in-app surveys are not presented.
