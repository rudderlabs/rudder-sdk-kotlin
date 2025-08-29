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
    description = "Configure Git to use hooks from scripts directory"
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
            currentHooksPath == "scripts" -> {
                println("✅ Git hooks already configured correctly")
            }
            currentHooksPath.isEmpty() -> {
                // Configure Git to use scripts directory for hooks
                exec {
                    commandLine("git", "config", "core.hooksPath", "scripts")
                }
                println("🔧 Configured Git to use scripts/ directory for hooks")
            }
            else -> {
                println("⚠️ Git hooks path is currently set to: $currentHooksPath")
                println("💡 To use project hooks, run: git config core.hooksPath scripts")
            }
        }

        // Make hook scripts executable
        val hookFiles = listOf("pre-commit", "pre-push", "commit-msg")
        hookFiles.forEach { hookName ->
            val hookFile = file("scripts/$hookName")
            if (hookFile.exists()) {
                hookFile.setExecutable(true)
                println("📋 Made $hookName executable")
            } else {
                println("⚠️ Hook file not found: scripts/$hookName")
            }
        }

        // Clean up old copied hooks if they exist
        val oldHooksDir = file(".git/hooks")
        hookFiles.forEach { hookName ->
            val oldHookFile = File(oldHooksDir, hookName)
            if (oldHookFile.exists() && !oldHookFile.readText().contains("#!/bin/bash")) {
                // Only delete if it looks like our copied hook (not a custom hook)
                val scriptContent = file("scripts/$hookName").readText()
                if (oldHookFile.readText() == scriptContent) {
                    oldHookFile.delete()
                    println("🧹 Removed old copied hook: .git/hooks/$hookName")
                }
            }
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