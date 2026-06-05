# Value Conversion: `JsonObject` → destination-SDK types

Every device-mode integration has to bridge between two value models:

- **RudderStack events** carry `properties`, `traits`, and `context` as `kotlinx.serialization.json.JsonObject`.
- **Destination SDKs** accept very different shapes: `Map<String, Any>`, `android.os.Bundle`, `org.json.JSONObject`, typed primitives, or their own typed parameter objects.

There is **no single canonical helper** that fits every integration. The existing integrations use five distinct strategies, and the choice is driven by what the destination SDK accepts. Pick the strategy first, then copy the matching helpers.

## Pick a strategy

Look at the destination SDK's main entry point (`track`-equivalent, custom-event call, `setUserAttributes`, etc.) and check what type it accepts:

| Destination SDK accepts… | Strategy | Reference integration |
|---|---|---|
| `Map<String, Any>` / `Map<String, Object>` | **Map-based** (`extractValue` + `toAnyMap`) | `integrations/appsflyer` |
| `android.os.Bundle` | **Bundle-based** (typed `putString`/`putInt`/… per key) | `integrations/firebase` |
| `org.json.JSONObject` | **JSONObject-based** (`JsonObject.toJSONObject()` via `toString()` round-trip) | `integrations/braze` |
| Typed primitives one-at-a-time (`addCallbackParameter(String, String)` etc.) | **Typed per-key** (per-type `getStringOrNull` / `getIntOrNull` / …) | `integrations/adjust` |
| A `@Serializable`-able typed payload (config, traits DTO) | **Decode to data class** (`LenientJson.decodeFromJsonElement<T>()`) | `integrations/adjust`, `integrations/braze` |

The strategies are not mutually exclusive — a single integration usually combines two. For example, `adjust` uses **typed per-key** for event properties and **decode to data class** for destination config; `braze` uses **JSONObject-based** for custom traits and **decode to data class** for the typed-trait subset.

### When none of the 5 strategies fit

This list reflects **the patterns that exist in the codebase today**, not an exhaustive taxonomy of all possible destination-SDK shapes. If the destination SDK accepts something genuinely different (a builder DSL with no value type, a `Flow`/`Channel`, a protobuf message, a domain-specific event class, an SDK whose API accepts `JsonElement` natively, etc.), do **not** force-fit it into typed per-key or any other existing strategy just because that's what's documented.

Instead:

1. **Surface the mismatch explicitly** to the user (use AskUserQuestion) with: the destination SDK's actual call shape, why none of the 5 strategies map cleanly, and the new strategy you propose (helper signatures + a single-method worked example).
2. **Get approval before implementing.** Picking a value-conversion strategy is a load-bearing decision — silently inventing one is the same class of mistake as silently mapping `setFragmentActivity` to auto-detection (see "Behavior Divergence" in Step 1).
3. **After it's implemented and reviewed, write the new strategy back into this file** as Strategy 6 (etc.), with: the row in the strategy table, the worked helper code, and a pointer to the reference integration. The next integration with a similar destination shape should find it documented here.

The 5 existing strategies are the ones that have proven themselves across the current integrations. A 6th is welcome — it just needs explicit design, explicit approval, and a doc update so it stops being one-off.

## Strategy 1: Map-based (`Map<String, Any>`)

Used when the destination SDK accepts a plain Kotlin/Java map. Two helpers, both `internal`, dropped into the integration's `Utils.kt`:

```kotlin
private fun extractValue(element: JsonElement?): Any? = when (element) {
    JsonNull, null -> null

    is JsonPrimitive -> when {
        element.isString -> element.content
        element.booleanOrNull != null -> element.boolean
        element.intOrNull != null -> element.int
        element.longOrNull != null -> element.long
        element.doubleOrNull != null -> element.double
        else -> element.content
    }

    is JsonObject -> element.toAnyMap()

    is JsonArray -> element.mapNotNull { extractValue(it) }
}

internal fun JsonObject?.toAnyMap(): Map<String, Any> {
    if (this == null) return emptyMap()
    val map = mutableMapOf<String, Any>()
    this.forEach { (key, element) ->
        extractValue(element)?.let { map[key] = it }
    }
    return map
}
```

Single-key typed lookup (used by appsflyer for selective property mapping):

```kotlin
private fun JsonObject?.getTypedValue(key: String): Any? =
    this?.get(key)?.let { extractValue(it) }
```

Notes:
- **Type-check order matters.** `intOrNull` must come before `longOrNull`, which must come before `doubleOrNull`, otherwise every integer becomes a double.
- **`mapNotNull` in the array branch** drops nulls so the result is safe for SDKs that reject null elements. Use `map { extractValue(it) }` instead if your destination SDK accepts nulls and array index matters.
- Existing integrations use inconsistent names (`toMutableMap`/`toRawMap` in appsflyer). Prefer `toAnyMap()` for new integrations.

## Strategy 2: Bundle-based

Used when the destination SDK takes an `android.os.Bundle` (Firebase). Do **not** go via a `Map` and convert — that loses type fidelity. Use the typed accessors already exported by the Kotlin SDK's android utilities:

```kotlin
import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.android.utils.getDouble
import com.rudderstack.sdk.kotlin.android.utils.getInt
import com.rudderstack.sdk.kotlin.android.utils.getLong
import com.rudderstack.sdk.kotlin.android.utils.isBoolean
import com.rudderstack.sdk.kotlin.android.utils.isDouble
import com.rudderstack.sdk.kotlin.android.utils.isInt
import com.rudderstack.sdk.kotlin.android.utils.isLong
import com.rudderstack.sdk.kotlin.android.utils.isString

private const val MAX_PROPERTY_VALUE_LENGTH = 100

private fun addPropertyToBundle(params: Bundle, firebaseKey: String, key: String, properties: JsonObject, logger: Logger) {
    when {
        properties.isString(key) -> {
            val value = getString(value = properties[key], maxLength = MAX_PROPERTY_VALUE_LENGTH, logger = logger)
            params.putString(firebaseKey, value)
        }
        properties.isInt(key)     -> params.putInt(firebaseKey, properties.getInt(key) ?: 0)
        properties.isLong(key)    -> params.putLong(firebaseKey, properties.getLong(key) ?: 0)
        properties.isDouble(key)  -> params.putDouble(firebaseKey, properties.getDouble(key) ?: 0.0)
        properties.isBoolean(key) -> params.putBoolean(firebaseKey, properties.getBoolean(key) ?: false)
        else -> properties[key]?.toString()?.take(MAX_PROPERTY_VALUE_LENGTH)?.let {
            params.putString(firebaseKey, it)
        }
    }
}

// Integration-local getString helper — not from the SDK, defined per-integration
internal fun getString(value: JsonElement?, maxLength: Int, logger: Logger): String {
    val stringValue = when (value) {
        is JsonPrimitive -> value.content
        is JsonArray, is JsonObject -> try {
            Json.encodeToString(value)
        } catch (e: Exception) {
            logger.error("Error converting JsonElement to String.", e)
            value.toString()
        }
        else -> value.toString()
    }
    return stringValue.take(maxLength)
}
```

See `integrations/firebase/src/main/.../Utils.kt` (`attachAllCustomProperties` and `addPropertyToBundle`) for the full pattern including reserved-keyword filtering and key sanitization (`formatFirebaseKey`). Note: `getString` is defined locally in the integration, not imported from the SDK.

## Strategy 3: JSONObject-based

Used when the destination SDK takes `org.json.JSONObject` (Braze). The round-trip through `toString()` is acceptable because `kotlinx.serialization.json.JsonObject.toString()` produces standards-compliant JSON that `org.json.JSONObject(String)` parses correctly:

```kotlin
internal fun JsonObject.toJSONObject(): JSONObject {
    return JSONObject(this.toString())
}
```

Don't try to walk the tree manually — `kotlinx.serialization` and `org.json` have different concepts of array, primitive, and null, and rolling your own walker re-introduces subtle bugs the round-trip avoids.

## Strategy 4: Typed per-key

Used when the destination SDK exposes typed primitive setters one-at-a-time (Adjust's `addCallbackParameter(String, String)`, `setRevenue(Double, String)`, etc.). Define per-type getters that return `null` on missing/wrong-type rather than throwing at the call site:

```kotlin
internal fun JsonObject.getStringOrNull(key: String, logger: Logger): String? = runCatching {
    when (val value = this[key]) {
        is JsonPrimitive -> value.content
        is JsonObject -> Json.encodeToString(value)
        is JsonArray -> value.toString()
        null -> throw NullPointerException()
        else -> throw UnsupportedOperationException()
    }
}.getOrElse {
    logger.debug("Failed to parse $key as String"); null
}

internal fun JsonObject.getIntOrNull(key: String, logger: Logger): Int? = runCatching {
    when (val value = this[key]) {
        is JsonPrimitive -> when {
            value.intOrNull != null -> value.int
            value.longOrNull != null -> value.long.toInt()
            value.doubleOrNull != null -> value.double.toInt()
            value.isString -> value.content.toInt()
            else -> throw IllegalArgumentException()
        }
        null -> throw NullPointerException()
        else -> throw IllegalArgumentException()
    }
}.getOrElse {
    logger.debug("Failed to parse $key as Int"); null
}
```

`getLongOrNull` and `getDoubleOrNull` follow the same shape. See `integrations/adjust/src/main/.../Utils.kt` for the full set.

Key differences vs. Strategy 1's `extractValue`:
- Each call has a target type, so cross-type coercion is allowed (string `"42"` → `Int 42`).
- Failures are debug-logged with the key name, so per-property issues are diagnosable in production.
- The call site decides what to do with `null` (skip the field, send a default, abort the event).

## Strategy 5: Decode to a data class

Used for **structured payloads** with a known schema: destination config, typed traits, attribution events. Define a `@Serializable` data class that mirrors the schema and decode the relevant subtree:

```kotlin
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parseConfig(logger: Logger): T? =
    this.takeIf { it.isNotEmpty() }?.let {
        LenientJson.decodeFromJsonElement<T>(this)
    } ?: run {
        logger.debug("Configuration is empty"); null
    }
```

Use `LenientJson` (from `com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson`), not the default `Json` — it's configured with `ignoreUnknownKeys = true` and other forgiving settings appropriate for destination payloads where new server-side fields appear over time.

Reference uses:
- `integrations/adjust/Utils.kt::parseConfig<T>` — destination config decoded into `AdjustDestinationConfig`
- `integrations/braze/Utils.kt::parse<T>` — destination config decoded into `BrazeDestinationConfig`
- `integrations/braze/Utils.kt::toIdentifyTraits` — `IdentifyEvent` → `IdentifyTraits` data class for typed-trait handling

This strategy composes with the others: braze uses it for the *known* trait fields and then routes the *remaining* keys through the JSONObject-based path for custom traits.

## The `IdentifyEvent.traits` extension

Most integrations (appsflyer, firebase, braze) define this identical three-line extension to reach into `context["traits"]`:

```kotlin
internal val IdentifyEvent.traits: JsonObject?
    get() = this.context["traits"]?.jsonObject
```

Define it `internal` per-integration. Don't try to promote it to a shared module — it's three lines, and the cost of a shared dependency is higher than the duplication. Adjust takes a different shape (`AnalyticsContext.toJsonObject("traits")` returning `emptyJsonObject` instead of `null`), which is also fine — match whichever default behavior fits how the integration uses the result.

## Why duplicate instead of reusing a core helper?

`core/src/main/.../javacompat/JsonInteropHelper.kt` already contains an `extractValue` / `toRawMap` pair, but it is `internal` and intentionally scoped to the Java-interop layer. Integrations cannot depend on it, so each integration redefines the primitives locally. **This is expected.** Do not try to make the helper public or build an integration-shared utility module unless that's an explicit task — keep the value-conversion code local to the integration so each integration can tune the type-coercion rules to what its destination SDK actually accepts.

## Checklist when implementing `identify` / `track`

1. **Pick the strategy.** Read the destination SDK's main entry point. Pick the row from the table at the top.
2. **Copy the matching helpers verbatim** from the reference integration named in the table. Don't reimplement from scratch — the type-check order and null-handling are easy to get wrong.
3. **Add `IdentifyEvent.traits`** (or `AnalyticsContext.toJsonObject("traits")` for Adjust-style) if you implement `identify()`.
4. **Decide null handling.** All the helpers above drop nulls by default. If your destination SDK has positional semantics (e.g. parallel arrays indexed by product position), switch to a null-preserving variant and document why at the call site.
5. **Decide nested-value handling.** Strategy 1 recurses, Strategy 2 stringifies, Strategy 3 round-trips, Strategy 4 stringifies via `Json.encodeToString`. Confirm which one your destination SDK expects before writing the `Map<JsonObject>` / nested case.
