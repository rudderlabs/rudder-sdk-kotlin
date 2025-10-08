// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.io.ByteArrayOutputStream

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

// Git Hooks Setup - Project-wide development tooling
tasks.register("setupGitHooks") {
    description = "Configure Git to use hooks from scripts/git-hooks directory"
    group = "setup"

    doLast {
        // Check current hooks path configuration
        val currentHooksPath = try {
            val output = ByteArrayOutputStream()
            exec {
                commandLine("git", "config", "--get", "core.hooksPath")
                standardOutput = output
                isIgnoreExitValue = true
            }
            output.toString().trim()
        } catch (e: Exception) {
            ""
        }

        when {
            currentHooksPath == "scripts/git-hooks" -> {
                println("✅ Git hooks already configured correctly")
            }
            else -> {
                // Always configure to use scripts/git-hooks directory
                exec {
                    commandLine("git", "config", "core.hooksPath", "scripts/git-hooks")
                }
                println("🔧 Configured Git to use scripts/git-hooks/ directory for hooks")
            }
        }

        // Make hook scripts executable
        val hooksDir = file("scripts/git-hooks")
        if (hooksDir.exists() && hooksDir.isDirectory) {
            hooksDir.listFiles()?.forEach { hookFile ->
                if (hookFile.isFile) {
                    hookFile.setExecutable(true)
                    println("📋 Made ${hookFile.name} executable")
                }
            }
        } else {
            println("⚠️ Hooks directory not found: scripts/git-hooks")
        }

        println("✅ Git hooks setup complete!")
    }
}

// Auto-run setup when any subproject builds
subprojects {
    tasks.matching { it.name == "build" }.configureEach {
        dependsOn(rootProject.tasks.named("setupGitHooks"))
    }
}

nexusPublishing {
    packageGroup.set("com.rudderstack")
    repositories {
        sonatype {
            username = System.getenv("NEXUS_USERNAME")
            password = System.getenv("NEXUS_PASSWORD")

            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

true // Needed to make the Suppress annotation work for the plugins block