package com.abhishek.sanitykotlin.migration.legacy

import com.abhishek.sanitykotlin.migration.MigrationLogger
import com.abhishek.sanitykotlin.migration.SharedPreferencesManager

/**
 * Handles cleanup of legacy `rudder-sdk-android` SharedPreferences.
 *
 * This class is responsible for deleting the legacy storage file after successful migration.
 *
 * @param legacyPrefs SharedPreferencesManager for legacy storage
 */
internal class LegacyCleaner(private val legacyPrefs: SharedPreferencesManager) {

    /**
     * Cleans up legacy SharedPreferences after successful migration.
     *
     * Deletes the legacy `rl_prefs` file from disk. This operation is irreversible.
     *
     * **Note:** Only call this after confirming successful write to new storage.
     *
     * @return true if cleanup was successful, false otherwise
     *
     * ## Example
     * ```kotlin
     * val legacyPrefs = SharedPreferencesManager(context, MigrationConstants.Legacy.PREFS_NAME)
     * val cleaner = LegacyCleaner(legacyPrefs)
     * val cleanupSuccess = cleaner.cleanup()
     * if (!cleanupSuccess) {
     *     Log.w("Migration", "Failed to cleanup legacy storage")
     * }
     * ```
     */
    internal fun cleanup(): Boolean {
        MigrationLogger.info("Starting cleanup of legacy storage...")

        return try {
            val success = legacyPrefs.delete()

            if (success) {
                MigrationLogger.info("✓ Cleanup complete: legacy storage deleted successfully")
            } else {
                MigrationLogger.error("✗ Cleanup failed: unable to delete legacy storage")
            }
            success
        } catch (e: Exception) {
            MigrationLogger.error("✗ Cleanup failed: ${e.message ?: "Unknown error"}")
            false
        }
    }
}
