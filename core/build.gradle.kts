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
                const val LIBRARY_PACKAGE_NAME = ${RudderStackBuildConfig.PacakageName.KOTLIN}
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
    implementation(libs.androidx.annotation.jvm)

    // detekt plugins
    detektPlugins(libs.detekt.formatting)

    //testImplementation
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.json.assert)
    testImplementation(libs.kotlinx.coroutines.test)
}
