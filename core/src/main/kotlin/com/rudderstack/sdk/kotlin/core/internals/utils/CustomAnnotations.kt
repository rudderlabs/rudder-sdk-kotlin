package com.rudderstack.sdk.kotlin.core.internals.utils

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is an internal API of RudderStack, intended exclusively for use within its modules. " +
        "No compatibility guarantees are provided for this feature. " +
        "Using this feature outside RudderStack modules may result in unexpected behavior."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER
)
/**
 * This annotation should be applied to internal APIs within the RudderStack SDK that are intended
 * **exclusively** for use within the SDK modules. APIs annotated with this indicate that the
 * feature is **for internal use only**.
 * Using these features outside the SDK can result in **unexpected behavior**.
 *
 * Example usage:
 * ```
 * import com.rudderstack.sdk.kotlin.core.internals.utils.RudderInternalUsageWarning
 *
 * @RudderInternalUsageWarning
 * fun internalFunction() {
 *     // Implementation of an internal function
 *     ...
 * }
 * ```
 */
annotation class RudderInternalUsageWarning

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal API of RudderStack, intended exclusively for use within its modules. " +
        "It does not come with compatibility guarantees and may change without notice. " +
        "Using this feature outside RudderStack modules can result in significant issues."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER
)
/**
 * This annotation should be applied to internal APIs within the RudderStack SDK that are intended
 * **exclusively** for use within the SDK modules. APIs annotated with this indicate that the
 * feature is **for internal use only**.
 * Using these features outside the SDK can result in **significant issues**.
 *
 * Example usage:
 * ```
 * import com.rudderstack.sdk.kotlin.core.internals.utils.RudderInternalUsageError
 *
 * @RudderInternalUsageError
 * fun internalFunction() {
 *     // Implementation of an internal function
 *     ...
 * }
 * ```
 */
annotation class RudderInternalUsageError
