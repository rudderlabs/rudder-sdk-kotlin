apply(plugin = "maven-publish")
apply(plugin = "signing")

private val PLATFORM_BRAZE = "braze"

fun getExtraString(name: String) = extra[name]?.toString()

// If not release build add SNAPSHOT suffix
fun getVersionName() =
    if (hasProperty("release"))
        "1.0.0"
    else
        "1.0.0-SNAPSHOT"

fun getModuleDetails(): ModuleConfig = when (project.name) {
    PLATFORM_BRAZE -> {
        RudderStackBuildConfig.Modules.Braze
    }

    else -> {
        throw IllegalArgumentException("Unknown module: ${project.name}")
    }
}

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.rudderstack.integration.kotlin"
            artifactId = getModuleDetails().artifactId
            version = getVersionName()

            println("Publishing: Integration: $groupId:$artifactId:$version")

            // Add the `aar` or `jar` file to the artifacts
            artifact("${layout.buildDirectory.get()}/outputs/aar/${project.name}-release.aar") {
                builtBy(tasks.getByName("assemble"))
            }

            // Add sources and javadoc jars
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourcesJar"))

            // Add pom configuration
            pom {
                name.set(RudderStackBuildConfig.POM.NAME)
                packaging = getModuleDetails().pomPackaging
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

                // To include the transitive dependencies upon which the library depends
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")

                    fun addDependency(dep: Dependency, scope: String) {
                        if (dep.group == null || dep.name == "unspecified" || dep.version == null) return
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dep.group)
                        dependencyNode.appendNode("artifactId", dep.name)
                        dependencyNode.appendNode("version", dep.version)
                        dependencyNode.appendNode("scope", scope)
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
