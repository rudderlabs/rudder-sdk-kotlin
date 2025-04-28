@file:Suppress("MatchingDeclarationName", "Filename")

package com.rudderstack.sdk.kotlin.core.internals.utils

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is intended for internal use within RudderStack modules only. " +
        "It does not provide compatibility guarantees and is subject to change without notice. " +
        "Using this API outside RudderStack modules may cause issues in behaviour of this library."
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
 * import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
 *
 * @InternalRudderApi
 * fun internalFunction() {
 *     // Implementation of an internal function
 *     ...
 * }
 * ```
 */
annotation class InternalRudderApi

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This method should be used with caution. Read the API documentation for more details."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER
)
/**
 * This annotation should be applied to methods, classes, or properties that require careful usage
 * due to potential risks or limited guarantees. APIs annotated with this indicate that additional
 * attention or understanding is required before use, as misuse may lead to unintended consequences.
 *
 * Example usage:
 * ```
 * import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
 *
 * @UseWithCaution
 * fun riskyFunction() {
 *     // Implementation of a cautious function
 *     ...
 * }
 * ```
 */
annotation class UseWithCaution
