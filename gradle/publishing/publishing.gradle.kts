apply(plugin = "maven-publish")
apply(plugin = "signing")

private val PLATFORM_ANDROID = "android"

fun getExtraString(name: String) = extra[name]?.toString()

// If not release build add SNAPSHOT suffix
fun getVersionName() =
    if (hasProperty("release"))
        RudderStackBuildConfig.Version.VERSION_NAME
    else
        RudderStackBuildConfig.Version.VERSION_NAME + "-SNAPSHOT"

fun getModuleDetails(): ModuleConfig =
    if (project.name == "android") {
        RudderStackBuildConfig.Modules.Android
    } else {
        RudderStackBuildConfig.Modules.Core
    }

configure<PublishingExtension> {
    publications {
        register<MavenPublication>("release") {
            groupId = RudderStackBuildConfig.PackageName.PACKAGE_NAME
            artifactId = getModuleDetails().artifactId
            version = getVersionName()

            println("RudderStack: Publishing following library to Maven Central: $groupId:$artifactId:$version")

            // Add the `aar` or `jar` file to the artifacts
            if (project.name == "android") {
                artifact("$buildDir/outputs/aar/${project.name}-release.aar") {
                    builtBy(tasks.getByName("assemble"))
                }
            } else {
                artifact("$buildDir/libs/${project.name}-${version}.jar") {
                    builtBy(tasks.getByName("assemble"))
                }
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
plugins.withType<SigningPlugin>().configureEach {
    extensions.configure<SigningExtension> {
    }
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
