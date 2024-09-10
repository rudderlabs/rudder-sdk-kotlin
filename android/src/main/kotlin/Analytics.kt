package com.rudderstack.android

import com.rudderstack.core.Analytics

/**
 * `Analytics` class in the `com.rudderstack.android` package.
 *
 * This class extends the `Analytics` class from the `com.rudderstack.core` package, providing
 * additional functionality for analytics tracking and configuration on Android platform.
 *
 * ## Description
 * The `Analytics` class is used to initialize analytics tracking in an Android application using
 * RudderStack. It provides methods and properties inherited from the core `Analytics` class, which
 * can be configured using the provided `Configuration` object.
 *
 * @constructor Creates an instance of the `Analytics` class.
 *
 * @param configuration An instance of the `Configuration` class that specifies the settings for
 * initializing the analytics.
 *
 * ## Example
 * ```kotlin
 * val configuration = Configuration.Builder(context, "your_write_key")
 *     .trackLifecycleEvents(true)
 *     .recordScreenViews(true)
 *     .build()
 * val analytics = Analytics(configuration)
 * ```
 *
 * @see com.rudderstack.core.Analytics
 */
class Analytics(
    configuration: Configuration,
) : Analytics(
    configuration
)
