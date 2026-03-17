// Unified publishing script for all modules (core, android, and integrations).
// Each module applies this script via: apply(from = rootProject.file("gradle/publishing/publishing.gradle.kts"))
// Module type is detected via project.name to resolve the correct groupId, artifactId, and version.

apply(plugin = "maven-publish")
apply(plugin = "signing")

// e.g., for android module: ModuleConfig("com.rudderstack.sdk.kotlin", "android", "1.3.0", "aar")
data class ModuleConfig(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val pomPackaging: String,
)

// Computes the next major version for semver range constraints in POM dependencies.
// e.g., "1.3.0" -> "2.0.0", used to produce ranges like [1.3.0, 2.0.0)
fun computeNextMajor(version: String): String {
    val major = version.split(".").first().toInt() + 1
    return "$major.0.0"
}

// Resolves publishing metadata (groupId, artifactId, version, packaging) based on the module applying this script.
// Appends "-SNAPSHOT" suffix for non-release builds.
// e.g., core module    -> ModuleConfig("com.rudderstack.sdk.kotlin", "core", "1.3.0-SNAPSHOT", "jar")
// e.g., android module -> ModuleConfig("com.rudderstack.sdk.kotlin", "android", "1.3.0-SNAPSHOT", "aar")
// e.g., adjust module  -> ModuleConfig("com.rudderstack.integration.kotlin", "adjust", "1.2.0-SNAPSHOT", "aar")
fun getModuleConfig(): ModuleConfig {
    val isRelease = hasProperty("release")
    return when (project.name) {
        "core" -> {
            val version = RudderStackBuildConfig.SDK.Core.VERSION_NAME
            ModuleConfig(
                groupId = RudderStackBuildConfig.SDK.PACKAGE_NAME,
                artifactId = RudderStackBuildConfig.SDK.Core.PublishConfig.artifactId,
                version = if (isRelease) version else "$version-SNAPSHOT",
                pomPackaging = RudderStackBuildConfig.SDK.Core.PublishConfig.pomPackaging,
            )
        }
        "android" -> {
            val version = RudderStackBuildConfig.SDK.Android.VERSION_NAME
            ModuleConfig(
                groupId = RudderStackBuildConfig.SDK.PACKAGE_NAME,
                artifactId = RudderStackBuildConfig.SDK.Android.PublishConfig.artifactId,
                version = if (isRelease) version else "$version-SNAPSHOT",
                pomPackaging = RudderStackBuildConfig.SDK.Android.PublishConfig.pomPackaging,
            )
        }
        else -> {

            // integrations
            val info = RudderStackBuildConfig.Integrations.getModuleInfo(project.name)
            ModuleConfig(
                groupId = RudderStackBuildConfig.Integrations.PACKAGE_NAME,
                artifactId = info.artifactId,
                version = if (isRelease) info.versionName else "${info.versionName}-SNAPSHOT",
                pomPackaging = info.pomPackaging,
            )
        }
    }
}

// Maps project dependencies (e.g., project(":core"), project(":android")) to Maven coordinates for the POM.
// Release builds:
// - Core dependency: pinned to exact version (android module tightly couples with core)
// - Android dependency: semver range [current, nextMajor) so integrations stay compatible across minor bumps
// Snapshot builds:
// - Both core and android dependencies: pinned to exact snapshot version
// e.g., release: android's project(":core") -> ("com.rudderstack.sdk.kotlin", "core", "1.3.0")
// e.g., release: adjust's project(":android") -> ("com.rudderstack.sdk.kotlin", "android", "[1.3.0, 2.0.0)")
// e.g., snapshot: android's project(":core") -> ("com.rudderstack.sdk.kotlin", "core", "1.3.0-SNAPSHOT")
// e.g., snapshot: adjust's project(":android") -> ("com.rudderstack.sdk.kotlin", "android", "1.3.0-SNAPSHOT")
fun resolveProjectDependency(dep: org.gradle.api.artifacts.ProjectDependency): Triple<String, String, String> {
    val isRelease = hasProperty("release")
    return when (dep.dependencyProject.name) {
        "core" -> {
            val version = RudderStackBuildConfig.SDK.Core.VERSION_NAME
            Triple(
                RudderStackBuildConfig.SDK.PACKAGE_NAME,
                RudderStackBuildConfig.SDK.Core.PublishConfig.artifactId,
                if (isRelease) version else "$version-SNAPSHOT",
            )
        }
        "android" -> {
            val version = RudderStackBuildConfig.SDK.Android.VERSION_NAME
            Triple(
                RudderStackBuildConfig.SDK.PACKAGE_NAME,
                RudderStackBuildConfig.SDK.Android.PublishConfig.artifactId,
                if (isRelease) "[$version, ${computeNextMajor(version)})" else "$version-SNAPSHOT",
            )
        }
        else -> throw IllegalArgumentException("Unexpected project dependency: ${dep.dependencyProject.name}")
    }
}

// Set project.version so jar/aar filenames match the module's own version
version = getModuleConfig().version

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("release") {
            val config = getModuleConfig()

            groupId = config.groupId
            artifactId = config.artifactId
            version = config.version

            // Add the artifact (jar for core, aar for android/integrations)
            if (config.pomPackaging == "jar") {
                artifact("${layout.buildDirectory.get()}/libs/${project.name}-${version}.jar") {
                    builtBy(tasks.getByName("assemble"))
                }
            } else {
                artifact("${layout.buildDirectory.get()}/outputs/aar/${project.name}-release.aar") {
                    builtBy(tasks.getByName("assemble"))
                }
            }

            // Add sources and javadoc jars
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourcesJar"))

            // Add pom configuration
            pom {
                name.set(RudderStackBuildConfig.POM.NAME)
                packaging = config.pomPackaging
                description.set(RudderStackBuildConfig.POM.DESCRIPTION)
                url.set(RudderStackBuildConfig.POM.URL)

                licenses {
                    license {
                        name.set(RudderStackBuildConfig.POM.LICENCE_NAME)
                        url.set(RudderStackBuildConfig.POM.LICENCE_URL)
                        distribution.set(RudderStackBuildConfig.POM.LICENCE_DIST)
                    }
                }

                developers {
                    developer {
                        id.set(RudderStackBuildConfig.POM.DEVELOPER_ID)
                        name.set(RudderStackBuildConfig.POM.DEVELOPER_NAME)
                    }
                }

                scm {
                    url.set(RudderStackBuildConfig.POM.SCM_URL)
                    connection.set(RudderStackBuildConfig.POM.SCM_CONNECTION)
                    developerConnection.set(RudderStackBuildConfig.POM.SCM_DEV_CONNECTION)
                }

                // Manually build POM dependencies to handle project dependencies correctly.
                // ProjectDependency instances are resolved to Maven coordinates via resolveProjectDependency(),
                // while external dependencies are written as-is.
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")

                    fun addDependency(dep: org.gradle.api.artifacts.Dependency, scope: String) {
                        if (dep.name == "unspecified") return

                        if (dep is org.gradle.api.artifacts.ProjectDependency) {
                            val (depGroupId, depArtifactId, depVersion) = resolveProjectDependency(dep)
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", depGroupId)
                            dependencyNode.appendNode("artifactId", depArtifactId)
                            dependencyNode.appendNode("version", depVersion)
                            dependencyNode.appendNode("scope", scope)
                        } else {
                            if (dep.group == null || dep.version == null) return
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", dep.group)
                            dependencyNode.appendNode("artifactId", dep.name)
                            dependencyNode.appendNode("version", dep.version)
                            dependencyNode.appendNode("scope", scope)
                        }
                    }

                    configurations.findByName("api")?.dependencies?.forEach { dep ->
                        addDependency(dep, "compile")
                    }

                    configurations.findByName("implementation")?.dependencies?.forEach { dep ->
                        addDependency(dep, "runtime")
                    }
                }
            }
        }
    }
}

// Signing configuration
configure<SigningExtension> {
    val signingKeyId = System.getenv("SIGNING_KEY_ID")
    val signingKey = System.getenv("SIGNING_PRIVATE_KEY_BASE64")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}

tasks.getByName("publish") {
    dependsOn("build")
}

tasks.getByName("publishToMavenLocal") {
    dependsOn("build")
}

tasks.getByName("publishToSonatype") {
    dependsOn("publish")
}

