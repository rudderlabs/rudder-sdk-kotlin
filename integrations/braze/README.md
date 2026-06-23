# Braze Integration

The Braze integration allows you to send your event data from RudderStack to Braze for customer engagement.

## Requirements

- Android SDK version 25 or higher

## Supported Native Braze Version

This integration supports Braze Android SDK version:

```
35.0.0
```

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

## Recommended ecommerce events

When the **`useRecommendedEcommerceEvents`** flag is enabled on the Braze destination, supported RudderStack
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
