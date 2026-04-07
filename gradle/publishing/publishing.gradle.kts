// Unified publishing script for all modules (core, android, and integrations).
// Each module applies this script via: apply(from = rootProject.file("gradle/publishing/publishing.gradle.kts"))
//
// Examples:
//   core    -> groupId: com.rudderstack.sdk.kotlin,         artifactId: core,    version: 6.0.0, packaging: jar
//   android -> groupId: com.rudderstack.sdk.kotlin,         artifactId: android, version: 6.0.0, packaging: aar
//   adjust  -> groupId: com.rudderstack.integration.kotlin, artifactId: adjust,  version: 6.0.0, packaging: aar

apply(plugin = "maven-publish")
apply(plugin = "signing")

// ── Data Model ──────────────────────────────────────────────────────────────────

data class ModuleConfig(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val pomPackaging: String,
)

// ── Module Config Resolution ────────────────────────────────────────────────────

// Returns the base (release) config for any module by name.
// e.g., "core"    -> ModuleConfig("com.rudderstack.sdk.kotlin", "core", "6.0.0", "jar")
// e.g., "android" -> ModuleConfig("com.rudderstack.sdk.kotlin", "android", "6.0.0", "aar")
// e.g., "adjust"  -> ModuleConfig("com.rudderstack.integration.kotlin", "adjust", "6.0.0", "aar")
fun getModuleConfig(moduleName: String): ModuleConfig = when (moduleName) {
    "core" -> ModuleConfig(
        groupId = RudderStackBuildConfig.SDK.PACKAGE_NAME,
        artifactId = RudderStackBuildConfig.SDK.Core.PublishConfig.artifactId,
        version = RudderStackBuildConfig.SDK.Core.VERSION_NAME,
        pomPackaging = RudderStackBuildConfig.SDK.Core.PublishConfig.pomPackaging,
    )
    "android" -> ModuleConfig(
        groupId = RudderStackBuildConfig.SDK.PACKAGE_NAME,
        artifactId = RudderStackBuildConfig.SDK.Android.PublishConfig.artifactId,
        version = RudderStackBuildConfig.SDK.Android.VERSION_NAME,
        pomPackaging = RudderStackBuildConfig.SDK.Android.PublishConfig.pomPackaging,
    )
    else -> {
        val info = RudderStackBuildConfig.Integrations.getModuleInfo(moduleName)
        ModuleConfig(
            groupId = RudderStackBuildConfig.Integrations.PACKAGE_NAME,
            artifactId = info.artifactId,
            version = info.versionName,
            pomPackaging = info.pomPackaging,
        )
    }
}

val isRelease = hasProperty("release")

// ── Snapshot Module Tracking ────────────────────────────────────────────────────

// Modules being snapshot-released together. When a dependency is in this set,
// its POM version is pinned to the exact snapshot instead of a release range.
//
// e.g., -PsnapshotModules=core,android,adjust -> setOf("core", "android", "adjust")
// e.g., (not set)                             -> emptySet()
val snapshotModules: Set<String> = findProperty("snapshotModules")
    ?.toString()
    ?.split(",")
    ?.map { it.trim() }
    ?.toSet()
    ?: emptySet()

// ── Dependency Version Resolution ───────────────────────────────────────────────

// Computes the next major version for semver range constraints.
// e.g., "6.0.0" -> "7.0.0", used to produce ranges like [6.0.0, 7.0.0)
fun computeNextMajor(version: String): String {
    val major = version.split(".").first().toInt() + 1
    return "$major.0.0"
}

// Resolves the POM version for a project dependency based on scope and snapshot state.
//
// e.g., core (api dep from android):               "6.0.0"           (exact, tight coupling)
// e.g., android (implementation dep from adjust):   "[6.0.0, 7.0.0)" (range, loose coupling)
// e.g., android (snapshot, -PsnapshotModules=...):  "6.0.0-SNAPSHOT" (pinned)
fun resolveDepVersion(depName: String, isApiDep: Boolean): String {
    val version = getModuleConfig(depName).version
    return when {
        depName in snapshotModules -> "${version}-SNAPSHOT"
        isApiDep -> version
        else -> "[${version}, ${computeNextMajor(version)})"
    }
}

// ── Publication ─────────────────────────────────────────────────────────────────

// Snapshot suffix applied here — the only place build mode affects the version.
val moduleConfig = getModuleConfig(project.name)
val publishVersion = if (isRelease) moduleConfig.version else "${moduleConfig.version}-SNAPSHOT"

version = publishVersion

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("release") {
            val config = moduleConfig.copy(version = publishVersion)

            groupId = config.groupId
            artifactId = config.artifactId
            version = config.version

            // Artifact (jar for core, aar for android/integrations)
            if (config.pomPackaging == "jar") {
                artifact("${layout.buildDirectory.get()}/libs/${project.name}-${version}.jar") {
                    builtBy(tasks.getByName("assemble"))
                }
            } else {
                artifact("${layout.buildDirectory.get()}/outputs/aar/${project.name}-release.aar") {
                    builtBy(tasks.getByName("assemble"))
                }
            }

            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourcesJar"))

            // POM metadata
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

                // POM dependencies — manually built to handle project dependencies correctly.
                // ProjectDependency instances are resolved via resolveProjectDependency(),
                // while external dependencies are written as-is.
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")

                    fun addDependency(dep: org.gradle.api.artifacts.Dependency, scope: String) {
                        if (dep.name == "unspecified") return

                        if (dep is org.gradle.api.artifacts.ProjectDependency) {
                            val depConfig = getModuleConfig(dep.dependencyProject.name)
                            val depVersion = resolveDepVersion(dep.dependencyProject.name, isApiDep = scope == "compile")
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", depConfig.groupId)
                            dependencyNode.appendNode("artifactId", depConfig.artifactId)
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

                    val addedDeps = mutableSetOf<String>()

                    fun addDependencyIfNew(dep: org.gradle.api.artifacts.Dependency, scope: String) {
                        val key = "${dep.group}:${dep.name}"
                        if (key in addedDeps) return
                        addedDeps.add(key)
                        addDependency(dep, scope)
                    }

                    // 1. Declared api dependencies (scope: compile)
                    configurations.findByName("api")?.dependencies?.forEach { dep ->
                        addDependencyIfNew(dep, "compile")
                    }

                    // 2. Kotlin plugin dependencies (scope: compile)
                    //    The Kotlin plugin adds kotlin-stdlib to apiDependenciesMetadata
                    //    rather than api directly. We resolve this configuration to pick
                    //    up those transitive compile dependencies.
                    configurations.findByName("apiDependenciesMetadata")
                        ?.resolvedConfiguration
                        ?.firstLevelModuleDependencies
                        ?.forEach { resolved ->
                            val key = "${resolved.moduleGroup}:${resolved.moduleName}"
                            if (key !in addedDeps) {
                                addedDeps.add(key)
                                val dependencyNode = dependenciesNode.appendNode("dependency")
                                dependencyNode.appendNode("groupId", resolved.moduleGroup)
                                dependencyNode.appendNode("artifactId", resolved.moduleName)
                                dependencyNode.appendNode("version", resolved.moduleVersion)
                                dependencyNode.appendNode("scope", "compile")
                            }
                        }

                    // 3. Declared implementation dependencies (scope: runtime)
                    configurations.findByName("implementation")?.dependencies?.forEach { dep ->
                        addDependencyIfNew(dep, "runtime")
                    }
                }
            }
        }
    }
}

// ── Signing ─────────────────────────────────────────────────────────────────────

configure<SigningExtension> {
    val signingKeyId = System.getenv("SIGNING_KEY_ID")
    val signingKey = System.getenv("SIGNING_PRIVATE_KEY_BASE64")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}

// ── Task Dependencies ───────────────────────────────────────────────────────────

tasks.getByName("publish") { dependsOn("build") }
tasks.getByName("publishToMavenLocal") { dependsOn("build") }
tasks.getByName("publishToSonatype") { dependsOn("publish") }

// ── POM Verification ────────────────────────────────────────────────────────────

// Prints the generated POM XML to stdout for CI dry-runs.
// e.g., ./gradlew :core:printPom -Prelease
// e.g., ./gradlew :integrations:adjust:printPom -PsnapshotModules=core,android
tasks.register("printPom") {
    dependsOn("generatePomFileForReleasePublication")
    doLast {
        val pomFile = layout.buildDirectory.file("publications/release/pom-default.xml").get().asFile
        println(pomFile.readText())
    }
}
