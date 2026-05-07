plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
