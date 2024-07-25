plugins {
    `kotlin-dsl`
    id("java-library")
}
repositories {
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
