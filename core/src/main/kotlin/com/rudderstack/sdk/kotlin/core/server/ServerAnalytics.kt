package com.rudderstack.sdk.kotlin.core.server

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Properties
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.addNameAndCategoryToProperties
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.server.ServerAnalytics.Companion.DEFAULT_TIMEOUT_IN_MILLIS
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The entry class of the SDK for server-side use (public beta).
 *
 * The class stores no user. Each event call takes the `userId`, the `anonymousId`, or both.
 * One instance is safe for all request threads. Create one instance for each write key and keep it for the life of the process.
 *
 * Call [shutdown] or [shutdownBlocking] before the process exits. In-memory storage loses each unsent event at exit.
 */
class ServerAnalytics internal constructor(private val analytics: Analytics) {

    /**
     * Creates the instance and starts the SDK.
     *
     * @param configuration The server configuration.
     */
    constructor(configuration: ServerConfiguration) : this(Analytics(configuration))

    private val isShutdown = AtomicBoolean(false)

    /**
     * Records an action of a user.
     *
     * The SDK drops the event and reports [DropReason.MISSING_IDENTITY] when [userId] and [anonymousId] are both blank.
     *
     * @param name The name of the event.
     * @param userId The ID of the known user. Defaults to an empty string.
     * @param anonymousId The ID of the anonymous user. Defaults to an empty string.
     * @param properties The properties of the event. Defaults to an empty JSON object.
     * @param options The options of the event. Defaults to an empty [RudderOption].
     */
    @JvmOverloads
    fun track(
        name: String,
        userId: String = String.empty(),
        anonymousId: String = String.empty(),
        properties: Properties = emptyJsonObject,
        options: RudderOption = RudderOption(),
    ) {
        val identity = provideIdentity(userId, anonymousId) ?: return

        analytics.enqueue(
            TrackEvent(event = name, properties = properties, options = options, userIdentityState = identity)
        )
    }

    /**
     * Records a screen view of a user.
     *
     * The SDK drops the event and reports [DropReason.MISSING_IDENTITY] when [userId] and [anonymousId] are both blank.
     *
     * @param screenName The name of the screen.
     * @param userId The ID of the known user. Defaults to an empty string.
     * @param anonymousId The ID of the anonymous user. Defaults to an empty string.
     * @param category The category of the screen. Defaults to an empty string.
     * @param properties The properties of the event. Defaults to an empty JSON object.
     * @param options The options of the event. Defaults to an empty [RudderOption].
     */
    @JvmOverloads
    fun screen(
        screenName: String,
        userId: String = String.empty(),
        anonymousId: String = String.empty(),
        category: String = String.empty(),
        properties: Properties = emptyJsonObject,
        options: RudderOption = RudderOption(),
    ) {
        val identity = provideIdentity(userId, anonymousId) ?: return

        analytics.enqueue(
            ScreenEvent(
                screenName = screenName,
                properties = addNameAndCategoryToProperties(screenName, category, properties),
                options = options,
                userIdentityState = identity,
            )
        )
    }

    /**
     * Adds a user to a group.
     *
     * The SDK drops the event and reports [DropReason.MISSING_IDENTITY] when [userId] and [anonymousId] are both blank.
     *
     * @param groupId The ID of the group.
     * @param userId The ID of the known user. Defaults to an empty string.
     * @param anonymousId The ID of the anonymous user. Defaults to an empty string.
     * @param traits The traits of the group. Defaults to an empty JSON object.
     * @param options The options of the event. Defaults to an empty [RudderOption].
     */
    @JvmOverloads
    fun group(
        groupId: String,
        userId: String = String.empty(),
        anonymousId: String = String.empty(),
        traits: Traits = emptyJsonObject,
        options: RudderOption = RudderOption(),
    ) {
        val identity = provideIdentity(userId, anonymousId) ?: return

        analytics.enqueue(
            GroupEvent(groupId = groupId, traits = traits, options = options, userIdentityState = identity)
        )
    }

    /**
     * Identifies a user and records the traits of that user.
     *
     * The SDK sends [traits] in `context.traits` of this event only. A later event call carries no traits.
     * The SDK drops the event and reports [DropReason.MISSING_IDENTITY] when [userId] and [anonymousId] are both blank.
     *
     * @param userId The ID of the known user. Defaults to an empty string.
     * @param anonymousId The ID of the anonymous user. Defaults to an empty string.
     * @param traits The traits of the user. Defaults to an empty JSON object.
     * @param options The options of the event. Defaults to an empty [RudderOption].
     */
    @JvmOverloads
    fun identify(
        userId: String = String.empty(),
        anonymousId: String = String.empty(),
        traits: Traits = emptyJsonObject,
        options: RudderOption = RudderOption(),
    ) {
        val identity = provideIdentity(userId, anonymousId, traits) ?: return

        analytics.enqueue(IdentifyEvent(options = options, userIdentityState = identity))
    }

    /**
     * Merges two identities of one user.
     *
     * The SDK drops the event and reports [DropReason.MISSING_IDENTITY] when [userId] or [previousId] is blank.
     *
     * @param userId The new ID of the user.
     * @param previousId The previous ID of the user.
     * @param options The options of the event. Defaults to an empty [RudderOption].
     */
    @JvmOverloads
    fun alias(userId: String, previousId: String, options: RudderOption = RudderOption()) {
        val identity = provideIdentity(
            userId = userId,
            isIdentified = userId.isNotBlank() && previousId.isNotBlank(),
        ) ?: return

        analytics.enqueue(AliasEvent(previousId = previousId, options = options, userIdentityState = identity))
    }

    /**
     * Sends a flush signal. This call does not wait for the upload.
     */
    fun flush() {
        analytics.flush()
    }

    /**
     * Uploads each event queued before this call and waits for the result.
     *
     * @param timeout The maximum time to wait. Defaults to [DEFAULT_TIMEOUT_IN_MILLIS].
     * @return `true` when the SDK sent each batch. `false` on a timeout, after a shutdown, or when the SDK dropped
     * a queued event during the call.
     */
    suspend fun flushAndWait(timeout: Duration = DEFAULT_TIMEOUT_IN_MILLIS.milliseconds): Boolean {
        if (isShutdown.get()) return false

        return drain(timeout)
    }

    /**
     * The blocking form of [flushAndWait]. Do not call this method from a coroutine.
     *
     * @param timeoutMillis The maximum time to wait, in milliseconds. Defaults to [DEFAULT_TIMEOUT_IN_MILLIS].
     * @return `true` when the SDK sent each batch. `false` on a timeout, after a shutdown, or when the SDK dropped
     * a queued event during the call.
     */
    @JvmOverloads
    fun flushBlocking(timeoutMillis: Long = DEFAULT_TIMEOUT_IN_MILLIS): Boolean = runBlocking {
        flushAndWait(timeoutMillis.milliseconds)
    }

    /**
     * Rejects each new event, uploads the queued events, and then shuts the SDK down.
     *
     * An event call that runs at the same time as this call can lose its event.
     *
     * @param timeout The maximum time to wait for the upload. Defaults to [DEFAULT_TIMEOUT_IN_MILLIS].
     * @return `true` when the SDK sent each batch. `false` on a timeout, on a second call, or when the SDK dropped
     * a queued event during the call.
     */
    suspend fun shutdown(timeout: Duration = DEFAULT_TIMEOUT_IN_MILLIS.milliseconds): Boolean {
        if (!isShutdown.compareAndSet(false, true)) return false

        val isDrained = drain(timeout)
        if (!isDrained) {
            analytics.logger.warn(
                "ServerAnalytics: Shutdown did not send each event. " +
                    "Batches left in the queue: ${analytics.storage.readFileList().size} or more"
            )
        }
        analytics.shutdown()
        return isDrained
    }

    /**
     * The blocking form of [shutdown]. Do not call this method from a coroutine.
     *
     * @param timeoutMillis The maximum time to wait for the upload, in milliseconds. Defaults to [DEFAULT_TIMEOUT_IN_MILLIS].
     * @return `true` when the SDK sent each batch. `false` on a timeout, on a second call, or when the SDK dropped
     * a queued event during the call.
     */
    @JvmOverloads
    fun shutdownBlocking(timeoutMillis: Long = DEFAULT_TIMEOUT_IN_MILLIS): Boolean = runBlocking {
        shutdown(timeoutMillis.milliseconds)
    }

    /**
     * Adds a plugin to the plugin chain.
     *
     * @param plugin The plugin to add.
     */
    fun add(plugin: Plugin) {
        analytics.add(plugin)
    }

    /**
     * Removes a plugin from the plugin chain.
     *
     * @param plugin The plugin to remove.
     */
    fun remove(plugin: Plugin) {
        analytics.remove(plugin)
    }

    private suspend fun drain(timeout: Duration): Boolean {
        return withTimeoutOrNull(timeout) { analytics.drain() } ?: false
    }

    private fun provideIdentity(
        userId: String,
        anonymousId: String = String.empty(),
        traits: Traits = emptyJsonObject,
        isIdentified: Boolean = userId.isNotBlank() || anonymousId.isNotBlank(),
    ): UserIdentity? {
        if (isShutdown.get()) {
            analytics.logger.warn("ServerAnalytics: The instance is shut down. The SDK dropped the event")
            return null
        }
        if (!isIdentified) {
            analytics.logger.error("ServerAnalytics: The event call has no user identity. The SDK dropped the event")
            analytics.reportDrop(DropReason.MISSING_IDENTITY, eventCount = 1)
            return null
        }
        return UserIdentity(
            anonymousId = anonymousId.ifBlank { String.empty() },
            userId = userId.ifBlank { String.empty() },
            traits = traits,
        )
    }

    companion object {

        /**
         * The default time, in milliseconds, that a flush or a shutdown waits for the upload.
         */
        const val DEFAULT_TIMEOUT_IN_MILLIS: Long = 10_000L
    }
}
