package com.abhishek.sanitykotlin.migration.model

/**
 * Represents data extracted from the legacy `rudder-sdk-android` SharedPreferences.
 *
 * This data class holds raw values from the legacy SDK before any transformation is applied.
 * All fields are nullable to represent potentially missing data in the legacy storage.
 *
 * ## Legacy Storage Keys
 * - Anonymous ID: `rl_anonymous_id_key`
 * - Traits: `rl_traits` (JSON string containing user attributes and potentially userId)
 * - Session ID: `rl_session_id_key`
 * - Last Activity: `rl_last_event_timestamp_key`
 * - App Version: `rl_application_version_key`
 * - App Build: `rl_application_build_key` (stored as Int in legacy)
 * - Auto Session Tracking: `rl_auto_session_tracking_status_key` (Boolean)
 *
 * @property anonymousId The anonymous identifier (UUID string) from legacy storage
 * @property traitsJson Raw JSON string containing user traits from legacy storage
 * @property userId The user identifier extracted from legacy traits JSON
 * @property sessionId Current session identifier from legacy storage
 * @property lastActivityTime Timestamp of last activity in milliseconds from legacy storage
 * @property appVersion Application version name string from legacy storage
 * @property appBuild Application build number (Int type as stored in legacy SDK)
 * @property autoSessionTrackingEnabled Whether automatic session tracking was enabled in legacy SDK
 *
 * ## Example Usage
 * ```kotlin
 * val legacyData = LegacyData(
 *     anonymousId = "550e8400-e29b-41d4-a716-446655440000",
 *     traitsJson = """{"id":"user123","name":"John","email":"john@example.com"}""",
 *     userId = "user123",
 *     sessionId = 1234567890L,
 *     lastActivityTime = 1640000000000L,
 *     appVersion = "1.0.0",
 *     appBuild = 42,
 *     autoSessionTrackingEnabled = true
 * )
 * ```
 */
data class LegacyData(
    val anonymousId: String? = null,
    val traitsJson: String? = null,
    val userId: String? = null,
    val sessionId: Long? = null,
    val lastActivityTime: Long? = null,
    val appVersion: String? = null,
    val appBuild: Int? = null,
    val autoSessionTrackingEnabled: Boolean? = null,
)
