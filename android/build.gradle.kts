@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.rudderstack.android2"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

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

    //implementation
    implementation(libs.android.core.ktx)

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
