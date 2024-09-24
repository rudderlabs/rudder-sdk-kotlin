import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
                const val VERSION_NAME = ${RudderStackBuildConfig.Version.VERSION_NAME}
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

    //compileOnly
    compileOnly(libs.gson)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.module)
    compileOnly(libs.moshi)
    compileOnly(libs.moshi.kotlin)
    compileOnly(libs.moshi.adapters)

    // detekt plugins
    detektPlugins(libs.detekt.formatting)

    //testImplementation
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.awaitility)
    testImplementation(libs.junit)
    testImplementation(libs.hamcrest)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.json.assert)
    testImplementation(libs.kotlinx.coroutines.test)
}
