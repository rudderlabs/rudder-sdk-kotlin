package com.rudderstack.sampleapp.analytics.migration.legacy

import com.rudderstack.sampleapp.analytics.migration.MigratableValue
import com.rudderstack.sampleapp.analytics.migration.MigrationConstants
import com.rudderstack.sampleapp.analytics.migration.MigrationLogger
import com.rudderstack.sampleapp.analytics.migration.SharedPreferencesManager
import com.rudderstack.sampleapp.analytics.migration.model.LegacyData
import org.json.JSONObject

/**
 * Handles extraction of data from legacy `rudder-sdk-android` SharedPreferences.
 *
 * This class is responsible for reading data from legacy storage.
 *
 * @param legacyPrefs SharedPreferencesManager for legacy storage
 */
internal class LegacyExtractor(private val legacyPrefs: SharedPreferencesManager) {

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
     * val legacyPrefs = SharedPreferencesManager(context, MigrationConstants.Legacy.PREFS_NAME)
     * val extractor = LegacyExtractor(legacyPrefs)
     *
     * // Extract all values
     * val data = extractor.extract(setOf(MigratableValue.ALL))
     *
     * // Extract specific values only
     * val data = extractor.extract(
     *     setOf(MigratableValue.ANONYMOUS_ID, MigratableValue.USER_ID)
     * )
     * ```
     */
    internal fun extract(values: Set<MigratableValue> = setOf(MigratableValue.ALL)): LegacyData {
        val expandedValues = expandMigratableValues(values)

        MigrationLogger.info("Starting data extraction from legacy SDK...")
        MigrationLogger.debug("Attempting to extract ${expandedValues.size} values: ${expandedValues.joinToString()}")

        // Extract all values using generic extractor
        val anonymousId = extractValue(
            migratableValue = MigratableValue.ANONYMOUS_ID,
            expandedValues = expandedValues,
            extractor = { legacyPrefs.getString(MigrationConstants.Legacy.ANONYMOUS_ID_KEY) },
            formatter = { it.take(50) }
        )
        val traitsJson = extractValue(
            migratableValue = MigratableValue.TRAITS,
            expandedValues = expandedValues,
            extractor = { legacyPrefs.getString(MigrationConstants.Legacy.TRAITS_KEY) },
            formatter = { it.take(50) }
        )
        val sessionId = extractValue(
            migratableValue = MigratableValue.SESSION_ID,
            expandedValues = expandedValues,
            extractor = {
                legacyPrefs.getLong(MigrationConstants.Legacy.SESSION_ID_KEY, -1L)
                    .takeIf { it != -1L }
            }
        )
        val lastActivityTime = extractValue(
            migratableValue = MigratableValue.LAST_ACTIVITY_TIME,
            expandedValues = expandedValues,
            extractor = {
                legacyPrefs.getLong(MigrationConstants.Legacy.LAST_ACTIVITY_KEY, -1L)
                    .takeIf { it != -1L }
            }
        )
        val appVersion = extractValue(
            migratableValue = MigratableValue.APP_VERSION,
            expandedValues = expandedValues,
            extractor = { legacyPrefs.getString(MigrationConstants.Legacy.APP_VERSION_KEY) }
        )
        val appBuild = extractValue(
            migratableValue = MigratableValue.APP_BUILD,
            expandedValues = expandedValues,
            extractor = {
                // Priority 1: Check newer key (Java SDK v1.5.2+)
                val buildFromNewKey = legacyPrefs.getInt(MigrationConstants.Legacy.APP_BUILD_KEY, -1)
                if (buildFromNewKey != -1) {
                    return@extractValue buildFromNewKey
                }
                // Priority 2: Fallback to legacy key (Java SDK v1.5.1-)
                legacyPrefs.getInt(MigrationConstants.Legacy.APP_INFO_KEY, -1)
                    .takeIf { it != -1 }
            }
        )
        val autoSessionTrackingEnabled = extractValue(
            migratableValue = MigratableValue.IS_SESSION_MANUAL,
            expandedValues = expandedValues,
            extractor = {
                val key = MigrationConstants.Legacy.AUTO_SESSION_TRACKING_STATUS_KEY
                if (legacyPrefs.contains(key)) {
                    legacyPrefs.getBoolean(key, true)
                } else {
                    null
                }
            },
            formatter = { "autoSessionTracking=$it" }
        )
        val userId = extractUserId(expandedValues)

        MigrationLogger.info("Extraction complete")

        return LegacyData(
            anonymousId = anonymousId,
            traitsJson = traitsJson,
            userId = userId,
            sessionId = sessionId,
            lastActivityTime = lastActivityTime,
            appVersion = appVersion,
            appBuild = appBuild,
            autoSessionTrackingEnabled = autoSessionTrackingEnabled,
        )
    }

    private fun expandMigratableValues(values: Set<MigratableValue>): Set<MigratableValue> {
        return if (values.contains(MigratableValue.ALL)) {
            MigratableValue.entries.filter { it != MigratableValue.ALL }.toSet()
        } else {
            values
        }
    }

    private fun <T> extractValue(
        migratableValue: MigratableValue,
        expandedValues: Set<MigratableValue>,
        extractor: () -> T?,
        formatter: (T) -> String = { it.toString() }
    ): T? {
        if (!expandedValues.contains(migratableValue)) return null

        val value = extractor()
        if (value != null) {
            MigrationLogger.debug("Found $migratableValue: ${formatter(value)}")
        } else {
            MigrationLogger.debug("Missing: $migratableValue")
        }
        return value
    }

    /**
     * Extracts USER_ID from traits JSON.
     *
     * @return The extracted user ID if found, null otherwise
     */
    private fun extractUserId(expandedValues: Set<MigratableValue>): String? {
        if (!expandedValues.contains(MigratableValue.USER_ID)) return null

        val traitsJson = legacyPrefs.getString(MigrationConstants.Legacy.TRAITS_KEY)
        if (traitsJson != null) {
            val extractedUserId = extractUserIdFromTraits(traitsJson)
            if (extractedUserId != null) {
                MigrationLogger.debug("Found ${MigratableValue.USER_ID}: ${extractedUserId.take(10)}")
                return extractedUserId
            }
        }
        MigrationLogger.debug("Missing: ${MigratableValue.USER_ID}")
        return null
    }

    /**
     * Extracts user ID from legacy traits JSON.
     *
     * Checks for "id" field first, then "userId" field.
     *
     * @param traitsJson JSON string containing user traits
     * @return The user ID if found, null otherwise
     */
    private fun extractUserIdFromTraits(traitsJson: String): String? {
        return try {
            val json = JSONObject(traitsJson)
            MigrationConstants.Legacy.Traits.USER_ID_FIELDS.firstNotNullOfOrNull { field ->
                if (json.has(field)) {
                    json.optString(field).takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            MigrationLogger.warn("Failed to parse traits JSON: ${e.message}")
            null
        }
    }
}
