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