---
name: onboard-kotlin-integration
description: Generates a Kotlin integration for the rudder-sdk-kotlin repo in a step-by-step manner by referencing the corresponding Java integration.
when_to_use: Use when the user wants to create a new Kotlin device-mode integration from an existing Java Android v1 integration. Trigger phrases - "onboard kotlin integration", "generate kotlin integration", "convert java integration to kotlin", "new kotlin integration for <name>".
argument-hint: [integration-name] [closest-example] [--auto]
allowed-tools: Bash, Read, Write, Edit, Glob, Grep, AskUserQuestion
---

You are an expert Android/Kotlin developer that creates Rudder integrations by converting Java integrations to Kotlin equivalents.

Your goal is to generate a new Kotlin integration in the `rudder-sdk-kotlin` repo by analyzing the corresponding Java integration and creating the Kotlin equivalent in a step-by-step manner.

## Input

Parse the following user input for:
- **integration_name** (required): The name of the integration to generate (e.g., 'appsflyer', 'clevertap')
- **closest_example** (optional): Name of an existing Kotlin integration most similar to the one being generated (e.g., 'firebase', 'braze')
- **auto_mode** (optional flag): If `--auto` is present, run in auto mode (see Execution Mode below)

User input: $ARGUMENTS

If integration_name is not provided, use AskUserQuestion to ask the user for it before proceeding.

## Execution Mode

The skill runs in one of two modes, controlled by the `--auto` flag:

### Interactive mode (default)
The agent pauses for user approval at **three checkpoints** — no more:
1. **After Step 1** (analysis + behavior divergences) — this is load-bearing; the entire run depends on getting the work list and divergence decisions right.
2. **After Step 8** (all implementation complete — module structure, integration class, event methods, utils) — the user reviews the full generated codebase before build/test.
3. **After Step 9** (build/test results) — only if there are failures the agent cannot auto-fix.

Between checkpoints, the agent works continuously without asking for approval. The user can always interrupt with feedback in the chat — they don't need a gate to do so.

### Auto mode (`--auto`)
The agent runs end-to-end with **no** `AskUserQuestion` calls except:
- **Behavior Divergences** in Step 1 that deviate from "preserve Java public API" — these require explicit approval per the skill's design rules.
- **Build/test failures** in Step 9 that the agent cannot auto-fix after two attempts.
- **Value-conversion strategy mismatch** — if none of the 5 documented strategies fit the destination SDK (see `references/value-conversion.md`), the agent must propose a new one before implementing.

Everything else proceeds without pausing.

## Reference Material (read on demand)

Load each reference file with `Read` only when the corresponding step needs it (and skip if already in context). Do not paste their contents into responses — refer by filename.

- **`references/api-mapping.md`** — Java v1 ↔ Kotlin SDK method mapping. Read before Step 1; consult during Steps 5–7.
- **`references/integration-plugin.md`** — `IntegrationPlugin` and `StandardIntegration` signatures. Read before Step 3.
- **`references/test-scaffold.md`** — Drop-in test structure (directory layout, `build.gradle.kts` test block, mockk patterns, POM verification). Read before Step 9b.
- **`references/value-conversion.md`** — `JsonObject` → destination-SDK type conversion across 5 strategies. Read before Step 5 and pick a strategy *before* writing helpers. The 5 strategies are not exhaustive — if none fit, propose a new one via AskUserQuestion, get approval, implement, then write it back into this reference.

## Locating the Java Integration Repo

The skill runs from inside `rudder-sdk-kotlin`. Existing Kotlin integrations live under `integrations/`. The Java integration repo must be located — use `AskUserQuestion`:

- Question: "Where is the Java integration repo `rudder-integration-<integration_name>-android` located?"
- Options:
  - **Provide local path** — "I have it cloned locally, I'll share the path"
  - **Clone from GitHub** — "Clone `https://github.com/rudderlabs/rudder-integration-<integration_name>-android` into a temp directory"
  - **Skip** — "I'll paste relevant Java snippets manually as we go"

If the user picks **Provide local path**, ask them for the absolute path (free-form input via the "Other" choice or a follow-up). Verify the path exists with `Bash`.

If the user picks **Clone from GitHub**, run:
```bash
git clone --depth=1 https://github.com/rudderlabs/rudder-integration-<integration_name>-android /tmp/rudder-integration-<integration_name>-android
```
Use the cloned directory as the Java integration source. Mention to the user that this is a throwaway clone they can delete after the skill completes.

If the user picks **Skip**, proceed without a Java repo reference and rely on snippets the user pastes inline during each step.

Store the resolved Java repo path and reuse it throughout subsequent steps.

## Detailed Instructions

### Step 1: Analyze Java Integration Structure

Locate the Java integration repo using the instructions above. If `closest_example` was provided, also read the existing Kotlin integration at `integrations/<closest_example>/` in the current repo.

Analyze the Java integration:
- Examine the main integration factory class
- Identify core business logic methods and their purposes
- Note any utility classes or configuration classes
- Document the integration's key features and event mappings
- **Enumerate custom public APIs**: list every public method beyond the standard v1 `RudderIntegration` / `Factory` surface (`dump`, `reset`, `flush`, `getUnderlyingInstance`, `create`, `key`). Methods on a custom `Factory` subinterface, public statics, or any other host-facing API count here — they have **no automatic equivalent** in the Kotlin SDK and need an explicit design decision.
- **Verify the destination SDK's Kotlin API surface**: the Java integration's call sites (e.g. `Foo.INSTANCE.configure(...)`, `Foo.getInstance().bar()`) reflect how *Java* sees the destination SDK and frequently do **not** compile from Kotlin. Before writing any Kotlin call, open the destination SDK's actual class files (its AAR/JAR, its public sources, or its docs) and record the real Kotlin call shape for every method the integration uses. A Kotlin `object` is called as `Foo.bar()` (no `INSTANCE`); a class with `companion object` is `Foo.bar()` (no `Companion` unless `@JvmStatic` is absent); a regular class needs an instance; suspend / extension / nullable-receiver signatures all change the call site. Do not infer the Kotlin surface from the Java integration's call sites — read the destination SDK directly.
- **If closest_example provided**: Also reference the existing Kotlin integration for implementation patterns, otherwise refer any other kotlin integration.

Collect findings internally covering these sections (list only methods with non-trivial Java logic — skip unimplemented ones):

- **Methods to Implement**: always `create()`, `getDestinationInstance()`, `key`. Then: which event methods have logic (identify, track, screen, group, alias); which lifecycle methods have logic (update only per Step 4b, reset, flush, teardown per cleanup symmetry rule); which activity lifecycle callbacks the Java integration implements. One-line description per method.
- **Custom Public APIs**: every public method/field beyond the standard v1 surface — Java signature, purpose, proposed Kotlin design. "None" if empty.
- **Behavior Divergence**: for each custom API and each place semantics can't be preserved 1:1. Format: **What Java did** → **Kotlin options** → **Recommended**. Default: preserve as instance method. Deviate only with a concrete reason. "None" if empty.
- **Dependencies**: external SDK dependencies.
- **Destination SDK Kotlin API**: actual Kotlin call shape for every method the integration uses. Flag any that differ from the Java call sites.
- **Closest Example** (if provided): patterns to follow, key differences.

**Checkpoint 1 — presenting the analysis:**

Do **not** dump the full analysis as a large markdown block into the conversation — that consumes context for the rest of the run. Instead:

1. Output a **brief text summary** (5–8 lines max): methods to implement, any behavior divergences, dependencies, and the destination SDK entry-point shape.
2. Put the **full structured analysis** in the `preview` field of the "Approve" option in `AskUserQuestion` — the user can inspect it there without it persisting in conversation context.

Use `AskUserQuestion` (interactive mode):
- Question: "<brief summary text>. How would you like to proceed?"
- Options:
  - **Approve** (with `preview` containing the full analysis) — "Analysis looks good, proceed to implementation"
  - **Request changes** — "I have corrections or additional context to share"

In **auto mode**, skip this gate unless there are Behavior Divergences that deviate from "preserve Java public API" — those still require explicit approval. If all divergences recommend preservation, proceed directly.

### Step 2: Create Module Structure
Create the basic module structure in `integrations/<integration_name>/` of the current repo:

**Create directories and basic files**:
- `build.gradle.kts` - Module build configuration
- `src/main/AndroidManifest.xml` - Android manifest
- `.gitignore` - Git ignore file
- `consumer-rules.pro` - ProGuard consumer rules
- `proguard-rules.pro` - ProGuard rules

**Update the config and gradle files**:
- `RudderStackBuildConfig.kt` - add the integration object here. Refer to other existing Kotlin integrations for exact patterns.
- `settings.gradle.kts` - include the integration here. Copy the pattern from other integrations.
- `libs.versions.toml` - add the dependency version here for the destination SDK. Follow existing integration examples.
- `build.gradle.kts` - add that dependency from libs.versions.toml. Use the same structure as other integrations.

**Important**: When creating or updating the above config files, **always refer to existing Kotlin integrations** (closest_example if provided, or firebase/braze/adjust) as they follow identical patterns.

Proceed directly to Step 3 — no approval gate here. Module scaffolding is mechanical.

### Step 3: Generate Main Integration Class
Create the main `<IntegrationName>Integration.kt` class and generate **only** the method stubs.

**Which stubs to generate**: derive the list directly from Step 1's "Methods to Implement" section. That means:
- Always: `create()`, `getDestinationInstance()`, `key` property.
- `update()` only if the destination SDK exposes a re-configure API (see Step 4b — most do not; the base class default is a no-op and overriding it with an empty body is dead code).
- From Step 1's per-method analysis: a stub for every method (event, lifecycle, activity-lifecycle callback) the Java integration implements with non-trivial logic.
- Do **not** add stubs for methods the Java integration leaves empty. Do **not** override `flush()`, `reset()`, `teardown()`, or `update()` just to fill them in — `IntegrationPlugin` already provides empty defaults for these, so an empty override is noise.

**Destination-instance field (non-negotiable)**: Declare a nullable `private var` for the destination SDK instance, default `null`, assigned in `create()` and returned by `getDestinationInstance()`. The Kotlin SDK uses this null-ness to decide between `create()` and `update()` on every config refresh — see `android/src/main/kotlin/com/rudderstack/sdk/kotlin/android/plugins/devicemode/IntegrationPlugin.kt:197` for the gating logic. Returning a non-null literal (e.g., the destination SDK's `object` itself) breaks this and causes `create()` to never fire.

**Thread-safety for mutable fields**: any `private var` that is written on one thread and read on another must be annotated `@Volatile`. The most common case is an activity reference field — lifecycle callbacks write it on the main thread, while event methods (`track`, `identify`) may read it from the SDK's background thread. Mark such fields `@Volatile` at declaration.

Pattern (confirmed in `integrations/braze`, `integrations/adjust`, `integrations/appsflyer`):

```kotlin
class FooIntegration : StandardIntegration, IntegrationPlugin() {
    override val key: String = FOO_KEY

    private var foo: Foo? = null

    override fun create(destinationConfig: JsonObject) {
        foo ?: run {
            // parse config, then assign
            foo = Foo.initialize(...)
        }
    }

    override fun getDestinationInstance(): Any? = foo
}
```

If the destination SDK is a Kotlin `object` (single global instance), the field still exists — typed as `Foo?` — and `create()` assigns it to the object reference after a successful `configure()` call. Every call site in `identify`/`track`/`reset`/lifecycle must use the stored field (`foo?.track(...)`), not the SDK class directly (`Foo.track(...)`), so that calls before `create()` is gated null-safe.

**Reference Java equivalent**: Show the main Java integration class method signatures alongside the Kotlin stubs.
**Generate Kotlin class**: Create the Kotlin equivalent with the proper class structure (`StandardIntegration`, `IntegrationPlugin()`, plus `ActivityLifecycleObserver` if Step 1 flagged any activity callbacks).

Proceed directly to Step 4a — no approval gate on stubs alone. The user will review the full implementation at Checkpoint 2.

### Step 4a: Implement Core Business Logic - Initialization
Convert the Java integration initialization logic to Kotlin.

**Use Step 1's "Destination SDK Kotlin API" section as the source of truth** for the call site you'll write. That section already recorded the entry-point shape (Kotlin `object` / class with `companion object` / instance-required class) and the exact initialization call (`Foo.configure(context, key)` vs. `Foo.getInstance().configure(...)` vs. `Foo.initialize(...).also { ... }`). Do not re-derive the shape here — re-deriving the destination SDK's call shape at code-write time is the classic source of `INSTANCE` / `Companion` build failures.

**Implement against the stored field**: The integration class has a `private var foo: Foo? = null` field (Step 3). `create()` must:
1. Guard with `foo ?: run { ... }` so re-entry on an initialized integration is a no-op.
2. Parse the destination config.
3. Call the destination SDK's init (`Foo.configure(...)` or equivalent) and assign the result to `foo`. For SDKs that don't return an instance (e.g., Kotlin `object` singletons), do the configure call then assign `foo = Foo`.
4. Register any cross-cutting hooks the integration needs (e.g., `(analytics as? AndroidAnalytics)?.addLifecycleObserver(this)` for activity-lifecycle observers).

**Cleanup symmetry rule**: every registration in `create()` — lifecycle observers, SDK event listeners, callbacks — requires a corresponding deregistration in `teardown()`. If `create()` calls `addLifecycleObserver(this)`, then `teardown()` must call `removeLifecycleObserver(this)`, null out the destination instance field, and clean up any other state (e.g., clear cached activity references). Add `teardown()` to Step 7's work list whenever `create()` registers anything — the Java integration may not have had explicit cleanup, but the Kotlin SDK's plugin lifecycle requires it.

Do **not** introduce a separate `isInitialized` flag — the `foo == null` check is the canonical signal and the SDK uses it directly (`IntegrationPlugin.kt:197`).

**Always use the decode to data class strategy** from `references/value-conversion.md` (Strategy 5) for destination config — define a `@Serializable` config class and a `parseConfig<T>` helper in `Utils.kt`. Even for simple configs with one or two fields, a data class is preferred — it's consistent across integrations, type-safe, and trivially extensible when new dashboard fields appear. Do not fall back to manual field-by-field parsing (`getString`, `getBoolean`, etc.) unless the config structure is genuinely dynamic (varying keys, not a fixed schema).

**Reference Java initialization**: Show Java constructor/factory logic alongside the Kotlin equivalent.
**Implement Kotlin equivalent**: Create Kotlin initialization in the `create()` method, using the call site recorded in Step 1 and assigning to the field declared in Step 3.

### Step 4b: Implement Update Logic
`update(destinationConfig: JsonObject)` is called by the Kotlin SDK when the destination's dashboard config changes at runtime, *after* `create()` has already run. The base class default is `open fun update(...) {}` — a no-op — so you should only override it when the destination SDK actually supports updating its configuration in-flight.

Decide between these three cases (and state which one applies in the output):

1. **Destination SDK has a re-configure API** (rare): override `update()` and call that API with the new config values. Do **not** re-run `create()` — the destination instance already exists and the SDK never calls `create()` while the integration's `getDestinationInstance()` returns non-null.
2. **Destination SDK has no re-configure API but the integration caches parsed config fields it consults later** (e.g., feature flags, mode toggles): override `update()` only to refresh those cached fields — the destination SDK instance itself stays put. Braze does this with its `RudderBrazeConfig` cache.
3. **Destination SDK has no re-configure API and the integration doesn't cache anything**: do **not** override `update()` at all. An empty override is dead code.

Skip ahead to Step 5 if case (3) applies. No approval gate here — proceed directly.

### Step 5: Implement Core Business Logic - Identify Method
Convert the Java identify method to Kotlin.

**Before writing this method, read `references/value-conversion.md`** (if not already loaded in context) and pick a strategy by looking at what the destination SDK's user-attribute / identify-equivalent call accepts (`Map<String, Any>`, `Bundle`, `JSONObject`, typed per-key, or a `@Serializable` typed-trait DTO). Copy the matching helpers from the reference integration named in the file's strategy table; do not assume `toAnyMap()` is the default — only 2 of 5 existing integrations use it. **If none of the 5 strategies fit the destination SDK's actual shape**, follow the "When none of the 5 strategies fit" section in the reference: propose a new strategy via AskUserQuestion before implementing, and write it back into the reference once approved.

**Utils extraction rule** (applies to Steps 5–7): write the event method body (`identify()`, `track()`, etc.) in the integration class, but extract any helper functions — attribute processing, trait mapping, value conversion, validation — to `Utils.kt` as `internal` functions. Rule of thumb: if a function doesn't need `this` (no `analytics`, no mutable instance fields like `currentActivity`), it belongs in `Utils.kt`, not in the integration class. The integration class should orchestrate; Utils should transform. This matches the existing integrations — see how braze, adjust, and firebase all keep transformation logic in their respective `Utils.kt`.

Proceed directly to Step 6 — no approval gate.

### Step 6: Implement Core Business Logic - Track Method
Convert the Java track method to Kotlin with event mappings. Use the same value-conversion strategy chosen in Step 5 — do not switch strategies between event methods within one integration.

Proceed directly to Step 7 — no approval gate.

### Step 7: Implement Remaining Event and Lifecycle Methods

Steps 5–6 covered `identify()` and `track()` because every integration has them. This step iterates through **every other method identified in Step 1's analysis** that has Java logic. There is no privileged "main" method here — `screen()`, `reset()`, and `ActivityLifecycleObserver` are equally important; which ones apply depends entirely on what the Java integration implements.

#### Build the work list

From the Step 1 analysis, list every method below that the Java integration implements with non-trivial logic. Skip any that the Java integration leaves empty.

- **Event methods**: `screen()`, `group()`, `alias()`, `flush()`
- **Lifecycle methods**: `reset()`, `teardown()`
- **Activity lifecycle** (`ActivityLifecycleObserver` callbacks): `onActivityCreated`, `onActivityStarted`, `onActivityResumed`, `onActivityPaused`, `onActivityStopped`, `onActivitySaveInstanceState`, `onActivityDestroyed`

#### Iterate

Implement all methods on the work list in a single pass — in the order the Java integration declares them (or alphabetically if you have no Java source). For each method:

1. Use the value-conversion strategy and helpers chosen in Steps 5/6. The same strategy applies uniformly — do not switch strategies between methods within one integration.
2. If there's a behavior divergence not already covered in Step 1's "Behavior Divergence" section, note it in the output but do not gate on it — unless it would deviate from "preserve Java public API", which requires user approval (use AskUserQuestion).

Do **not** ask for approval per method. The user reviews the full implementation at Checkpoint 2 after Step 8.

#### Activity lifecycle: extra setup

If the work list contains any `ActivityLifecycleObserver` callbacks, also confirm during the first sub-step for that group:
- The class declaration adds `ActivityLifecycleObserver` to its `implements` list.
- `create()` registers the integration via `(analytics as? AndroidAnalytics)?.addLifecycleObserver(this)` (see `integrations/braze` and `integrations/adjust` for worked examples).
- The Java integration may have used `Application.ActivityLifecycleCallbacks` directly — confirm the Kotlin port routes through the SDK's lifecycle plumbing instead (it's not a 1:1 mapping; the host registers the SDK once, and the SDK fans out to integrations).

Do **not** add empty overrides or stubs for methods the Java integration doesn't implement — `IntegrationPlugin` provides defaults. `create()`/`update()`/`getDestinationInstance()` are Step 4; `identify()`/`track()` are Steps 5–6.

### Step 8: Finalize Utility Classes and Configuration
By this point, `Utils.kt` should already contain the value-conversion helpers and any transformation functions extracted from Steps 5–7 (per the Utils extraction rule in Step 5). This step handles anything remaining:

- **Verify `Utils.kt` completeness**: review the integration class for any private helper functions that don't access `this`. If any exist, move them to `Utils.kt` as `internal` functions now.
- **Config data class**: should already exist from Step 4a (the `@Serializable` config class + `parseConfig<T>` helper). If it wasn't created yet, create it now.
- **Additional utility classes**: convert any remaining Java utility or configuration classes not covered above. For value-conversion utilities, use the implementations from `references/value-conversion.md` matching the strategy picked in Step 5/6 verbatim — these are duplicated across integrations on purpose (the core helper is `internal`).

**Checkpoint 2 (interactive mode only):** After completing Steps 2–8, present a summary of all generated files and key implementation decisions. Use `AskUserQuestion`:
- Question: "Implementation complete — here's what was generated: [list files + key decisions]. Review before build/test?"
- Options:
  - **Approve** — "Looks good, proceed to build and test"
  - **Request changes** — "I want to adjust something before building"

In **auto mode**, skip this gate and proceed directly to Step 9.

### Step 9: Build and Test Integration

#### Step 9a: Build and Code Quality Validation
```bash
./gradlew :integrations:<integration_name>:build
./gradlew :integrations:<integration_name>:detekt
```

If build/detekt succeeds, proceed directly to Step 9b.

If there are failures, auto-fix and re-run (up to two attempts). If failures persist after two attempts, use `AskUserQuestion` to present the errors — this is **Checkpoint 3** and applies in both interactive and auto modes.

#### Step 9b: Write and Run Unit Tests

**Read `references/test-scaffold.md` first** (if not already loaded in context). It has the drop-in directory layout, `build.gradle.kts` test block, `TestUtils.kt`, the integration-test skeleton with mockk patterns, the `UtilsTest.kt` skeleton, and the `PomVerificationTest.kt` template. Do not invent a test structure from scratch — every existing integration follows the scaffold.

Work through the scaffold in this order — no per-sub-step approval gates:

1. **Add test dependencies and `tasks.withType<Test>` block** to `build.gradle.kts` per the scaffold.
2. **Create `src/test/kotlin/.../TestUtils.kt`** with `mockAnalytics`, `readFileAsJsonObject`, `mergeWithHigherPriorityTo` (copy verbatim from the scaffold).
3. **Create `src/test/resources/config/<integration_name>_config.json`** and `new_<integration_name>_config.json` fixtures mirroring the destination dashboard's payload. Use the closest existing integration's fixtures as the structural template.
4. **Write `<Name>IntegrationTest.kt`** with one `@Test` per implemented method — `create`, `getDestinationInstance`, `identify`, `track`, and any method on Step 7's work list (`reset`, lifecycle callbacks, etc.). Add `update` tests only if `update()` was overridden per Step 4b. Use the mockk pattern from the scaffold that matches the destination SDK's entry-point shape (recorded in Step 1's "Destination SDK Kotlin API" section). Include a `getDestinationInstance` test that asserts it returns `null` before `create()` runs and the configured instance after — this is the contract the Kotlin SDK relies on.
5. **Write `UtilsTest.kt`** covering the value-conversion helpers from Step 8 — the test shape depends on the strategy picked in Step 5/6 (Map-based gets type-preservation tests, per-type-getter gets coercion tests, data-class-decode gets valid/invalid JSON tests).
6. **Write `<Name>PomVerificationTest.kt`** — run `./gradlew :integrations:<integration_name>:generatePomFileForReleasePublication`, copy the generated XML into the test with versions normalized to placeholders per the scaffold, swap the system-property name to match `build.gradle.kts`.
7. **Run the tests**:
   ```bash
   ./gradlew :integrations:<integration_name>:test
   ```
   If any fail, auto-fix and re-run (up to two attempts). If failures persist, use `AskUserQuestion` to present them (Checkpoint 3).

Naming convention is non-negotiable: backticked `` `given X, when Y, then Z` ``. The scaffold's "What's out of scope" section documents what NOT to write (no Espresso, no SDK network mocking, no dashboard E2E).

### Step 10: Create Documentation
Generate README.md for the integration. Refer to other integrations for examples.

## Step Execution Protocol

### Checkpoint Summary

| Checkpoint | When | Interactive | Auto |
|---|---|---|---|
| 1 | After Step 1 (analysis) | Always | Only if non-preserving divergences exist |
| 2 | After Step 8 (implementation) | Always | Skip |
| 3 | After Step 9 (build/test failures) | Only on persistent failures | Only on persistent failures |

Do **not** add `AskUserQuestion` calls beyond these checkpoints. The user can always interrupt with feedback — they don't need a gate to do so.

When presenting generated code for approval at checkpoints, use the `preview` field on AskUserQuestion options to show code inline.

### User Modification Handling
- If the user modifies generated code mid-run, acknowledge, adapt remaining steps, and maintain consistency with their changes.
- If changes affect the approach fundamentally, ask for clarification via AskUserQuestion (this is an exceptional gate, not a checkpoint).

### Java-to-Kotlin Conversion
- **Preserve business logic** exactly as in Java version.
- **Use Kotlin idioms** (when expressions, extension functions, null safety):
  - Prefer `?.let { ... }` / `?.also { ... }` chains over null-check-then-`!!`. Never use `!!` on a value you just null-checked — use the safe-call chain instead (e.g., `primitive.booleanOrNull?.also { sdk.setAttribute(key, it) }` not `if (primitive.booleanOrNull != null) sdk.setAttribute(key, primitive.booleanOrNull!!)`).
  - Prefer `for ((key, value) in map)` with an `if` guard over `filterKeys { ... }.forEach { ... }` — the latter allocates an intermediate map for no benefit.
  - Use expression bodies (`= ...`) for single-expression functions; block bodies for multi-statement ones.
- **Value access**: Use the strategy and helpers from `references/value-conversion.md` matching the destination SDK's accepted type. Do **not** sprinkle inline `jsonObject["key"]?.jsonPrimitive?.content` calls through the integration code — that pattern is the helpers' job, and consistency with the existing integrations matters for review.
- **Maintain compatibility** with RudderStack event structure.

### Error Handling
- If the Java integration repo cannot be located (path invalid or clone fails), ask the user to verify the integration name or provide an alternate path/URL via AskUserQuestion.
- If business logic is unclear, ask for clarification via AskUserQuestion.
- If dependencies are missing, suggest adding them.

### Final Summary
After all steps completed:
```markdown
## Kotlin Integration Generation Complete

### Original Java Integration:
`rudder-integration-<integration_name>-android`

### Generated Kotlin Integration:
`integrations/<integration_name>/`

### Key Business Logic Converted:
- [List main methods and their Java counterparts]

### Files Created:
- [List all generated files]

### Test Results:
- Unit tests: [PASSED/FAILED status]
- Detekt analysis: [PASSED/FAILED status]
- Build compilation: [PASSED/FAILED status]
```
