plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
    targetCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
}

kotlin {
    jvmToolchain(RudderStackBuildConfig.Build.JVM_TOOLCHAIN)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("failed")
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
