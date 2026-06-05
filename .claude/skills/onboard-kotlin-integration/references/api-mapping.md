# API Mapping: Java Android v1 ↔ Kotlin SDK

Use this mapping when converting a Java integration's logic to the Kotlin equivalent.

| Java Android v1 Method | Kotlin SDK Method | Description |
|-------------------------|-------------------|-------------|
| `RudderIntegration()` constructor / `getFactory()` | `create(destinationConfig: JsonObject)` | Initialize the integration with configuration |
| N/A | `update(destinationConfig: JsonObject)` | **New in Kotlin**: Update config dynamically without re-init |
| N/A | `getDestinationInstance(): Any?` | **New in Kotlin**: Return the destination SDK instance |
| `reset()` | `reset()` | Reset user state / logout |
| `flush()` | `flush()` | Flush pending events |
| `dump(RudderMessage)` with type check | `identify(payload: IdentifyEvent)` | Handle identify events |
| `dump(RudderMessage)` with type check | `track(payload: TrackEvent)` | Handle track events |
| `dump(RudderMessage)` with type check | `screen(payload: ScreenEvent)` | Handle screen events |
| `dump(RudderMessage)` with type check | `group(payload: GroupEvent)` | Handle group events |
| `dump(RudderMessage)` with type check | `alias(payload: AliasEvent)` | Handle alias events |
| Activity lifecycle callbacks | `ActivityLifecycleObserver` methods | Activity lifecycle handling (see timing note below) |

## Key Differences

- Java uses a single `dump()` method with `MessageType` checking; Kotlin has separate methods per event type.
- `create()` and `update()` receive `JsonObject` (not `Map<String, Any>`). For destination config, decode to a `@Serializable` data class via `parseConfig<T>()` (see SKILL.md Step 4a). For event properties/traits, use the value-conversion strategy from `value-conversion.md`.
- `update()` and `getDestinationInstance()` are new in Kotlin with no Java equivalent.
- **Activity lifecycle timing divergence**: Java v1's `Application.ActivityLifecycleCallbacks` are typically registered at host-app boot (in `Application.onCreate`), so they observe every activity event from the first `onCreate`. The Kotlin SDK's `addLifecycleObserver(this)` is called from the integration's `create()`, which only fires **after** SourceConfig is fetched from the dashboard. By then the host activity may already be RESUMED, and earlier lifecycle events are **not** replayed. If the integration needs an Activity reference for any call site (UI presentation, in-app messages, etc.), prefer preserving the Java-style explicit setter API rather than relying on auto-observe alone.
