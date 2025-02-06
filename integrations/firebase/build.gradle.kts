import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi")
}

android {
    namespace = "com.rudderstack.integration.kotlin.firebase"
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

    implementation(libs.android.core.ktx)

    implementation (platform(libs.firebase.bom))
    implementation (libs.firebase.analytics)

    testImplementation(libs.junit)
}
