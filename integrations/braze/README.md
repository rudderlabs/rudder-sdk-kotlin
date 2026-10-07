# Braze Integration

The Braze integration allows you to send your event data from RudderStack to Braze for customer engagement.

## Requirements

- Android SDK version 25 or higher

## Supported Native Braze Version

This integration supports Braze Android SDK versions:

```
[35.0.0, 45.0.0)
```

It resolves to `35.0.0` by default. If your app declares a higher Braze version, Gradle uses yours. The upper
bound is not enforced: an app on Braze 45 or later still builds, but that combination is untested. An app on a
version below 35.0.0 is upgraded to 35.0.0.

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
        analytics.add(BrazeIntegration())
    }
}
```

## Lite mode: bring your own Braze instance

By default, the integration initialises Braze for you using the App Identifier Key and endpoint from the RudderStack
dashboard. If you need Braze options the dashboard does not expose (push notifications, in-app message settings,
SDK authentication, and so on), initialise Braze yourself and pass the instance to the integration:

```kotlin
// 1. Configure and initialise Braze with any options you need.
Braze.configure(
    this,
    BrazeConfig.Builder()
        .setApiKey("<BRAZE_API_KEY>")
        .setCustomEndpoint("<BRAZE_ENDPOINT>")
        .setHandlePushDeepLinksAutomatically(true)
        .build()
)
registerActivityLifecycleCallbacks(BrazeActivityLifecycleCallbackListener())

// 2. Pass your instance to RudderStack.
analytics.add(BrazeIntegration(Braze.getInstance(this)))
```

In lite mode:

- The integration **never initialises Braze** and never opens or closes Braze sessions. You own the Braze
  configuration and lifecycle.
- The integration still **maps and forwards events** (track, identify, flush) exactly as in standard mode.
- The Braze destination must still exist and be enabled in the dashboard. Its event-mapping settings (connection
  mode, de-duplication, recommended ecommerce events) still apply; its App Identifier Key and endpoint are ignored.
- You choose the Braze SDK version. The integration requires Braze **35.0.0 or higher** and has been tested
  against **35.0.0 and 44.0.0**; if your app declares a newer Braze version, Gradle uses yours.

## Recommended ecommerce events

When the **`useEcommerceRecommendedEvents`** flag is enabled on the Braze destination, supported RudderStack
ecommerce track events are mapped to [Braze recommended events](https://www.braze.com/docs/user_guide/data/activation/events/recommended_events)
(`ecommerce.*`) and sent via `logCustomEvent`. The flag defaults to off; when off, behaviour is unchanged.

| RudderStack event | Braze event | Action |
|---|---|---|
| Product Viewed | `ecommerce.product_viewed` | — |
| Product Added | `ecommerce.cart_updated` | `add` |
| Product Removed | `ecommerce.cart_updated` | `remove` |
| Checkout Started | `ecommerce.checkout_started` | — |
| Order Completed | `ecommerce.order_placed` | — |
| Order Refunded | `ecommerce.order_refunded` | — |
| Order Cancelled | `ecommerce.order_cancelled` | — |

Notes:

- `Cart Viewed` and `Cart Updated` are not mapped — they continue to flow through the generic custom-event path.
- When the flag is enabled, `Order Completed` emits a single `ecommerce.order_placed` event instead of one purchase
  per product via the legacy `logPurchase` path.
- The `source` field is always set to `android`.
- Events are never dropped on incomplete data: missing Braze-required fields are logged as a warning and the event
  is still sent (`0` and `false` are treated as valid values).
- Field values are coerced to the type Braze expects where possible (e.g. a numeric string `"29.99"` → `29.99`,
  a number → string). When a value cannot be coerced (e.g. `quantity` as `2.5`), a warning is logged and the
  value is sent as-is.
- Properties not covered by the mapping are forwarded under `metadata` (event level) and `products[].metadata`
  (per product).
