// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.io.ByteArrayOutputStream
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.artifacts.ProjectDependency

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
    tasks.matching { it.name == "build" || it.name.startsWith("assemble") }.configureEach {
        dependsOn(rootProject.tasks.named("setupGitHooks"))
    }
}

tasks.register("listModules") {
    description = "Lists publishable modules categorised by type (android/jvm)"
    group = "help"
    doLast {
        val androidModules = mutableListOf<String>()
        val jvmModules = mutableListOf<String>()
        subprojects.forEach { sub ->
            if (sub.plugins.hasPlugin("maven-publish")) {
                if (sub.plugins.hasPlugin("com.android.library")) {
                    androidModules.add(sub.path)
                } else {
                    jvmModules.add(sub.path)
                }
            }
        }
        println("ANDROID_MODULES=${androidModules.joinToString(",")}")
        println("JVM_MODULES=${jvmModules.joinToString(",")}")
    }
}

tasks.register("printDependencyChain") {
    description = "Prints the dependency chain of all publishable modules"
    group = "help"
    doLast {
        println("name|groupId|artifactId|version|packaging|deps")
        subprojects.forEach { sub ->
            if (!sub.plugins.hasPlugin("maven-publish")) return@forEach

            val pub = sub.extensions.getByType<PublishingExtension>()
                .publications.findByName("release") as? MavenPublication
                ?: return@forEach

            val packaging = if (sub.plugins.hasPlugin("com.android.library")) "aar" else "jar"

            val deps = mutableListOf<String>()
            sub.configurations.findByName("api")?.dependencies?.forEach { dep ->
                if (dep is ProjectDependency) deps.add("${dep.dependencyProject.name}:api")
            }
            sub.configurations.findByName("implementation")?.dependencies?.forEach { dep ->
                if (dep is ProjectDependency) deps.add("${dep.dependencyProject.name}:implementation")
            }

            println("${sub.name}|${pub.groupId}|${pub.artifactId}|${pub.version}|${packaging}|${deps.joinToString(",")}")
        }
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
    useStaging.set(hasProperty("release"))
}

true // Needed to make the Suppress annotation work for the plugins block