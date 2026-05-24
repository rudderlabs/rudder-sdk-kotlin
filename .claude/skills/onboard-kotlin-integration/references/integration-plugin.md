# IntegrationPlugin Abstract Class Reference

Reference the signatures below when generating the main integration class (Step 3) and when deciding which methods to override.

```kotlin
abstract class IntegrationPlugin : EventPlugin {
    // --- Abstract (must implement) ---
    abstract val key: String                                    // Destination key from source config
    abstract fun create(destinationConfig: JsonObject)          // Initialize destination instance
    abstract fun getDestinationInstance(): Any?                 // Return the created destination instance

    // --- Optional overrides ---
    open fun update(destinationConfig: JsonObject) {}           // Called when config updates dynamically
    open fun flush() {}                                         // Handle Analytics.flush()
    open fun reset() {}                                         // Handle Analytics.reset()
    override fun teardown() {}                                  // Cleanup when plugin is removed

    // --- Inherited from EventPlugin (override as needed) ---
    // fun track(payload: TrackEvent) {}
    // fun screen(payload: ScreenEvent) {}
    // fun group(payload: GroupEvent) {}
    // fun identify(payload: IdentifyEvent) {}
    // fun alias(payload: AliasEvent) {}

    // --- Inherited from Plugin ---
    // val pluginType: Plugin.PluginType   -> Always .Terminal for integrations
    // var analytics: Analytics            -> Analytics instance reference
    // setup(analytics: Analytics)         -> FINAL OVERRIDE in IntegrationPlugin — cannot
    //                                        be overridden. Runs at plugin-attach time
    //                                        (before SourceConfig fetch); do all init work
    //                                        in create() instead, which fires after the
    //                                        fetch with the destination config in hand.

    // --- Integration-specific ---
    fun onDestinationReady(callback: (Any?, DestinationResult) -> Unit)
}

// Marker interface for standard integrations
interface StandardIntegration
```
