package com.rudderstack.sampleapp.analytics.migration

/**
 * Enum representing values that can be migrated from the legacy `rudder-sdk-android`
 * to the new `rudder-sdk-kotlin`.
 *
 * Users can select specific values to migrate, or use [ALL] to migrate everything.
 *
 * ## Migration Mapping
 * | Legacy Key | New Key | Notes |
 * |------------|---------|-------|
 * | `rl_anonymous_id_key` | `anonymous_id` | Direct copy |
 * | `rl_traits` (contains id) | `traits` + `user_id` | userId extracted, traits cleaned |
 * | `rl_session_id_key` | `session_id` | Direct copy |
 * | `rl_last_event_timestamp_key` | `last_activity_time` | Direct copy |
 * | `rl_application_version_key` | `rudder.app_version` | Direct copy |
 * | `rl_application_build_key` | `rudder.app_build` | Int → Long conversion |
 * | `rl_auto_session_tracking_status_key` | `is_session_manual` | Boolean inversion |
 */
enum class MigratableValue {
    /**
     * The user identifier.
     *
     * **Legacy:** Embedded as "id" or "userId" field within `rl_traits` JSON.
     * **New:** Stored as separate `user_id` key.
     *
     * During transformation, the userId is extracted from the legacy traits JSON.
     */
    USER_ID,

    /**
     * The anonymous identifier (UUID).
     *
     * **Legacy key:** `rl_anonymous_id_key`
     * **New key:** `anonymous_id`
     *
     * Direct copy, no transformation needed.
     */
    ANONYMOUS_ID,

    /**
     * User traits (JSON object containing user attributes).
     *
     * **Legacy key:** `rl_traits`
     * **New key:** `traits`
     *
     * During transformation:
     * - `id`, `userId`, and `anonymousId` fields are removed from the JSON
     * - The userId is extracted and stored separately as [USER_ID]
     */
    TRAITS,

    /**
     * Session identifier.
     *
     * **Legacy key:** `rl_session_id_key`
     * **New key:** `session_id`
     *
     * Direct copy, no transformation needed. Both use Long type.
     */
    SESSION_ID,

    /**
     * Last activity timestamp for session management.
     *
     * **Legacy key:** `rl_last_event_timestamp_key`
     * **New key:** `last_activity_time`
     *
     * Direct copy, no transformation needed. Both use Long type (milliseconds).
     */
    LAST_ACTIVITY_TIME,

    /**
     * Application version name string.
     *
     * **Legacy key:** `rl_application_version_key`
     * **New key:** `rudder.app_version`
     *
     * Direct copy, no transformation needed.
     */
    APP_VERSION,

    /**
     * Application build number.
     *
     * **Legacy key:** `rl_application_build_key` (stored as Int)
     * **New key:** `rudder.app_build` (stored as Long)
     *
     * Requires type conversion from Int to Long during transformation.
     */
    APP_BUILD,

    /**
     * Manual session flag indicating whether session tracking is manual.
     *
     * **Legacy key:** `rl_auto_session_tracking_status_key` (Boolean)
     * **New key:** `is_session_manual` (Boolean)
     *
     * Requires inversion during transformation:
     * - Legacy `true` (auto tracking enabled) → New `false` (not manual)
     * - Legacy `false` (auto tracking disabled) → New `true` (is manual)
     */
    IS_SESSION_MANUAL,

    /**
     * Convenience value to migrate all supported values.
     *
     * When used in [Migration.extract], this expands to all other values
     * in this enum (excluding [ALL] itself).
     */
    ALL
}
