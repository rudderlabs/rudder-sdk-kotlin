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
    namespace = RudderStackBuildConfig.PacakageName.ANDROID
    compileSdk = 34

    buildFeatures {
        buildFeatures {
            buildConfig = true
        }
    }

    defaultConfig {
        minSdk = 21

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    //api
    api(project(":core"))

    // detekt plugins
    detektPlugins(libs.detekt.formatting)

    // compile only
    compileOnly(libs.navigation.fragment)

    //implementation
    implementation(libs.android.core.ktx)
    implementation(libs.lifecycle.process)

    //testImplementation
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
}
