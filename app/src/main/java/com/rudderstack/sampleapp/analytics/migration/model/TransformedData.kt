package com.rudderstack.sampleapp.analytics.migration.model

/**
 * Represents data transformed from legacy format to new SDK format, ready for writing.
 *
 * This data class holds values after transformation has been applied, with type conversions
 * and format changes necessary for the new `rudder-sdk-kotlin` storage schema.
 *
 * ## Key Transformations
 * - **userId extraction**: Extracted from legacy traits JSON and stored separately
 * - **Traits cleaning**: Legacy traits JSON has userId/anonymousId removed
 * - **Type conversion**: App build converted from Int (legacy) to Long (new)
 * - **Key mapping**: Legacy keys mapped to new SDK keys (see [MigratableValue] documentation)
 *
 * ## New SDK Storage Keys
 * - Anonymous ID: `anonymous_id`
 * - User ID: `user_id` (extracted from traits)
 * - Traits: `traits` (cleaned JSON without id fields)
 * - Session ID: `session_id`
 * - Last Activity: `last_activity_time`
 * - App Version: `rudder.app_version`
 * - App Build: `rudder.app_build` (stored as Long in new SDK)
 * - Is Session Manual: `is_session_manual` (inverted from legacy auto tracking)
 *
 * @property anonymousId The anonymous identifier (UUID string) for new SDK
 * @property userId The user identifier extracted from legacy traits JSON
 * @property traitsJson Cleaned JSON string with userId/anonymousId removed
 * @property sessionId Current session identifier for new SDK
 * @property lastActivityTime Timestamp of last activity in milliseconds for new SDK
 * @property appVersion Application version name string for new SDK
 * @property appBuild Application build number (Long type as required by new SDK)
 * @property isSessionManual Whether session is manual (inverted from legacy auto tracking)
 *
 * ## Example Usage
 * ```kotlin
 * val transformedData = TransformedData(
 *     anonymousId = "550e8400-e29b-41d4-a716-446655440000",
 *     userId = "user123",
 *     traitsJson = """{"name":"John","email":"john@example.com"}""", // userId removed
 *     sessionId = 1234567890L,
 *     lastActivityTime = 1640000000000L,
 *     appVersion = "1.0.0",
 *     appBuild = 42L, // Converted from Int to Long
 *     isSessionManual = false // Inverted from autoSessionTrackingEnabled = true
 * )
 * ```
 */
data class TransformedData(
    val anonymousId: String? = null,
    val userId: String? = null,
    val traitsJson: String? = null,
    val sessionId: Long? = null,
    val lastActivityTime: Long? = null,
    val appVersion: String? = null,
    val appBuild: Long? = null,
    val isSessionManual: Boolean? = null,
)
