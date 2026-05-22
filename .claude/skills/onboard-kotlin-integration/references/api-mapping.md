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
| Activity lifecycle callbacks | `ActivityLifecycleObserver` methods | Activity lifecycle handling |

## Key Differences

- Java uses a single `dump()` method with `MessageType` checking; Kotlin has separate methods per event type.
- `create()` and `update()` receive `JsonObject` (not `Map<String, Any>`). Access config via `jsonObject["key"]?.jsonPrimitive?.content` etc.
- `update()` and `getDestinationInstance()` are new in Kotlin with no Java equivalent.
