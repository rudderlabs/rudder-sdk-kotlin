# Readme.MD

![https://img.shields.io/github/release/rudderlabs/rudder-sdk-kotlin.svg](https://img.shields.io/github/release/rudderlabs/rudder-sdk-kotlin.svg)

![https://img.shields.io/badge/%3C%2F%3E-Kotlin-%230095D5.svg](https://img.shields.io/badge/%3C%2F%3E-Kotlin-%230095D5.svg)

[https://sonarcloud.io/api/project_badges/measure?project=rudderlabs_rudder-sdk-kotlin&metric=alert_status](https://sonarcloud.io/api/project_badges/measure?project=rudderlabs_rudder-sdk-kotlin&metric=alert_status)

[https://sonarcloud.io/api/project_badges/measure?project=rudderlabs_rudder-sdk-kotlin&metric=sqale_rating](https://sonarcloud.io/api/project_badges/measure?project=rudderlabs_rudder-sdk-kotlin&metric=sqale_rating)

---

# RudderStack Kotlin SDK

The Kotlin SDK enables you to track customer event data from your Android or Kotlin Multiplatform applications and send it to your configured destinations via RudderStack.

---

## Table of Contents

- [**Installing the Kotlin SDK**](#installing-the-kotlin-sdk)
- [**Initializing the SDK**](#initializing-the-sdk)
- [**Identifying your users**](#identifying-users)
- [**Tracking user actions**](#tracking-user-actions)
- [**Contribute**](https://chatgpt.com/c/6761552e-6118-8001-bd2a-cee1d902a13c#contribute)
- [**Contact us**](https://chatgpt.com/c/6761552e-6118-8001-bd2a-cee1d902a13c#contact-us)

---

## Installing the Kotlin Android SDK

Add the SDK to your Android project using Gradle:

```kotlin
dependencies {
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
}

```

Replace `<latest_version>` with the version number you want to use. You can find the latest release [**here**](https://github.com/rudderlabs/rudder-sdk-kotlin/releases).

## Installing the Kotlin Core SDK

Add the SDK to your Kotlin JVM project using Gradle:

```kotlin
dependencies {
    implementation("com.rudderstack.sdk.kotlin:core:<latest_version>")
}

```

Replace `<latest_version>` with the version number you want to use. You can find the latest release [**here**](https://github.com/rudderlabs/rudder-sdk-kotlin/releases).

---

## Initializing the SDK

To initialize the Android RudderStack SDK, add the Analytics initialisation snippet to your application’s entry point (e.g., in `onCreate` method):

```kotlin
import android.app.Application
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration

class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()
        initializeAnalytics(this)
    }

    private fun initializeAnalytics(application: Application) {
        analytics = Analytics(
            configuration = Configuration(
                writeKey = "<WRITE_KEY>",
                application = application,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )
    }
}
```

Replace:

- `<WRITE_KEY>`: Your project’s write key.
- `<DATA_PLANE_URL>`: The URL of your RudderStack data plane.

---

## Identifying users

The `identify` API lets you recognize a user and associate them with their traits:

```kotlin
analytics.identify(
	userId = "1hKOmRA4el9Zt1WSfVJIVo4GRlm",
	traits = buildJsonObject {
			put("name", "Alex Keener")
			put("email", "alex@example.com")
		}
)
```

---

## Tracking user actions

The `track` API lets you capture user events:

```kotlin
analytics.track(
    event = "Order Completed",
    properties = buildJsonObject {
		    put("revenue", 30)
        put("currency", "USD")
    }
)
```

---

## Contribute

We welcome contributions! To get started, check out our [**Contribution Guidelines**](https://github.com/rudderlabs/rudder-sdk-kotlin/blob/main/CONTRIBUTING.md).

---

## Contact us

For more information:

- Email us at [docs@rudderstack.com](mailto:docs@rudderstack.com)
- Join our [**Community Slack**](https://rudderstack.com/join-rudderstack-slack-community)

---

## Follow Us

- [RudderStack Blog](https://rudderstack.com/blog/)
- [Slack](https://rudderstack.com/join-rudderstack-slack-community)
- [Twitter](https://twitter.com/rudderstack)
- [YouTube](https://www.youtube.com/channel/UCgV-B77bV_-LOmKYHw8jvBw)

---

## :clap: Our Supporters

[https://reporoster.com/stars/rudderlabs/rudder-sdk-kotlin](https://reporoster.com/stars/rudderlabs/rudder-sdk-kotlin)

[https://reporoster.com/forks/rudderlabs/rudder-sdk-kotlin](https://reporoster.com/forks/rudderlabs/rudder-sdk-kotlin)

---

This document is now focused on the **Kotlin SDK** while retaining the structure and general style of the original. Let me know if you need further refinements!
