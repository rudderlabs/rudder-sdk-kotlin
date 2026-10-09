# Server-side samples (public beta)

Each sample is a standalone Gradle build. It uses the published `com.rudderstack.sdk.kotlin:core` artifact.

| Sample | Shows |
| --- | --- |
| `ktor` | One `ServerAnalytics` instance in the application module, a `track` call in a route, and `shutdownBlocking` on `ApplicationStopped`. |
| `spring-boot` | One `ServerAnalytics` bean, a `track` call in a controller, and `shutdownBlocking` as the destroy method of the bean. |

## Run a sample

Run these commands from the root of the repository. Use `spring-boot` and `bootRun` for the Spring Boot sample.

```bash
export RUDDERSTACK_WRITE_KEY=<WRITE_KEY>
export RUDDERSTACK_DATA_PLANE_URL=<DATA_PLANE_URL>
./gradlew -p samples/ktor run
```

Send an event for a user:

```bash
curl -X POST "http://localhost:8080/orders?userId=user-1"
```

Stop the sample with `Ctrl+C`. The SDK sends each queued event before the process exits.

## Use a local build of the SDK

```bash
./gradlew :core:publishToMavenLocal
./gradlew -p samples/ktor run -PuseMavenLocal -PrudderStackCoreVersion=<LOCAL_VERSION>
```

`<LOCAL_VERSION>` is the version in `~/.m2/repository/com/rudderstack/sdk/kotlin/core`, for example `1.8.0-SNAPSHOT`.
