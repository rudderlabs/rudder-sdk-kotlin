import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val sampleRudderPropertiesFile: File = rootProject.file("${projectDir}/rudderstack.properties")
val sampleRudderProperties = Properties().apply {
    sampleRudderPropertiesFile.canRead().apply { load(FileInputStream(sampleRudderPropertiesFile)) }
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
        buildConfigField("String", "WRITE_KEY", sampleRudderProperties.getProperty("writeKey"))
        buildConfigField("String", "DATA_PLANE_URL", sampleRudderProperties.getProperty("dataPlaneUrl"))
        // For self hosted control plane url, please uncomment and use value below
        // and add it in SDK's initialization Configuration.
        // buildConfigField("String", "CONTROL_PLANE_URL", sampleRudderProperties.getProperty("controlPlaneUrl"))
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

    implementation(libs.material)
    //compose
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.ui.tooling)
    implementation(libs.foundation)
    // Material Design
    implementation(libs.androidx.material)
    // Integration with activities
    implementation(libs.androidx.activity.compose)
    // Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // adding play services to generate advertising id
    implementation(libs.play.services.ads)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.json.assert)
    testImplementation(libs.navigation.runtime)
}

tasks.named("preBuild").configure {
    doFirst {
        val oldCommitFile = file("${rootProject.rootDir}/.git/hooks/pre-commit")
        val oldPushFile = file("${rootProject.rootDir}/.git/hooks/pre-push")
        val oldCommitMessageFile = file("${rootProject.rootDir}/.git/hooks/commit-msg")
        val newCommitFile = file("${rootProject.rootDir}/scripts/pre-commit")
        val newPushFile = file("${rootProject.rootDir}/scripts/pre-push")
        val newCommitMessageFile = file("${rootProject.rootDir}/scripts/commit-msg")
        if (
            oldCommitFile.length() != newCommitFile.length() ||
            oldPushFile.length() != newPushFile.length() ||
            oldCommitMessageFile.length() != newCommitMessageFile.length()
        ) {
            oldCommitFile.delete()
            oldPushFile.delete()
            oldCommitMessageFile.delete()
            println("Old hooks are deleted.")

            copy {
                from("${rootProject.rootDir}/scripts/")
                into("${rootProject.rootDir}/.git/hooks/")
                // to make the git hook executable
                fileMode = "0777".toInt(8)
            }
            println("New hooks are copied.")
        }
    }
}
