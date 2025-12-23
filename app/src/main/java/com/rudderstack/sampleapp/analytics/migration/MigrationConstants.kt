package com.abhishek.sanitykotlin.migration

/**
 * Constants for SharedPreferences migration from legacy `rudder-sdk-android`
 * to new `rudder-sdk-kotlin`.
 *
 * This object organizes all storage keys and configuration values used during
 * the migration process, separated into legacy and new SDK sections.
 */
object MigrationConstants {

    /**
     * Constants for the legacy SDK (`rudder-sdk-android`).
     */
    object Legacy {
        /**
         * SharedPreferences file name used by the legacy SDK.
         */
        const val PREFS_NAME = "rl_prefs"

        /**
         * Key for anonymous ID storage.
         */
        const val ANONYMOUS_ID_KEY = "rl_anonymous_id_key"

        /**
         * Key for user traits (JSON string).
         */
        const val TRAITS_KEY = "rl_traits"

        /**
         * Key for external IDs (not migratable).
         */
        const val EXTERNAL_ID_KEY = "rl_external_id"

        /**
         * Key for session ID.
         */
        const val SESSION_ID_KEY = "rl_session_id_key"

        /**
         * Key for last activity timestamp.
         */
        const val LAST_ACTIVITY_KEY = "rl_last_event_timestamp_key"

        /**
         * Key for application version string.
         */
        const val APP_VERSION_KEY = "rl_application_version_key"

        /**
         * Key for application build number (stored as Int).
         * Used in SDK v1.5.2+ (June 2022 onwards).
         */
        const val APP_BUILD_KEY = "rl_application_build_key"

        /**
         * Key for application build number (legacy location, pre-June 2022).
         * Used in SDK v1.5.1 and earlier. Superseded by APP_BUILD_KEY.
         */
        const val APP_INFO_KEY = "rl_application_info_key"

        /**
         * Key for auto session tracking status.
         * Stores whether automatic session tracking was enabled.
         */
        const val AUTO_SESSION_TRACKING_STATUS_KEY = "rl_auto_session_tracking_status_key"

        /**
         * Constants for processing legacy traits JSON during transformation.
         */
        object Traits {
            /**
             * Fields to check when extracting user ID from legacy traits JSON.
             * Checked in order: "id" first, then "userId".
             */
            val USER_ID_FIELDS = listOf("id", "userId")

            /**
             * Identity fields to remove from traits during transformation.
             * These are stored separately in the new SDK.
             */
            val FIELDS_TO_REMOVE = listOf("id", "userId", "anonymousId")
        }
    }

    /**
     * Constants for the new SDK (`rudder-sdk-kotlin`).
     */
    object New {
        /**
         * SharedPreferences file name prefix.
         * Actual file name is "{PREFS_PREFIX}-{writeKey}".
         */
        const val PREFS_PREFIX = "rl_prefs"

        /**
         * Key for anonymous ID storage.
         */
        const val ANONYMOUS_ID_KEY = "anonymous_id"

        /**
         * Key for user ID storage (extracted from legacy traits).
         */
        const val USER_ID_KEY = "user_id"

        /**
         * Key for user traits (JSON string, cleaned of identity fields).
         */
        const val TRAITS_KEY = "traits"

        /**
         * Key for session ID.
         */
        const val SESSION_ID_KEY = "session_id"

        /**
         * Key for last activity timestamp.
         */
        const val LAST_ACTIVITY_KEY = "last_activity_time"

        /**
         * Key for application version string.
         */
        const val APP_VERSION_KEY = "rudder.app_version"

        /**
         * Key for application build number (stored as Long).
         */
        const val APP_BUILD_KEY = "rudder.app_build"

        /**
         * Key for manual session flag.
         * Stores whether the session is manual (not automatic).
         * This is the inverse of the legacy AUTO_SESSION_TRACKING_STATUS_KEY.
         */
        const val IS_SESSION_MANUAL_KEY = "is_session_manual"
    }
}
