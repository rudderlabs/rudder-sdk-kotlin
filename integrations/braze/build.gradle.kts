import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.serialization)
}

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

android {
    namespace = RudderStackBuildConfig.PackageName.Integrations.BRAZE
    compileSdk = RudderStackBuildConfig.Android.COMPILE_SDK

    buildFeatures {
        buildFeatures {
            buildConfig = true
        }
    }

    defaultConfig {
        minSdk = 25

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
        targetCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
    }
    kotlinOptions {
        jvmTarget = RudderStackBuildConfig.Build.JVM_TARGET
    }
    kotlin {
        jvmToolchain(RudderStackBuildConfig.Build.JVM_TOOLCHAIN)
    }
}
dependencies {
    api(project(":android"))

    // detekt plugins
    detektPlugins(libs.detekt.formatting)

    // implementation
    implementation(libs.android.core.ktx)
    // Braze SDK
    implementation(libs.android.sdk.ui)

    // testImplementation
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.json.assert)
}
