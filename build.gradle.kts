// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.nexus)
}

fun getVersionName(): String {
    return if (project.hasProperty("release")) {
        RudderStackBuildConfig.AndroidAndCoreSDKs.VERSION_NAME
    } else {
        "${RudderStackBuildConfig.AndroidAndCoreSDKs.VERSION_NAME}-SNAPSHOT"
    }
}

allprojects {
    version = getVersionName()
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

nexusPublishing {
    repositories {
        sonatype {
            username = System.getenv("NEXUS_USERNAME")
            password = System.getenv("NEXUS_PASSWORD")
            stagingProfileId = System.getenv("SONATYPE_STAGING_PROFILE_ID")

            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

true // Needed to make the Suppress annotation work for the plugins block
