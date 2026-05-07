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
}

dependencies {
    implementation(project(":android"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)

    // androidTest uses JUnit4 — JUnit5 instrumentation runners are awkward on Android
    // and AGP's testInstrumentationRunner expects a JUnit4-compatible runner.
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
