import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
    }
}

android {
    namespace = RudderStackBuildConfig.PackageName.Integrations.FACEBOOK
    compileSdk = RudderStackBuildConfig.Android.COMPILE_SDK

    defaultConfig {
        minSdk = RudderStackBuildConfig.Android.MIN_SDK

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

    implementation(libs.android.core.ktx)

    implementation (libs.facebook.android)

    testImplementation(platform(libs.junit.bom))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.json.assert)
    testImplementation(libs.navigation.runtime)

    testRuntimeOnly(libs.junit.jupiter.engine)
}
