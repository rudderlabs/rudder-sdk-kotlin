plugins {
    `kotlin-dsl`
    id("java-library")
}
repositories {
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {

}