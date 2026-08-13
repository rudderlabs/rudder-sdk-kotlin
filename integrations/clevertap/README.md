# CleverTap Integration

The CleverTap integration sends RudderStack Android Kotlin SDK events to the CleverTap Android SDK in device mode.

## Requirements

- Android SDK version 21 or higher
- A CleverTap destination configured in the RudderStack dashboard with an account ID and account token
- Host app permission `android.permission.INTERNET` (required by CleverTap)
- Host app permission `android.permission.ACCESS_NETWORK_STATE` (recommended by CleverTap)
- Host app permission `com.google.android.gms.permission.AD_ID` when targeting Android 13+ and using Google Advertising ID
- Host app dependency `com.android.installreferrer:installreferrer` for install-referrer attribution; that dependency supplies its own manifest permission

## Supported Native CleverTap Version

This integration supports CleverTap Android SDK versions in the range:

```text
[7.3.1, 7.7.0)
```

CleverTap 7.7.0 adds `androidx.lifecycle:lifecycle-process:2.9.4`. The `androidx.lifecycle` atomic
group then raises `lifecycle-viewmodel-savedstate` to 2.9.4, which requires `kotlinx-serialization`
1.7.3 and therefore Kotlin 2.0. This SDK builds with Kotlin 1.9.0, so the range stops below 7.7.0.

## Installation

Add the RudderStack Android Kotlin SDK and CleverTap integration to your project:

```kotlin
dependencies {
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
    implementation("com.rudderstack.integration.kotlin:clevertap:<latest_version>")
}
```

## Usage

Initialize the RudderStack SDK and add the CleverTap integration:

```kotlin
import android.app.Application
import com.rudderstack.integration.kotlin.clevertap.CleverTapIntegration
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration

class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()

        analytics = Analytics(
            configuration = Configuration(
                writeKey = "<WRITE_KEY>",
                application = this,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )

        analytics.add(CleverTapIntegration())
    }
}
```

The integration initializes CleverTap from the dashboard fields `accountId`, `accountToken`, and optional `region`. If `region` is blank or `none`, the default CleverTap credential setup is used.

## Supported RudderStack Events

### Identify

`identify` maps RudderStack traits to CleverTap profile fields and calls `CleverTapAPI.onUserLogin(profile)`.

| RudderStack trait | CleverTap profile field |
|---|---|
| `id` | `Identity` |
| `name` | `Name` |
| `email` | `Email` |
| `phone` | `Phone` |
| `gender` (`M`, `MALE`, `F`, `FEMALE`) | `Gender` (`M` or `F`) |
| `birthday` (`yyyy-MM-dd`) | `DOB` |

Nested `address` and `company` traits are flattened to match the legacy Java integration. Nested `id` becomes `companyId`, nested `name` becomes `companyName`, and other nested keys keep their names.

### Track

`track` calls `CleverTapAPI.pushEvent(eventName, properties)` for custom events. If the event has no properties, the integration calls `pushEvent(eventName)`.

`Order Completed` is mapped to CleverTap's charged event API:

- `revenue` → `Amount`
- `order_id` → `Charged ID`
- `products[].product_id` → `items[].id`
- Other root and product properties are forwarded unchanged.

### Screen

`screen` sends a custom CleverTap event named `Screen Viewed: <screenName>` and forwards screen properties when present.

### Lifecycle and Push Handling

The integration observes Android activity lifecycle callbacks through the RudderStack Android SDK after the destination is created:

- `onActivityCreated` calls `CleverTapAPI.setAppForeground(true)`, forwards notification click extras, and forwards deep links from the activity intent.
- `onActivityResumed` calls `CleverTapAPI.onActivityResumed(activity)`.
- `onActivityPaused` calls `CleverTapAPI.onActivityPaused()`.

Because destination creation happens after source config is fetched, early activity callbacks may not be replayed. If your app needs to forward a notification click or deep link before the callback is observed, keep a reference to the integration instance and call:

```kotlin
val cleverTapIntegration = CleverTapIntegration()
analytics.add(cleverTapIntegration)

cleverTapIntegration.pushNotificationClickedEvent(intent.extras)
cleverTapIntegration.pushDeepLink(intent.data)
cleverTapIntegration.setAppForeground(true)
```

For Firebase Cloud Messaging push delivery, configure the host app according to CleverTap's Android push documentation. A typical setup includes a messaging service declaration such as:

```xml
<service
    android:name="com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

If you initialize CleverTap directly from manifest metadata in addition to RudderStack dashboard configuration, add the CleverTap account metadata in the host application manifest. This integration normally supplies credentials from the RudderStack dashboard, so manifest credentials are optional.

## Notes

- `reset` and `flush` are not overridden because the legacy Java CleverTap integration did not implement non-trivial behavior for them.
- The CleverTap Android SDK publishes its own consumer ProGuard rules; no additional integration-specific keep rules are required.
