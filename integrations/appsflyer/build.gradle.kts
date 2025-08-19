import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
        showStandardStreams = true
    }
}

android {
    namespace = RudderStackBuildConfig.Integrations.AppsFlyer.namespace
    compileSdk = RudderStackBuildConfig.Android.COMPILE_SDK

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = RudderStackBuildConfig.Android.MIN_SDK

        buildConfigField("String", "VERSION_NAME", "\"${RudderStackBuildConfig.Integrations.AppsFlyer.versionName}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
}

// For generating SourcesJar and JavadocJar
tasks {
    val sourceFiles = (android.sourceSets["main"].kotlin as com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet).srcDirs

    register<Javadoc>("withJavadoc") {
        isFailOnError = false

        setSource(sourceFiles)

        // add Android runtime classpath
        android.bootClasspath.forEach { classpath += project.fileTree(it) }

        // add classpath for all dependencies
        android.libraryVariants.forEach { variant ->
            variant.javaCompileProvider.get().classpath.files.forEach { file ->
                classpath += project.fileTree(file)
            }
        }
    }

    register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        dependsOn(named("withJavadoc"))
        val destination = named<Javadoc>("withJavadoc").get().destinationDir
        from(destination)
    }

    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceFiles)
    }
}

dependencies {
    // RudderStack SDK
    implementation(libs.rudder.android.sdk)

    // detekt plugins
    detektPlugins(libs.detekt.formatting)

    implementation(libs.android.core.ktx)

    // AppsFlyer SDK
    implementation(libs.appsflyer.android.sdk)
    implementation(libs.android.installreferrer)

    testImplementation(platform(libs.junit.bom))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.json.assert)
    testImplementation(libs.navigation.runtime)

    testRuntimeOnly(libs.junit.jupiter.engine)
}

apply(from = rootProject.file("gradle/publishing/publishing.integration.gradle.kts"))
