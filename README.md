<p align="center">
  <a href="https://rudderstack.com/">
    <img alt="RudderStack" width="512" src="https://raw.githubusercontent.com/rudderlabs/rudder-sdk-js/develop/assets/rs-logo-full-light.jpg">
  </a>
  <br />
  <caption>The Customer Data Platform for Developers</caption>
</p>
<p align="center">
  <b>
    <a href="https://rudderstack.com">Website</a>
    ·
    <a href="https://rudderstack.com/docs/stream-sources/rudderstack-sdk-integration-guides/rudderstack-javascript-sdk/">Documentation</a>
    ·
    <a href="https://rudderstack.com/join-rudderstack-slack-community">Community Slack</a>
  </b>
</p>

---

# RudderStack Kotlin SDK

The Kotlin SDK enables you to track customer event data from your Android or Kotlin JVM applications and send it to your configured destinations via RudderStack.

---

## Table of Contents

- [**Installing the Kotlin Android SDK**](#installing-the-kotlin-android-sdk)
- [**Installing the Kotlin JVM SDK**](#installing-the-kotlin-jvm-sdk)
- [**Initializing the SDK**](#initializing-the-sdk)
- [**Identifying your users**](#identifying-users)
- [**Tracking user actions**](#tracking-user-actions)
- [**Integrations**](#integrations)
- [**Development**](#development)
- [**Contact us**](#contact-us)

---

## Installing the Kotlin Android SDK

Add the SDK to your Android project using Gradle:

```kotlin
dependencies {
    implementation("com.rudderstack.sdk.kotlin:android:<latest_version>")
}

```

Replace `<latest_version>` with the version number you want to use. You can find the latest release [**here**](https://github.com/rudderlabs/rudder-sdk-kotlin/releases).

## Installing the Kotlin JVM SDK

Add the SDK to your Kotlin JVM project using Gradle:

```kotlin
dependencies {
    implementation("com.rudderstack.sdk.kotlin:core:<latest_version>")
}

```

Replace `<latest_version>` with the version number you want to use. You can find the latest release [**here**](https://github.com/rudderlabs/rudder-sdk-kotlin/releases).

---

## Initializing the SDK

To initialize the Android RudderStack SDK, add the Analytics initialisation snippet to your application’s entry point (e.g., in `onCreate` method):

```kotlin
import android.app.Application
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration

class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()
        initializeAnalytics(this)
    }

    private fun initializeAnalytics(application: Application) {
        analytics = Analytics(
            configuration = Configuration(
                writeKey = "<WRITE_KEY>",
                application = application,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )
    }
}
```

Replace:

- `<WRITE_KEY>`: Your project’s write key.
- `<DATA_PLANE_URL>`: The URL of your RudderStack data plane.

---

## Identifying users

The `identify` API lets you recognize a user and associate them with their traits:

```kotlin
analytics.identify(
	userId = "1hKOmRA4el9Zt1WSfVJIVo4GRlm",
	traits = buildJsonObject {
		put("name", "Alex Keener")
		put("email", "alex@example.com")
	}
)
```

---

## Tracking user actions

The `track` API lets you capture user events:

```kotlin
analytics.track(
    event = "Order Completed",
    properties = buildJsonObject {
	put("revenue", 30)
        put("currency", "USD")
    }
)
```

## Integrations

RudderStack Kotlin SDK supports various third-party integrations that allow you to send your event data to external analytics and marketing platforms. These integrations are implemented as separate modules that you can include in your project as needed.

### Available Integrations

The following integrations are currently available:

- [Firebase](integrations/firebase/README.md) - Send your event data to Google Firebase Analytics
- [Adjust](integrations/adjust/README.md) - Track and attribute your mobile app installs and in-app events
- [Braze](integrations/braze/README.md) - Send your event data to Braze for customer engagement
- [Facebook](integrations/facebook/README.md) - Send your event data to Facebook for analytics and advertising

### Using Integrations

To use an integration, follow these steps:

1. Add the integration dependency to your project's `build.gradle.kts` file
2. Initialize the RudderStack SDK as usual
3. Add the integration to your Analytics instance

Example with multiple integrations:

```kotlin
class MyApplication : Application() {

    lateinit var analytics: Analytics

    override fun onCreate() {
        super.onCreate()

        // Initialize RudderStack SDK
        analytics = Analytics(
            configuration = Configuration(
                writeKey = "<WRITE_KEY>",
                application = this,
                dataPlaneUrl = "<DATA_PLANE_URL>",
            )
        )

        // Add integrations
        analytics.add(FirebaseIntegration())
        analytics.add(BrazeIntegration())
        // Add more integrations as needed
    }
}
```

---

## Development

This section provides information for developers contributing to the RudderStack Kotlin SDK.

### Git Hooks

The project includes automated git hooks to maintain code quality and enforce development standards. These hooks run automatically during git operations to catch issues early.

#### Available Hooks

- **pre-commit**: Runs before each commit
  - Executes Detekt static code analysis
  - Runs Android Lint on affected modules
  - Prevents commits if quality checks fail

- **pre-push**: Runs before each push
  - Validates branch naming conventions (e.g., `feat/feature-name`, `fix/bug-name`)
  - Runs a clean build to ensure code compiles
  - Prevents pushes if validation fails

- **commit-msg**: Runs when creating commit messages
  - Validates commit message format using conventional commits
  - Enforces format: `type(scope): description` (e.g., `feat: add new analytics feature`)
  - Supported types: `feat`, `fix`, `refactor`, `perf`, `style`, `test`, `docs`, `chore`, `build`, `ci`, `revert`

#### Enabling Git Hooks

The git hooks are automatically configured when you build the project, but you can also enable them manually:

```bash
# Option 1: Use the Gradle task (recommended)
./gradlew setupGitHooks

# Option 2: Manual setup
git config core.hooksPath scripts
chmod +x scripts/pre-commit scripts/pre-push scripts/commit-msg
```

#### Branch Naming Convention

When creating branches, follow this naming pattern:
```
<type>/<description>

Examples:
feat/user-authentication
fix/memory-leak-issue
refactor/analytics-core
docs/update-readme
```

#### Commit Message Convention

Follow conventional commit format:
```
<type>(<scope>): <description>

Examples:
feat(android): add user identification support
fix(core): resolve memory leak in event processing
docs: update installation instructions
```

---

## Contact us

For more information:

- Email us at [docs@rudderstack.com](mailto:docs@rudderstack.com)
- Join our [**Community Slack**](https://rudderstack.com/join-rudderstack-slack-community)

---

## Follow Us

- [RudderStack Blog](https://rudderstack.com/blog/)
- [Slack](https://rudderstack.com/join-rudderstack-slack-community)
- [Twitter](https://twitter.com/rudderstack)
- [YouTube](https://www.youtube.com/channel/UCgV-B77bV_-LOmKYHw8jvBw)

---

<!----variables---->

[rudderstack-blog]: https://rudderstack.com/blog/
[slack]: https://resources.rudderstack.com/join-rudderstack-slack
[twitter]: https://twitter.com/rudderstack
[linkedin]: https://www.linkedin.com/company/rudderlabs/
[devto]: https://dev.to/rudderstack
[medium]: https://rudderstack.medium.com/
[youtube]: https://www.youtube.com/channel/UCgV-B77bV_-LOmKYHw8jvBw
[hackernews]: https://news.ycombinator.com/item?id=21081756
[producthunt]: https://www.producthunt.com/posts/rudderstack
