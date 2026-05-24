# Test Scaffold

Concrete drop-in test structure for a new Kotlin integration. Every existing integration (adjust, braze, firebase, appsflyer, facebook) follows this shape — replicate it verbatim, then write tests for the methods this integration actually implements.

## What every integration must have

Three test files plus a helpers file:

| File | Purpose |
|---|---|
| `<Name>IntegrationTest.kt` | Exercises the integration's `create`, `update`, `identify`, `track`, and any other implemented methods against mocked destination-SDK calls. |
| `UtilsTest.kt` | Unit tests for everything in `Utils.kt` (value conversion, key sanitization, event mapping, etc.). |
| `TestUtils.kt` | Shared test helpers — `mockAnalytics`, `readFileAsJsonObject`, `mergeWithHigherPriorityTo`. |
| `<Name>PomVerificationTest.kt` | Pins the generated Maven POM so changes to dependency groups or scopes are caught at test time, not at publish time. |

Plus `src/test/resources/config/*.json` fixtures that mirror the real destination-config payloads from the dashboard.

## Directory layout

```
integrations/<integration_name>/
└── src/
    └── test/
        ├── kotlin/                              # use kotlin/, not java/ — module is Kotlin-only
        │   └── com/rudderstack/integration/kotlin/<integration_name>/
        │       ├── <Name>IntegrationTest.kt
        │       ├── UtilsTest.kt
        │       ├── TestUtils.kt
        │       └── <Name>PomVerificationTest.kt
        └── resources/
            └── config/
                ├── <integration_name>_config.json
                └── new_<integration_name>_config.json   # for testing update() behavior
```

> **Source-set variance**: adjust and braze use `src/test/java/...` (Gradle's Kotlin plugin treats `java/` and `kotlin/` identically for Kotlin sources). For new integrations, **prefer `src/test/kotlin/...`** — it's what firebase, appsflyer, and facebook use, and it matches the language of the module.

## `build.gradle.kts` test block

Add this to the integration's `build.gradle.kts`. Adjust the `<integration_name>` references; everything else is identical across modules.

```kotlin
dependencies {
    // ... main deps ...

    // testImplementation
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.json.assert)
    // Add testImplementation(libs.kotlinx.coroutines.test) if you mock coroutine dispatchers
    // (firebase does this; adjust does not — only needed when you use TestScope/TestDispatcher).

    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
    }
    // POM verification plumbing — see <Name>PomVerificationTest section below
    dependsOn("generatePomFileForReleasePublication")
    val pomFile = layout.buildDirectory.file("publications/release/pom-default.xml")
    inputs.file(pomFile)
    systemProperty("<integration_name>PomFile", pomFile.get().asFile.absolutePath)
}
```

The `dependsOn("generatePomFileForReleasePublication")` and `systemProperty(...)` lines feed the POM verification test. Match the system property name to whatever your `PomVerificationTest` reads (`adjustPomFile`, `brazePomFile`, `<name>PomFile`).

## `TestUtils.kt`

Drop in verbatim, swap the package. This is the canonical version — promote `readFileAsJsonObject` and `mergeWithHigherPriorityTo` to this file so they're not redefined inline per-test-class as adjust does.

```kotlin
package com.rudderstack.integration.kotlin.<integration_name>

import com.rudderstack.sdk.kotlin.android.Analytics
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader

/**
 * Builds a relaxed mock Analytics whose dispatchers route through the test scope —
 * needed whenever the integration touches `analytics.analyticsScope` or any of the
 * `*Dispatcher` accessors. Required for tests that exercise coroutine-based paths.
 */
fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mockAnalytics = mockk<Analytics>(relaxed = true)

    mockAnalytics.also {
        every { it.analyticsScope } returns testScope
        every { it.analyticsDispatcher } returns testDispatcher
        every { it.fileStorageDispatcher } returns testDispatcher
        every { it.networkDispatcher } returns testDispatcher
        every { it.integrationsDispatcher } returns testDispatcher
    }

    return mockAnalytics
}

/**
 * Loads a JSON fixture from `src/test/resources/<fileName>` and parses it into a JsonObject.
 * Use for destination configs and event payload fixtures.
 */
internal fun Any.readFileAsJsonObject(fileName: String): JsonObject {
    val inputStream = this::class.java.classLoader!!.getResourceAsStream(fileName)
    val jsonString = inputStream!!.bufferedReader().use(BufferedReader::readText)
    return Json.parseToJsonElement(jsonString).jsonObject
}

/**
 * Merges two JsonObjects with the right-hand side taking priority on key conflicts.
 * Useful for building event fixtures (base context + per-test overrides).
 */
infix fun JsonObject.mergeWithHigherPriorityTo(other: JsonObject): JsonObject {
    return JsonObject(this.toMap() + other.toMap())
}
```

## `<Name>IntegrationTest.kt` skeleton

The full pattern, lifted from adjust's tests. Replace `<Name>`/`<name>` and substitute the destination-SDK types and call sites.

```kotlin
package com.rudderstack.integration.kotlin.<integration_name>

import android.app.Application
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val PATH_TO_CONFIG = "config/<integration_name>_config.json"
private const val PATH_TO_NEW_CONFIG = "config/new_<integration_name>_config.json"

class <Name>IntegrationTest {

    private val mockIntegrationConfig: JsonObject = readFileAsJsonObject(PATH_TO_CONFIG)
    private val mockNewIntegrationConfig: JsonObject = readFileAsJsonObject(PATH_TO_NEW_CONFIG)

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    private lateinit var integration: <Name>Integration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock the destination SDK's static / object entry points.
        // For a Kotlin `object` SDK: mockkObject(Foo)
        // For a Java class with static helpers: mockkStatic(Foo::class)
        // For a singleton with INSTANCE: mockkStatic(Foo::class) then mock Foo.INSTANCE
        mockkStatic(/* destination SDK entry point */)
        every { /* destination SDK init call */ } just Runs

        // Mock the analytics.application accessor used by create()
        every { mockAnalytics.application } returns mockApplication

        integration = <Name>Integration()
        integration.setup(analytics = mockAnalytics)   // calls create() internally
    }

    // === create() / update() / getDestinationInstance() ===

    @Test
    fun `given integration is not initialised, when instance is requested, then null is returned`() {
        val fresh = <Name>Integration()
        assertNull(fresh.getDestinationInstance())
    }

    @Test
    fun `given integration is initialised, when instance is requested, then destination SDK instance is returned`() {
        integration.create(mockIntegrationConfig)
        // assertEquals(expectedInstance, integration.getDestinationInstance())
    }

    @Test
    fun `when integration is initialised, then destination SDK is configured with expected values`() {
        integration.create(mockIntegrationConfig)
        verify { /* destination SDK initialization call with expected args */ }
    }

    @Test
    fun `given destination config is updated, when update is called, then SDK is NOT re-initialised`() {
        integration.create(mockIntegrationConfig)
        integration.update(mockNewIntegrationConfig)
        verify(exactly = 1) { /* destination SDK initialization call */ }
    }

    // === identify() ===

    @Test
    fun `given identify event has userId and traits, when identify is called, then destination receives them`() {
        integration.create(mockIntegrationConfig)
        val event = IdentifyEvent(userId = "u-1", traits = /* JsonObject of traits */ emptyJsonObject)
        integration.identify(event)
        verify { /* destination SDK identify call */ }
    }

    // === track() ===

    @Test
    fun `given track event has event name and properties, when track is called, then destination receives them`() {
        integration.create(mockIntegrationConfig)
        val event = TrackEvent(event = "Order Completed", properties = /* JsonObject */ emptyJsonObject)
        integration.track(event)
        verify { /* destination SDK track call */ }
    }

    // === reset() — only if implemented ===

    @Test
    fun `when reset is called, then destination logout is invoked`() {
        integration.create(mockIntegrationConfig)
        integration.reset()
        verify { /* destination SDK logout/reset call */ }
    }

    // === ActivityLifecycleObserver — only if implemented ===
    // Pattern: pass a FragmentActivity (or whatever the integration filters on),
    // call onActivityResumed/onActivityDestroyed, assert the integration's internal
    // state via the side effects on the next track/identify call.
}
```

### mockk patterns by destination-SDK shape

The mock setup in `@BeforeEach` depends on what the destination SDK looks like (this ties back to Step 1's "Destination SDK Kotlin API" analysis):

- **Kotlin `object`** (destination SDK is a Kotlin singleton, called as `Foo.bar()`): `mockkObject(Foo)`, then `every { Foo.bar(...) } just Runs`. `mockkObject` is a *spy* — every method the integration calls must have an explicit `every { ... } just Runs` (or returns) stub, otherwise the real implementation runs and typically hits unmocked Android framework calls.
- **Class with static helpers via `@JvmStatic`** (adjust's `Adjust.initSdk`): `mockkStatic(Foo::class)`, then `every { Foo.bar(...) } just Runs`.
- **Singleton with `INSTANCE`** (Java class observed from Kotlin): `mockkStatic(Foo::class)` and mock `Foo.INSTANCE.bar(...)`.
- **Instance-required class** (firebase's `FirebaseAnalytics`): `mockk<Foo>(relaxed = true)`, inject via the integration's factory hook.

The choice mirrors Step 1's analysis directly — if Step 1 captured "entry point shape: Kotlin object", the mock pattern is `mockkObject`.

## `UtilsTest.kt` skeleton

Targets the value-conversion helpers in `Utils.kt`. The shape depends on which value-conversion strategy was picked (see `value-conversion.md`):

```kotlin
package com.rudderstack.integration.kotlin.<integration_name>

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UtilsTest {

    // For Map-based strategy (appsflyer): test toAnyMap / extractValue
    @Test
    fun `when toAnyMap converts a JsonObject with mixed primitives, then types are preserved`() {
        val input = buildJsonObject {
            put("str", "hello")
            put("int", 42)
            put("bool", true)
            put("dbl", 3.14)
        }

        val result = input.toAnyMap()

        assertEquals("hello", result["str"])
        assertEquals(42, result["int"])
        assertEquals(true, result["bool"])
        assertEquals(3.14, result["dbl"])
    }

    @Test
    fun `when toAnyMap encounters a null entry, then the key is dropped`() {
        // ... see appsflyer/UtilsTest.kt for the full set of edge cases
    }

    // For event-mapping helpers (appsflyer, firebase): test the eventName → SDK-event mapping
    // For per-type getters (adjust): test getStringOrNull / getIntOrNull with type coercion
    // For data-class decode (adjust, braze): test parseConfig<T> with valid/invalid JSON
}
```

Refer to the closest existing `UtilsTest.kt` matching this integration's value-conversion strategy.

## `<Name>PomVerificationTest.kt`

This test guards against accidental publishing-config drift. Copy adjust's verbatim, swap two things:

1. The system-property name in `System.getProperty(...)` to match what you set in `build.gradle.kts` (`<name>PomFile`).
2. The expected POM XML to match this integration's published artifact: `groupId`, `artifactId`, dependency list with their scopes.

```kotlin
package com.rudderstack.integration.kotlin.<integration_name>

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File

class <Name>PomVerificationTest {

    @Test
    fun `given publishing script generates pom, when pom is read, then it matches expected xml`() {
        val pomFile = File(System.getProperty("<integration_name>PomFile"))
        val generatedPom = pomFile.readText().trimEnd().normalizeVersions()
        assertEquals(expectedPomXml().normalizeVersions(), generatedPom)
    }

    /**
     * Replaces all `<version>...</version>` values with placeholders so version bumps don't break the test.
     * Semver ranges like `[1.3.0, 2.0.0)` become `[x.y.z, x.y.z)` (range structure preserved);
     * flat versions like `1.3.0-SNAPSHOT` become `x.y.z`.
     */
    private fun String.normalizeVersions(): String =
        replace(Regex("""<version>[^<]+</version>"""), "<version>x.y.z</version>")

    private fun expectedPomXml(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.rudderstack.integration.kotlin</groupId>
          <artifactId><integration_name></artifactId>
          <version>x.y.z</version>
          <packaging>aar</packaging>
          <!-- ... name, description, url, licenses, developers, scm ... copy from adjust/PomVerificationTest.kt ... -->
          <dependencies>
            <!-- Kotlin stdlib comes first, then SDK dependencies (sorted by groupId in the generated POM).
                 Copy the structure from adjust/PomVerificationTest.kt and replace dependency entries
                 to match what this integration's build.gradle.kts produces. -->
          </dependencies>
        </project>
    """.trimIndent()
}
```

**How to get the expected POM the first time**: run `./gradlew :integrations:<integration_name>:generatePomFileForReleasePublication`, open the generated XML at `integrations/<integration_name>/build/publications/release/pom-default.xml`, normalize versions by hand (apply the regex above), and paste it in. Subsequent edits to `build.gradle.kts` will break this test until you re-sync the expected XML — that's the point.

## Test fixture conventions

Under `src/test/resources/config/`:

```
<integration_name>_config.json          # the canonical destination config used by most tests
new_<integration_name>_config.json      # an alternate config used for update() tests — should differ from the first in at least one meaningful field
```

The fixtures must be parseable by whatever `parseConfig<T>` (or equivalent) the integration uses. If your config has a feature flag that toggles behavior, add a fixture per branch.

## Test naming convention

Use backticked, sentence-form names following the **given/when/then** pattern:

```kotlin
@Test
fun `given integration is initialised, when track event is made, then destination SDK receives it`() { ... }
```

This shows up cleanly in IDE test runners and gradle reports. Not negotiable — every existing integration follows it, mixed naming will fail review.

## Running the tests

```bash
./gradlew :integrations:<integration_name>:test
./gradlew :integrations:<integration_name>:test --tests "*<Name>IntegrationTest.*"
./gradlew :integrations:<integration_name>:test --tests "*PomVerificationTest.*"
```

The PomVerificationTest depends on `generatePomFileForReleasePublication` automatically (via the `dependsOn` line in `tasks.withType<Test>`), so a clean `test` run will produce the POM first.

## What's deliberately *out of scope* here

- **Espresso / instrumented tests.** Integration unit tests run on the JVM with mocked Android. Instrumented tests are heavier and aren't part of this scaffold; existing integrations don't ship them.
- **Mocking the destination SDK's network layer.** Mock the SDK's public methods (`Adjust.trackEvent(...)`, `FirebaseAnalytics.logEvent(...)`) not the HTTP they make — the SDK is responsible for its own network, the integration only forwards calls.
- **End-to-end tests against real destination dashboards.** Manual verification, not test-suite work.
