package com.rudderstack.sampleapp.analytics.migration

import android.content.Context
import com.rudderstack.sampleapp.analytics.migration.legacy.LegacyCleaner
import com.rudderstack.sampleapp.analytics.migration.legacy.LegacyExtractor
import com.rudderstack.sampleapp.analytics.migration.model.LegacyData
import com.rudderstack.sampleapp.analytics.migration.model.TransformedData
import com.rudderstack.sampleapp.analytics.migration.newAndroidSdk.NewAndroidSdkWriter
import com.rudderstack.sampleapp.analytics.migration.transform.DataTransformer

/**
 * Main facade for migrating SharedPreferences data from legacy `rudder-sdk-android`
 * to the new `rudder-sdk-kotlin`.
 *
 * This migration utility provides a complete Extract-Transform-Write-Cleanup (ETWC) workflow
 * for safely transferring user data between SDK versions.
 *
 * ## Critical Timing Requirements
 *
 * **WARNING:** This migration MUST be executed BEFORE initializing the new SDK.
 * Running migration after SDK initialization may result in data conflicts or loss.
 *
 * ## Migration Workflow
 *
 * 1. **Pre-flight Checks** - Validate environment before migration
 * 2. **Extract** - Read data from legacy storage (`rl_prefs`)
 * 3. **Transform** - Convert legacy format to new SDK format
 * 4. **Write** - Store transformed data in new storage (`rl_prefs-{writeKey}`)
 * 5. **Cleanup** - Remove legacy storage after successful migration
 *
 * ## Usage Example
 *
 * ```kotlin
 * import com.rudderstack.sampleapp.analytics.migration.Migration
 * import com.rudderstack.sampleapp.analytics.migration.MigratableValue
 *
 * // IMPORTANT: Create Migration instance BEFORE SDK initialization
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *
 *         // Use the write key for the NEW Kotlin SDK (not the legacy SDK write key)
 *         val writeKey = "YOUR_NEW_SDK_WRITE_KEY"
 *
 *         // Concise Kotlin-idiomatic approach
 *         Migration(context = this, writeKey = writeKey).run {
 *             if (!runPreflightChecks()) return@run
 *
 *             extract()
 *                 .let(::transform)
 *                 .let(::write)
 *                 .also { success -> if (success) cleanup() }
 *         }
 *
 *         // NOW it's safe to initialize the SDK
 *         initializeSDK()
 *     }
 *
 *     private fun initializeSDK() {
 *         // Initialize rudder-sdk-kotlin here
 *     }
 * }
 * ```
 *
 * ## Selective Migration
 *
 * You can choose which values to migrate:
 *
 * ```kotlin
 * // Migrate only specific values
 * val legacyData = migration.extract(
 *     values = setOf(
 *         MigratableValue.ANONYMOUS_ID,
 *         MigratableValue.USER_ID,
 *         MigratableValue.TRAITS
 *     )
 * )
 * ```
 *
 * ## Edge Cases & Limitations
 *
 * - **Empty legacy storage**: Pre-flight checks will detect and report this
 * - **Existing new storage**: Migration will abort to prevent data loss
 * - **Partial data**: Migration continues with available values, logs missing ones
 * - **External IDs**: Cannot be migrated (logged as warning)
 * - **Malformed JSON**: Transformation errors are logged, migration continues for other values
 * - **Write failures**: Reported via return value, legacy data remains intact
 *
 * @param context Android context
 * @param writeKey The SDK write key to use for new storage
 *
 * @see MigratableValue for list of values that can be migrated
 * @see LegacyData for extracted legacy data format
 * @see TransformedData for transformed data format ready for new SDK
 */
class Migration(
    private val context: Context,
    private val writeKey: String
) {

    /**
     * SharedPreferences managers.
     */
    private val legacyPrefs: SharedPreferencesManager =
        SharedPreferencesManager(context, MigrationConstants.Legacy.PREFS_NAME)

    private val newPrefs: SharedPreferencesManager =
        SharedPreferencesManager(context, SharedPreferencesManager.newPrefsName(writeKey))

    /**
     * Runs pre-flight validation checks before migration.
     *
     * **IMPORTANT:** Call this method explicitly before starting migration.
     * Validates the environment to ensure migration can proceed safely:
     * 1. Legacy storage file exists
     * 2. Legacy storage contains data
     * 3. New storage file does NOT exist (prevents overwriting)
     *
     * @return true if all checks passed and migration can proceed, false otherwise
     *
     * ## Example
     * ```kotlin
     * val migration = Migration(context, writeKey)
     * val canProceed = migration.runPreflightChecks()
     *
     * if (!canProceed) {
     *     Log.e("Migration", "Pre-flight checks failed")
     *     return
     * }
     *
     * // Proceed with migration
     * val legacyData = migration.extract()
     * ```
     */
    fun runPreflightChecks(): Boolean {
        return PreflightChecker(legacyPrefs, newPrefs).runAllChecks()
    }

    /**
     * Extracts data from legacy SharedPreferences.
     *
     * Reads specified values from the legacy `rudder-sdk-android` storage.
     * Missing values are logged but do not cause failure.
     *
     * @param values Set of [MigratableValue] to extract. Use [MigratableValue.ALL] to extract everything.
     * @return [LegacyData] containing extracted values and metadata about extraction
     *
     * ## Example
     * ```kotlin
     * // Extract all values
     * val data = migration.extract(setOf(MigratableValue.ALL))
     *
     * // Extract specific values only
     * val data = migration.extract(
     *     setOf(MigratableValue.ANONYMOUS_ID, MigratableValue.USER_ID)
     * )
     * ```
     */
    fun extract(values: Set<MigratableValue> = setOf(MigratableValue.ALL)): LegacyData {
        return LegacyExtractor(legacyPrefs).extract(values)
    }

    /**
     * Transforms legacy data to new SDK format.
     *
     * Applies necessary transformations:
     * - Extracts userId from traits JSON
     * - Removes identity fields from traits JSON
     * - Converts appBuild from Int to Long
     *
     * @param legacyData The data extracted from legacy storage
     * @return [TransformedData] ready for writing to new SDK storage
     *
     * ## Example
     * ```kotlin
     * val legacyData = migration.extract()
     * val transformedData = migration.transform(legacyData)
     *
     * // Check for warnings
     * transformedData.transformationWarnings.forEach { warning ->
     *     Log.w("Migration", warning)
     * }
     * ```
     */
    fun transform(legacyData: LegacyData): TransformedData {
        return DataTransformer().transform(legacyData)
    }

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
     * val success = migration.write(transformedData)
     * if (success) {
     *     Log.i("Migration", "Data written successfully")
     * } else {
     *     Log.e("Migration", "Failed to write data")
     * }
     * ```
     */
    fun write(transformedData: TransformedData): Boolean {
        return NewAndroidSdkWriter(newPrefs).write(transformedData)
    }

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
     * val writeSuccess = migration.write(transformedData)
     * if (writeSuccess) {
     *     val cleanupSuccess = migration.cleanup()
     *     if (!cleanupSuccess) {
     *         Log.w("Migration", "Failed to cleanup legacy storage")
     *     }
     * }
     * ```
     */
    fun cleanup(): Boolean {
        return LegacyCleaner(legacyPrefs).cleanup()
    }
}
