plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

val rudderStackCoreVersion: String by project

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.rudderstack.sdk.kotlin:core:$rudderStackCoreVersion")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
