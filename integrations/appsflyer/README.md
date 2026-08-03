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
7.x (>= 7.0.1, < 8.0.0)
```

> **Why 7.0.1 and not 7.0.0?** AppsFlyer removed `setUserEmails(vararg String)` in 7.0.1 and replaced
> it with `setUserEmail(String)`. This integration compiles against the 7.0.1 API, so AppsFlyer 7.0.0
> is not supported — using it would fail at runtime with `NoSuchMethodError`.

> **Using AppsFlyer v6?** Stay on the `1.x` line of this integration. Version `2.0.0` and above require
> AppsFlyer Android SDK v7 and cannot resolve alongside AppsFlyer 6.x. See AppsFlyer's
> [Android v6 → v7 migration guide](https://dev.appsflyer.com/hc/docs/migrate-android-sdk-to-v7)
> before upgrading. Note that v7 also changes how the `identify` email reaches AppsFlyer — see
> [Identify and email handling](#identify-and-email-handling) below.

AppsFlyer v7 also raises the minimum Android API level to 21 and pulls in
`com.google.android.play:integrity` as a new transitive dependency.

## Usage

Initialize the AppsFlyer SDK in your Application class before initializing the RudderStack SDK, then
add the AppsFlyer integration.

In AppsFlyer v7, `start()` no longer takes a `Context` and must be called from inside a
`registerSessionReadyListener` block:

```kotlin
import com.appsflyer.AFLogger
import com.appsflyer.AppsFlyerLib

class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()

        // Initialize AppsFlyer SDK
        AppsFlyerLib.getInstance().setLogLevel(AFLogger.LogLevel.DEBUG)
        AppsFlyerLib.getInstance().init("<DEV_KEY>", null, this)
        AppsFlyerLib.getInstance().registerSessionReadyListener {
            AppsFlyerLib.getInstance().start()
        }

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

> **Note:** If `init()` and `start()` are not called, this integration still forwards events to the
> AppsFlyer SDK, but nothing reaches AppsFlyer and no error is reported.

### Install referrer collection

In AppsFlyer v6 the SDK collected install referrer data automatically via broadcast receivers. In v7
those receivers were removed and collection is opt-in. If you rely on referrer attribution, call
`collectDataFromLauncherActivity` from your launcher activity's `onCreate` before `start()` runs for
that cold start:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppsFlyerLib.getInstance().collectDataFromLauncherActivity(this)
    }
}
```

> AppsFlyer's v7 migration guide lists `com.android.installreferrer:installreferrer` as a required
> dependency. You do not need to add it yourself — this integration already declares it, so it comes
> in transitively.

### Identify and email handling

AppsFlyer v7 changed how the SDK transmits the email supplied via `identify`.

| | AppsFlyer v6 | AppsFlyer v7 |
|---|---|---|
| API this integration calls | `setUserEmails(email)` | `setUserEmail(email)` |
| Field sent to AppsFlyer | `user_emails` | `email_hashed` |
| Form | plaintext | SHA-256 hash |

In v6 the email was attached in plaintext to every subsequent in-app event. In v7 the SDK hashes it
before transmission, so AppsFlyer no longer receives the raw address.

This is handled entirely inside the integration — no code change is required in your app. But if
anything downstream of AppsFlyer keys on the plaintext `user_emails` field (audience matching, partner
integrations, exports), it will now receive a SHA-256 hash instead and may need reconfiguring.
