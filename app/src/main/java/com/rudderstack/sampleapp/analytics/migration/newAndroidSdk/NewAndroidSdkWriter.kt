package com.rudderstack.sampleapp.analytics.migration.newAndroidSdk

import com.rudderstack.sampleapp.analytics.migration.MigrationConstants
import com.rudderstack.sampleapp.analytics.migration.MigrationLogger
import com.rudderstack.sampleapp.analytics.migration.SharedPreferencesManager
import com.rudderstack.sampleapp.analytics.migration.model.TransformedData

/**
 * Handles writing data to new `rudder-sdk-kotlin` SharedPreferences.
 *
 * This class is responsible for writing transformed data to new SDK storage.
 *
 * @param newPrefs SharedPreferencesManager for new SDK storage
 */
internal class NewAndroidSdkWriter(
    private val newPrefs: SharedPreferencesManager
) {

    /**
     * Writes transformed data to new SDK SharedPreferences.
     *
     * Stores all non-null values using the new SDK's storage keys and format.
     * Uses synchronous commit() to ensure data is written before returning.
     *
     * @param transformedData The data to write
     * @return true if write was successful, false otherwise
     *
     * ## Example
     * ```kotlin
     * val newPrefs = SharedPreferencesManager(context, SharedPreferencesManager.newPrefsName(writeKey))
     * val writer = NewAndroidSdkWriter(newPrefs)
     * val success = writer.write(transformedData)
     * if (success) {
     *     Log.i("Migration", "Data written successfully")
     * } else {
     *     Log.e("Migration", "Failed to write data")
     * }
     * ```
     */
    internal fun write(transformedData: TransformedData): Boolean {
        MigrationLogger.info("Starting write to new storage")

        try {
            val editor = newPrefs.edit()

            // Write all values using helpers
            writeStringValue(editor, transformedData.anonymousId, MigrationConstants.New.ANONYMOUS_ID_KEY)
            writeStringValue(editor, transformedData.userId, MigrationConstants.New.USER_ID_KEY)
            writeStringValue(editor, transformedData.traitsJson, MigrationConstants.New.TRAITS_KEY)
            writeLongValue(editor, transformedData.sessionId, MigrationConstants.New.SESSION_ID_KEY)
            writeLongValue(editor, transformedData.lastActivityTime, MigrationConstants.New.LAST_ACTIVITY_KEY)
            writeStringValue(editor, transformedData.appVersion, MigrationConstants.New.APP_VERSION_KEY)
            writeLongValue(editor, transformedData.appBuild, MigrationConstants.New.APP_BUILD_KEY)
            writeBooleanValue(editor, transformedData.isSessionManual, MigrationConstants.New.IS_SESSION_MANUAL_KEY)

            val success = editor.commit()
            if (success) {
                MigrationLogger.info("✓ Write complete: values written successfully")
            } else {
                MigrationLogger.error("✗ Write failed (commit failed): SharedPreferences commit returned false")
            }

            return success
        } catch (e: Exception) {
            MigrationLogger.error("✗ Write failed (exception): ${e.message ?: "Unknown error"}")
            return false
        }
    }

    private fun writeStringValue(editor: SharedPreferencesManager.Editor, value: String?, key: String): Boolean {
        return value?.let {
            editor.putString(key, it)
            MigrationLogger.debug("Written key: $key")
            true
        } ?: false
    }

    private fun writeLongValue(editor: SharedPreferencesManager.Editor, value: Long?, key: String): Boolean {
        return value?.let {
            editor.putLong(key, it)
            MigrationLogger.debug("Written key: $key")
            true
        } ?: false
    }

    private fun writeBooleanValue(editor: SharedPreferencesManager.Editor, value: Boolean?, key: String): Boolean {
        return value?.let {
            editor.putBoolean(key, it)
            MigrationLogger.debug("Written key: $key = $it")
            true
        } ?: false
    }
}
