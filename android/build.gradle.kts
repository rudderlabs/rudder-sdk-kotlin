import io.gitlab.arturbosch.detekt.Detekt

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.detekt)
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
    namespace = RudderStackBuildConfig.PackageName.PACKAGE_NAME
    compileSdk = RudderStackBuildConfig.Android.COMPILE_SDK

    buildFeatures {
        buildFeatures {
            buildConfig = true
        }
    }

    defaultConfig {
        minSdk = RudderStackBuildConfig.Android.MIN_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "VERSION_NAME", RudderStackBuildConfig.Version.VERSION_NAME)
        buildConfigField("int", "VERSION_CODE", RudderStackBuildConfig.Version.VERSION_CODE)
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
    //api
    api(project(":core"))

    // detekt plugins
    detektPlugins(libs.detekt.formatting)

    // compile only
    compileOnly(libs.navigation.runtime)

    //implementation
    implementation(libs.android.core.ktx)
    implementation(libs.lifecycle.process)

    //testImplementation
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.json.assert)
    testImplementation(libs.navigation.runtime)
}
