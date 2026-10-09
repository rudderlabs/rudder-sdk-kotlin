package com.rudderstack.sdk.kotlin.core.internals.pipeline

import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.storage.inmemory.UNLIMITED_EVENTS
import com.rudderstack.sdk.kotlin.core.server.DropListener
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration

/**
 * The rules that the event pipeline follows. The SDK builds one set of rules for each instance, at start.
 *
 * The default values are the client rules: one user, no limits, and uploads that wait for the source.
 *
 * @property usesSourceConfig `true` when the SDK accepts and uploads events only while the source config says "enabled".
 * @property maxRetries The maximum number of retries for one batch. `null` means no limit.
 * @property maxQueuedEvents The maximum number of events in memory.
 * @property onSourceNotFound The action for a 404 from the data plane.
 * @property splitsBatchByAnonymousId `true` when a new anonymous ID starts a new batch.
 * @property sendsAnonymousIdHeader `true` when each upload has the `AnonymousId` header of its batch.
 * @property omitsBlankAnonymousId `true` when an empty `anonymousId` is left out of the event JSON.
 * @property continuesAfterEventFailure `true` when an exception for one event does not stop the event loop.
 * @property usesFileDispatcherForRollover `true` when the upload loop closes the open batch on the file dispatcher.
 * @property dropListener The listener that gets a report for each drop. `null` means no report.
 */
internal data class PipelineRules(
    val usesSourceConfig: Boolean,
    val maxRetries: Int? = null,
    val maxQueuedEvents: Int = UNLIMITED_EVENTS,
    val onSourceNotFound: SourceNotFoundAction = SourceNotFoundAction.STOP_UPLOADS,
    val splitsBatchByAnonymousId: Boolean = true,
    val sendsAnonymousIdHeader: Boolean = true,
    val omitsBlankAnonymousId: Boolean = false,
    val continuesAfterEventFailure: Boolean = false,
    val usesFileDispatcherForRollover: Boolean = true,
    val dropListener: DropListener? = null,
)

internal enum class SourceNotFoundAction {
    STOP_UPLOADS,
    DROP_BATCH,
}

/**
 * Builds the rules of an instance. This function is the only place that reads the platform type
 * and the configuration type to select the rules.
 */
internal fun providePipelineRules(platformType: PlatformType, configuration: Configuration): PipelineRules {
    val usesSourceConfig = platformType == PlatformType.Mobile
    if (configuration !is ServerConfiguration) {
        return PipelineRules(usesSourceConfig = usesSourceConfig)
    }
    return PipelineRules(
        usesSourceConfig = usesSourceConfig,
        maxRetries = configuration.maxRetries,
        maxQueuedEvents = configuration.maxQueuedEvents,
        onSourceNotFound = SourceNotFoundAction.DROP_BATCH,
        splitsBatchByAnonymousId = false,
        sendsAnonymousIdHeader = false,
        omitsBlankAnonymousId = true,
        continuesAfterEventFailure = true,
        usesFileDispatcherForRollover = false,
        dropListener = configuration.dropListener,
    )
}
