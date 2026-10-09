import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

java {
    sourceCompatibility = RudderStackBuildConfig.CoreBuild.JAVA_VERSION
    targetCompatibility = RudderStackBuildConfig.CoreBuild.JAVA_VERSION
}
kotlin {
    jvmToolchain(RudderStackBuildConfig.Build.JVM_TOOLCHAIN)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(RudderStackBuildConfig.CoreBuild.JAVA_RELEASE)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(RudderStackBuildConfig.CoreBuild.JVM_TARGET))
        freeCompilerArgs.add("-Xjdk-release=${RudderStackBuildConfig.CoreBuild.JVM_TARGET}")
        freeCompilerArgs.add("-opt-in=com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
    }
    dependsOn("generatePomFileForReleasePublication")
    val pomFile = layout.buildDirectory.file("publications/release/pom-default.xml")
    inputs.file(pomFile)
    systemProperty("corePomFile", pomFile.get().asFile.absolutePath)
}

// Manual load test for server mode. CI does not run it.
// e.g., ./gradlew :core:serverLoadTest -PloadTestEventsPerSecond=10000 -PloadTestDurationSeconds=300
tasks.register<JavaExec>("serverLoadTest") {
    group = "verification"
    description = "Runs the server-mode load test against a local fake data plane."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.rudderstack.sdk.kotlin.core.server.loadtest.ServerLoadTestKt")
    maxHeapSize = "512m"
    args(
        listOfNotNull(
            findProperty("loadTestEventsPerSecond")?.toString(),
            findProperty("loadTestDurationSeconds")?.toString(),
        )
    )
}

// For generating SourcesJar and JavadocJar
tasks {
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }
}

// Task to generate a Kotlin file with version constants
tasks.register("generateVersionConstants") {
    val outputDir = layout.buildDirectory.dir("generated/source/version")
    val outputFile = layout.buildDirectory.file("generated/source/version/VersionConstants.kt")

    outputs.file(outputFile)

    doLast {
        outputDir.get().asFile.mkdirs()
        outputFile.get().asFile.writeText(
            """
            package source.version

            object VersionConstants {
                const val VERSION_NAME = "${RudderStackBuildConfig.SDK.Core.VERSION_NAME}"
                const val LIBRARY_NAME = "${RudderStackBuildConfig.SDK.Core.LibraryInfo.name}"
            }
            """.trimIndent()
        )
    }
}

// Ensure that the task runs before the compilation
tasks.named("compileKotlin") {
    dependsOn("generateVersionConstants")
}

// Include the generated source folder in the source set
sourceSets["main"].java.srcDir(layout.buildDirectory.dir("generated"))

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = true
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
    }
}

dependencies {
    //api
    api(libs.kotlinx.serialization.json)

    //implementation
    implementation(libs.kotlinx.coroutines.core)

    // detekt plugins
    detektPlugins(libs.detekt.formatting)


    //testImplementation
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.json.assert)
    testImplementation(libs.kotlinx.coroutines.test)

    testRuntimeOnly(libs.junit.jupiter.engine)
}

apply(from = rootProject.file("gradle/publishing/publishing.gradle.kts"))
