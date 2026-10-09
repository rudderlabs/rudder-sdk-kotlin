pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // To test a local build of the SDK, run `./gradlew :core:publishToMavenLocal` and pass `-PuseMavenLocal`.
        if (providers.gradleProperty("useMavenLocal").isPresent) {
            mavenLocal()
        }
        mavenCentral()
    }
}

rootProject.name = "rudderstack-ktor-sample"
