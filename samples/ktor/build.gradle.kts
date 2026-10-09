plugins {
    kotlin("jvm") version "2.1.21"
    application
}

val rudderStackCoreVersion: String by project

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.rudderstack.sample.ktor.ApplicationKt")
}

dependencies {
    implementation("com.rudderstack.sdk.kotlin:core:$rudderStackCoreVersion")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("ch.qos.logback:logback-classic:1.5.18")
}
