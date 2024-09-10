import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
}

val sampleRudderPropertiesFile: File = rootProject.file("${projectDir}/rudderstack.properties")
val sampleRudderProperties = Properties().apply {
    sampleRudderPropertiesFile.canRead().apply { load(FileInputStream(sampleRudderPropertiesFile)) }
}

android {
    val javaVersion = JavaVersion.VERSION_17//RudderstackBuildConfig.Build.JAVA_VERSION
    val jvm = 17
    val composeCompilerVersion = "1.4.8"//RudderstackBuildConfig.Kotlin.COMPILER_EXTENSION_VERSION
    val androidCompileSdkVersion = 34//RudderstackBuildConfig.Android.COMPILE_SDK
    val androidMinSdkVersion = 26
    val majorVersion = 0
    val minVersion = 1
    val patchVersion = 0
    val libraryVersionName = "${majorVersion}.${minVersion}.${patchVersion}"
    val libraryVersionCode = majorVersion * 1000 + minVersion * 100 + patchVersion

    compileSdk = androidCompileSdkVersion

    buildFeatures {
        buildConfig = true
    }


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

        buildConfigField(
            "String", "WRITE_KEY",
            sampleRudderProperties.getProperty("writeKey")
        )
        buildConfigField(
            "String", "CONTROL_PLANE_URL",
            sampleRudderProperties.getProperty("controlplaneUrl")
        )
        buildConfigField(
            "String", "DATA_PLANE_URL",
            sampleRudderProperties.getProperty("dataplaneUrl")
        )
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
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    kotlinOptions {
        jvmTarget = "17"//RudderstackBuildConfig.Build.JVM_TARGET
        javaParameters = true
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(jvm))
        }
    }
    buildFeatures {
        buildFeatures {
            compose = true
        }
        composeOptions {
            kotlinCompilerExtensionVersion = composeCompilerVersion
        }
        tasks.withType<Test> {
            useJUnitPlatform()
        }
        namespace = "com.rudderstack.android.sampleapp"
    }

    dependencies {
        implementation("com.google.android.material:material:1.12.0")
        //compose
        implementation("androidx.compose.ui:ui:1.6.7")
        implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
        implementation("androidx.compose.ui:ui-tooling:1.6.7")
        implementation("androidx.compose.foundation:foundation:1.6.7")
        // Material Design
        implementation("androidx.compose.material:material:1.6.7")
        // Integration with activities
        implementation("androidx.activity:activity-compose:1.9.0")
        // Integration with ViewModels
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
        // adding play services to generate advertising id
        implementation("com.google.android.gms:play-services-ads:22.1.0")

        implementation(project(":android"))
        implementation(project(":core"))
    }
}

tasks.named("preBuild").configure {
    doFirst {
        val oldCommitFile = file("${rootProject.rootDir}/.git/hooks/pre-commit")

        val newCommitFile = file("${rootProject.rootDir}/scripts/pre-commit")

        if (oldCommitFile.length() != newCommitFile.length()) {
            oldCommitFile.delete()
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
