import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // kotlinx.serialization plugin needed for @Serializable types in the MCP server (Step 9).
    // The compiler-plugin generates serializers at compile time; without it, McpProtocol.kt's
    // request/response shapes fail to compile and every reference into the package cascades.
    alias(libs.plugins.kotlin.serialization)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
    }
}

android {
    namespace = "com.rudderstack.testapp"
    compileSdk = RudderStackBuildConfig.AndroidBuild.COMPILE_SDK

    defaultConfig {
        applicationId = "com.rudderstack.testapp"
        // The driver runs as its own APK so that destructive ops aimed at the SUT
        // (am force-stop, pm clear, am kill) don't take the test process with them.
        // See §11 of the design doc.
        testApplicationId = "com.rudderstack.testapp.driver"
        minSdk = RudderStackBuildConfig.AndroidBuild.MIN_SDK
        targetSdk = RudderStackBuildConfig.AndroidBuild.COMPILE_SDK
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
        targetCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
    }

    kotlinOptions {
        jvmTarget = RudderStackBuildConfig.Build.JVM_TARGET
    }

    // Step 9 — keep Kotlin reflection metadata in the APK.
    //
    // AGP's default packaging strips `*.kotlin_builtins` and `*.kotlin_module` files. Most
    // Kotlin code on Android works fine without them, but Ktor 2.x's request pipeline hits
    // them via `kotlin.reflect.jvm.internal.impl.types.TypeUtils` on the first
    // `call.respond(message: Any)`. Stripping them makes the POST hang with
    // `Resource not found in classpath: kotlin/kotlin.kotlin_builtins` in logcat.
    //
    // Re-include these resources only on the androidTest variant (live-mode MCP server) —
    // the main SUT APK doesn't host Ktor and stays at AGP defaults to keep its size lean.
    packaging {
        resources {
            merges += listOf(
                "**/*.kotlin_builtins",
                "**/*.kotlin_module",
            )
            // Avoid duplicate-resource failures when multiple deps ship the same META-INF
            // license / version files (Ktor + okhttp + kotlinx ones overlap).
            pickFirsts += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
            )
        }
    }
}

// =====================================================================================
// Step 6b destructive-op survival: two build hacks needed.
//
// The design doc (§11) only specifies the two-APK split (different applicationId vs
// testApplicationId). Once we wired Step 6's destructive lifecycle ops, two further AGP
// behaviors had to be defeated:
//
//   1. AGP synthesizes <instrumentation android:targetPackage> from applicationId (the SUT)
//      via a tempFile that overrides any source-manifest declaration. While targetPackage =
//      SUT, AMS ties the instrumentation lifecycle to the SUT's process — `am force-stop` /
//      `pm clear` / `am kill` on the SUT also kill the test process. The fix below rewrites
//      the merged manifest to point targetPackage at the driver, which decouples the lifecycle.
//
//   2. AGP's "tested variant" packaging strips classes (kotlin-stdlib, the SUT's code, etc.)
//      from the test APK on the assumption that the test runs in the SUT's process and gets
//      those classes for free. Once targetPackage = driver, the test runs in its own process
//      under its own UID and cannot reach the SUT's classpath. The injection task below
//      copies the SUT APK's dex files into the test APK and re-signs — making the test APK
//      self-contained at runtime.
//
// Both hacks are localized. If AGP exposes proper DSL knobs for these in a future version,
// these blocks become deletable in one PR.
// =====================================================================================

tasks.matching { it.name == "processDebugAndroidTestManifest" }.configureEach {
    doLast {
        val manifest = layout.buildDirectory
            .file("intermediates/packaged_manifests/debugAndroidTest/processDebugAndroidTestManifest/AndroidManifest.xml")
            .get().asFile
        if (!manifest.exists()) {
            throw GradleException(
                "Expected packaged androidTest manifest at $manifest — AGP layout changed?",
            )
        }
        val original = manifest.readText()
        val rewritten = original.replace(
            """android:targetPackage="com.rudderstack.testapp"""",
            """android:targetPackage="com.rudderstack.testapp.driver"""",
        )
        if (rewritten == original) {
            throw GradleException(
                "targetPackage rewrite found no match in $manifest — AGP no longer emits the SUT id, or the rewrite ran twice.",
            )
        }
        manifest.writeText(rewritten)
    }
}

// Post-package step: append every dex from the SUT APK to the androidTest APK so the driver
// process can resolve classes that AGP's tested-variant deduplication stripped (kotlin-stdlib,
// SUT-side code, transitive SDK classes). Re-signs the merged APK with the debug keystore so
// `adb install` (driven by `connectedDebugAndroidTest`) accepts it.
val injectSutClassesIntoAndroidTestApk = tasks.register("injectSutClassesIntoAndroidTestApk") {
    description = "Append SUT APK dex files to the androidTest APK and re-sign so the driver " +
        "process is self-contained under targetPackage=driver."
    dependsOn("packageDebug", "packageDebugAndroidTest")

    val sutApkFile = layout.buildDirectory.file("outputs/apk/debug/android-test-app-debug.apk")
    val testApkFile = layout.buildDirectory.file(
        "outputs/apk/androidTest/debug/android-test-app-debug-androidTest.apk",
    )
    // Link inputs to the producing tasks so Gradle resolves them after packaging runs,
    // not at configuration time when the APK doesn't exist yet on a clean build.
    inputs.files(tasks.named("packageDebug"), tasks.named("packageDebugAndroidTest"))
    outputs.file(testApkFile)

    doLast {
        val sutApk = sutApkFile.get().asFile
        val testApk = testApkFile.get().asFile
        if (!sutApk.exists() || !testApk.exists()) {
            throw GradleException("Expected APKs at $sutApk and $testApk — AGP layout changed?")
        }

        // Pull all dex bytes from the SUT APK.
        val sutDexes = mutableListOf<ByteArray>()
        ZipFile(sutApk).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                .forEach { entry -> sutDexes += zip.getInputStream(entry).readBytes() }
        }
        if (sutDexes.isEmpty()) {
            throw GradleException("No dex files found in SUT APK $sutApk — AGP layout changed?")
        }

        // Read every entry in the test APK, preserving the original compression method —
        // resources.arsc and similar must stay STORED on Android R+ or installPackageLI
        // refuses the install. Track the highest classesN.dex index so the appended SUT
        // dexes don't collide. Two parallel maps because Gradle's Kotlin DSL can't lower
        // a local `data class` inside a doLast.
        val dexNumberRegex = Regex("classes(\\d*)\\.dex")
        val testBytes = linkedMapOf<String, ByteArray>()
        val testMethods = mutableMapOf<String, Int>()
        var maxDex = 0
        ZipFile(testApk).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                testBytes[entry.name] = zip.getInputStream(entry).readBytes()
                testMethods[entry.name] = entry.method
                dexNumberRegex.matchEntire(entry.name)?.let { match ->
                    val n = match.groupValues[1].toIntOrNull() ?: 1
                    if (n > maxDex) maxDex = n
                }
            }
        }

        // Append SUT dexes (always DEFLATED — no Android-side requirement on dex compression).
        // Strip the v1 JAR signature artefacts; apksigner rebuilds them from scratch below.
        sutDexes.forEachIndexed { i, dex ->
            val name = "classes${maxDex + i + 1}.dex"
            testBytes[name] = dex
            testMethods[name] = ZipEntry.DEFLATED
        }
        val signatureNames = testBytes.keys.filter { name ->
            name.startsWith("META-INF/") &&
                (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") ||
                    name.endsWith("MANIFEST.MF"))
        }
        signatureNames.forEach { testBytes.remove(it); testMethods.remove(it) }

        // Rewrite the APK in place via a temp file → atomic rename. STORED entries need their
        // size + crc declared explicitly; DEFLATED entries can use defaults.
        val crc32 = CRC32()
        val tempApk = File(testApk.parentFile, "${testApk.nameWithoutExtension}.merged.apk")
        ZipOutputStream(tempApk.outputStream().buffered()).use { out ->
            testBytes.forEach { (name, bytes) ->
                val entryMethod = testMethods.getValue(name)
                val entry = ZipEntry(name).apply {
                    method = entryMethod
                    if (entryMethod == ZipEntry.STORED) {
                        size = bytes.size.toLong()
                        compressedSize = bytes.size.toLong()
                        crc32.reset()
                        crc32.update(bytes)
                        crc = crc32.value
                    }
                }
                out.putNextEntry(entry)
                out.write(bytes)
                out.closeEntry()
            }
        }
        if (!testApk.delete()) throw GradleException("Failed to delete original test APK at $testApk")
        if (!tempApk.renameTo(testApk)) throw GradleException("Failed to rename merged APK to $testApk")

        // Re-sign with the debug keystore. AGP's debug build uses the SDK's default keystore
        // at ~/.android/debug.keystore (alias androiddebugkey, password android). Pin
        // build-tools to the version AGP itself selected so we don't drift across SDK installs.
        val sdkDir = android.sdkDirectory
        val buildToolsVersion = android.buildToolsVersion
        val apksigner = File(sdkDir, "build-tools/$buildToolsVersion/apksigner")
        val zipalign = File(sdkDir, "build-tools/$buildToolsVersion/zipalign")
        if (!apksigner.exists()) throw GradleException("apksigner not found at $apksigner")

        // zipalign first so the v2 signature aligns to 4-byte boundaries (required for install).
        if (zipalign.exists()) {
            val aligned = File(testApk.parentFile, "${testApk.nameWithoutExtension}.aligned.apk")
            exec {
                commandLine(zipalign.absolutePath, "-f", "4", testApk.absolutePath, aligned.absolutePath)
            }
            if (!testApk.delete()) throw GradleException("Failed to delete pre-zipalign APK")
            if (!aligned.renameTo(testApk)) throw GradleException("Failed to rename zipaligned APK")
        }

        val keystore = File(System.getProperty("user.home"), ".android/debug.keystore")
        if (!keystore.exists()) {
            throw GradleException(
                "Debug keystore not found at $keystore — AGP normally creates this automatically. " +
                "Run any debug build first to generate it.",
            )
        }
        exec {
            commandLine(
                apksigner.absolutePath, "sign",
                "--ks", keystore.absolutePath,
                "--ks-pass", "pass:android",
                "--key-pass", "pass:android",
                "--ks-key-alias", "androiddebugkey",
                testApk.absolutePath,
            )
        }
    }
}

// Wire the injection so it runs before connectedDebugAndroidTest installs / launches the test APK.
tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
    dependsOn(injectSutClassesIntoAndroidTestApk)
}

// installDebugAndroidTest is the AGP task that pushes the test APK onto a device. The
// inject step rewrites that APK in-place, so install must wait until inject runs — without
// this the live-mode flow (startMcpLive) would push the un-injected APK and the driver
// process would crash on launch with NoClassDefFoundError on SUT-side classes.
tasks.matching { it.name == "installDebugAndroidTest" }.configureEach {
    dependsOn(injectSutClassesIntoAndroidTestApk)
}

// NOTE: Running tests directly from Android Studio's "Run Test" gutter does NOT work today —
// AS's IDE-side task graph for instrumentation tests skips `packageDebug` even when invoking
// `connectedDebugAndroidTest`, leaving inject without a SUT APK to merge from. CLI works
// (`./gradlew :android-test-app:connectedDebugAndroidTest`); AS integration is on the backlog.

/**
 * `startMcpLive` — orchestrate a live-mode MCP session.
 *
 * Build + install both APKs, `adb forward` the MCP port, then `am instrument` the [McpLiveTest]
 * class with the `mcp.live=true` opt-in argument. The test boots the MCP server inside the
 * driver process and blocks; the host machine connects at `http://localhost:5111/sse`.
 *
 * The task itself blocks while `am instrument` runs (test blocks until idle timeout or
 * Ctrl+C). Forwarding stdout/stderr live so the user sees server logs and can kill cleanly.
 *
 * **Test runner.** With Step 6b's `testApplicationId = "com.rudderstack.testapp.driver"`,
 * the test APK applicationId is `com.rudderstack.testapp.driver` (no `.test` suffix), so
 * the instrumentation target is `<that>/androidx.test.runner.AndroidJUnitRunner`.
 *
 * **Why `adb forward` and not `adb reverse`.** The server lives on the *device*; the AI
 * client lives on the *host*. `adb forward host_port → device_port` is the host-to-device
 * direction (host opens a local socket, traffic tunnels to the device). The doc's §14.2
 * reads "adb reverse" — that's the opposite topology (server on host, device-side client),
 * which doesn't match where the engine helpers run. We use `adb forward` and document the
 * deviation here. Bonus: `adb reverse` would have the device's adbd bind to 5111, blocking
 * the in-process Ktor server from binding — an empirical confirmation of the direction.
 */
tasks.register("startMcpLive") {
    description = "Boot the MCP server in live mode and adb-forward the port. Blocks until idle timeout."
    group = "mcp"
    dependsOn("installDebug", "installDebugAndroidTest")

    val mcpPort = 5111
    val testClass = "com.rudderstack.scenarioengine.scenarios.mcp.McpLiveTest"
    val instrumentation = "com.rudderstack.testapp.driver/androidx.test.runner.AndroidJUnitRunner"

    doLast {
        // Clear any prior forward / reverse on the same port to avoid Address already in use.
        exec {
            commandLine("adb", "forward", "--remove", "tcp:$mcpPort")
            isIgnoreExitValue = true
        }
        exec {
            commandLine("adb", "reverse", "--remove", "tcp:$mcpPort")
            isIgnoreExitValue = true
        }
        // host:5111 → device:5111. Set up before the test starts so the host can connect
        // as soon as the in-process Ktor server is ready, without races on the @Before.
        exec {
            commandLine("adb", "forward", "tcp:$mcpPort", "tcp:$mcpPort")
        }
        println()
        println("=========================================================")
        println("  MCP server starting…")
        println("  Connect from this host: http://localhost:$mcpPort/sse")
        println("  Press Ctrl+C to stop the session.")
        println("=========================================================")
        println()

        // -w: wait for completion. -r: raw output (verbose).
        // -e mcp.live true: opt-in arg the McpLiveTest's `assumeTrue` checks.
        // -e class <FQN>: restrict to the live-mode test (don't run anything else).
        exec {
            commandLine(
                "adb", "shell", "am", "instrument", "-w", "-r",
                "-e", "mcp.live", "true",
                "-e", "class", testClass,
                instrumentation,
            )
        }
    }
}

dependencies {
    implementation(project(":android"))
    // Engine domain types + IPC ABI constants. Lives in its own JVM library so that AGP
    // packages it into BOTH the SUT APK and the driver (androidTest) APK as a transitive AAR
    // dependency. With the Step 6b targetPackage rewrite the driver runs in its own process
    // and cannot rely on the SUT's classpath at runtime — the shared module is what makes
    // those classes visible from both processes.
    implementation(project(":scenarioengine"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)

    // androidTest uses JUnit4 — JUnit5 instrumentation runners are awkward on Android
    // and AGP's testInstrumentationRunner expects a JUnit4-compatible runner.
    androidTestImplementation(libs.junit4)
    // With targetPackage = driver, the test APK runs in its own process and AGP strips
    // shared deps from the test APK assuming the SUT's classpath is reachable at runtime.
    // Explicitly request kotlin-stdlib + coroutines + serialization here so they're force-
    // packaged into the driver APK regardless of AGP's tested-variant deduplication.
    androidTestImplementation(kotlin("stdlib"))
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.kotlinx.serialization.json)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.android.core.ktx) // ContextCompat.registerReceiver
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // MCP server — hosts the JSON-RPC + SSE endpoint inside the driver process during
    // live-mode runs. Ktor 2.3.13 is the last line that compiles against Kotlin 1.9; the
    // official MCP Kotlin SDK requires Kotlin 2.x and is therefore unusable here, so the
    // protocol layer (initialize / tools/list / tools/call) is hand-rolled on top.
    androidTestImplementation(libs.ktor.server.core)
    androidTestImplementation(libs.ktor.server.cio)
    // Ktor's request pipeline reflects on inbound types in `call.receive<T>` and on response
    // shapes in `call.respond(message)`. Without kotlin-reflect, the first POST hits
    // `Resource not found in classpath: kotlin/kotlin.kotlin_builtins` and the call hangs.
    // The packaging block below is the matching half of the fix.
    androidTestImplementation(kotlin("reflect"))
}
