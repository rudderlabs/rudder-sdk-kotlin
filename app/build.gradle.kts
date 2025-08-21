import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val rudderStackPropertiesFile: File = rootProject.file("${projectDir}/rudderstack.properties")
val sampleRudderProperties = Properties().apply {
    if (rudderStackPropertiesFile.canRead() && rudderStackPropertiesFile.length() > 0) {
        load(FileInputStream(rudderStackPropertiesFile))
    } else {
        println("Properties file is empty or cannot be read.")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
    }
}

android {
    val composeCompilerVersion = RudderStackBuildConfig.Kotlin.COMPILER_EXTENSION_VERSION
    val androidCompileSdkVersion = RudderStackBuildConfig.Android.COMPILE_SDK
    val androidMinSdkVersion = RudderStackBuildConfig.Android.MIN_SDK
    val majorVersion = 0
    val minVersion = 1
    val patchVersion = 0
    val libraryVersionName = "${majorVersion}.${minVersion}.${patchVersion}"
    val libraryVersionCode = majorVersion * 1000 + minVersion * 100 + patchVersion

    namespace = "com.rudderstack.android.sampleapp"
    compileSdk = androidCompileSdkVersion

    defaultConfig {
        applicationId = "com.rudderstack.android.sampleapp"
        minSdk = androidMinSdkVersion
        targetSdk = androidCompileSdkVersion
        versionCode = libraryVersionCode
        versionName = libraryVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "WRITE_KEY", sampleRudderProperties.getProperty("writeKey", "\"<WRITE_KEY>\""))
        buildConfigField(
            "String",
            "DATA_PLANE_URL",
            sampleRudderProperties.getProperty("dataPlaneUrl", "\"<DATA_PLANE_URL>\"")
        )
        // For self hosted or proxied control plane, please uncomment and provide your URL:
        // buildConfigField("String", "CONTROL_PLANE_URL", sampleRudderProperties.getProperty("controlPlaneUrl", "\"<CONTROL_PLANE_URL>\""))
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
                )
            )
        }
    }

    compileOptions {
        sourceCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
        targetCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
    }
    kotlinOptions {
        jvmTarget = RudderStackBuildConfig.Build.JVM_TARGET
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":android"))

    // RudderStack Integrations
    // implementation(project(":integrations:adjust"))
    // implementation(project(":integrations:braze")) // This requires minimum Sdk version of 25 and above.
    // implementation(project(":integrations:appsflyer"))
    //compose
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.ui.tooling)
    implementation(libs.foundation)
    // Material Design
    implementation(libs.androidx.material3.android)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // Integration with activities
    implementation(libs.androidx.activity.compose)
    // Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // adding play services to generate advertising id
    implementation(libs.play.services.ads)
    implementation(libs.timber)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.json.assert)
    testImplementation(libs.navigation.runtime)

    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.register("setupGitHooks") {
    description = "Configure Git to use hooks from scripts directory"
    group = "setup"

    doLast {
        // Check current hooks path configuration
        val currentHooksPath = try {
            val output = ByteArrayOutputStream()
            exec {
                commandLine("git", "config", "--get", "core.hooksPath")
                standardOutput = output
                isIgnoreExitValue = true
            }
            output.toString().trim()
        } catch (e: Exception) {
            ""
        }

        when {
            currentHooksPath == "scripts" -> {
                println("✅ Git hooks already configured correctly")
            }
            currentHooksPath.isEmpty() -> {
                // Configure Git to use scripts directory for hooks
                exec {
                    commandLine("git", "config", "core.hooksPath", "scripts")
                }
                println("🔧 Configured Git to use scripts/ directory for hooks")
            }
            else -> {
                println("⚠️ Git hooks path is currently set to: $currentHooksPath")
                println("💡 To use project hooks, run: git config core.hooksPath scripts")
            }
        }

        // Make hook scripts executable
        val hookFiles = listOf("pre-commit", "pre-push", "commit-msg")
        hookFiles.forEach { hookName ->
            val hookFile = file("${rootProject.rootDir}/scripts/$hookName")
            if (hookFile.exists()) {
                hookFile.setExecutable(true)
                println("📋 Made $hookName executable")
            } else {
                println("⚠️ Hook file not found: scripts/$hookName")
            }
        }

        // Clean up old copied hooks if they exist
        val oldHooksDir = file("${rootProject.rootDir}/.git/hooks")
        hookFiles.forEach { hookName ->
            val oldHookFile = File(oldHooksDir, hookName)
            if (oldHookFile.exists() && !oldHookFile.readText().contains("#!/bin/bash")) {
                // Only delete if it looks like our copied hook (not a custom hook)
                val scriptContent = file("${rootProject.rootDir}/scripts/$hookName").readText()
                if (oldHookFile.readText() == scriptContent) {
                    oldHookFile.delete()
                    println("🧹 Removed old copied hook: .git/hooks/$hookName")
                }
            }
        }

        println("✅ Git hooks setup complete!")
    }
}

// Auto-run setup on build to ensure hooks are configured
tasks.named("build") {
    dependsOn("setupGitHooks")
}
