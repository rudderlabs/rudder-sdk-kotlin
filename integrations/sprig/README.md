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

## Use your own Sprig instance

In the default setup, the integration configures Sprig from the destination configuration. Instead, your app can
configure Sprig and pass that instance to the integration. The integration then only maps events and sends them to
Sprig. This needs integration version 2.1.0 or higher.

1. Add the Sprig Android SDK to your app dependencies. Use a version in the range `[2.23.0, 3.0.0)`. The integration
   adds the Sprig SDK at runtime scope only, so your code can't call Sprig without this dependency.

```kotlin
dependencies {
    implementation("com.userleap:userleap-android-sdk:<version>")
}
```

2. In `RudderAnalyticsUtils`, create the integration with the Sprig instance, and configure Sprig at the start of
   `initialize`:

```kotlin
import com.userleap.Sprig

object RudderAnalyticsUtils {

    lateinit var analytics: Analytics
    val sprigIntegration = SprigIntegration(Sprig)

    fun initialize(application: Application) {
        Sprig.configure(application, "<SPRIG_ENVIRONMENT_ID>")

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

If your app already configures Sprig, keep that setup and skip the `configure` call. Configure Sprig before you add
the integration.

With your own instance, the integration:

- Doesn't call `Sprig.configure`.
- Still forwards the Sprig log output to the RudderStack logger.
- Still presents surveys in the same way. Call `setFragmentActivity` as [Presenting in-app surveys](#presenting-in-app-surveys) describes.
- Still calls Sprig `logout` on `reset`.
- Waits for the source configuration before it sends events.

The **Environment ID** connection setting still needs a value. The integration doesn't use it.
