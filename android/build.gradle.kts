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
    namespace = "com.rudderstack.android2"
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
        buildConfigField("String", "VERSION_CODE", RudderStackBuildConfig.Version.VERSION_CODE)
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

    //compileOnly
    compileOnly(libs.work)
    compileOnly(libs.work.multiprocess)

    // detekt plugins
    detektPlugins(libs.detekt.formatting)

    //implementation
    implementation(libs.android.core.ktx)
    implementation(libs.lifecycle.process)

    //testImplementation
    testImplementation(project(":core"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.android.x.test)
    testImplementation(libs.android.x.testrules)
    testImplementation(libs.android.x.test.ext.junitktx)
    testImplementation(libs.awaitility)
    testImplementation(libs.hamcrest)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.robolectric)
    testImplementation(libs.work.test)
    
    //androidTestImplementation
    androidTestImplementation(libs.android.x.test.ext.junitktx)
}
