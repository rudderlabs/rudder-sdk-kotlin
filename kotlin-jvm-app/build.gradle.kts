plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
    targetCompatibility = RudderStackBuildConfig.Build.JAVA_VERSION
}
kotlin {
    jvmToolchain(RudderStackBuildConfig.Build.JVM_TOOLCHAIN)
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.annotation.jvm)
}
