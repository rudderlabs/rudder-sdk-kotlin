package com.rudderstack.sampleapp.analytics.migration.transform

import com.rudderstack.sampleapp.analytics.migration.MigrationConstants
import com.rudderstack.sampleapp.analytics.migration.MigrationLogger
import com.rudderstack.sampleapp.analytics.migration.model.LegacyData
import com.rudderstack.sampleapp.analytics.migration.model.TransformedData
import org.json.JSONObject

/**
 * Handles transformation of data from legacy SDK format to new SDK format.
 *
 * This class provides stateless transformation logic to convert legacy data
 * into the format required by the new `rudder-sdk-kotlin`.
 */
internal class DataTransformer {

    /**
     * Transforms legacy data to new SDK format.
     *
     * Applies necessary transformations:
     * - Removes identity fields from traits JSON
     * - Converts appBuild from Int to Long
     * - Inverts autoSessionTrackingEnabled to isSessionManual
     *
     * Note: userId is already extracted in the extraction phase and is copied directly.
     *
     * @param legacyData The data extracted from legacy storage
     * @return [TransformedData] ready for writing to new SDK storage
     *
     * ## Example
     * ```kotlin
     * val transformer = DataTransformer()
     * val legacyData = LegacyData(...)
     * val transformedData = transformer.transform(legacyData)
     * ```
     */
    internal fun transform(legacyData: LegacyData): TransformedData {
        MigrationLogger.info("Starting data transformation...")

        // Clean traits
        val cleanedTraits = cleanTraits(legacyData.traitsJson)

        // Convert appBuild
        val appBuild = convertAppBuild(legacyData.appBuild)

        // Invert autoSessionTrackingEnabled to isSessionManual
        val isSessionManual = convertAutoSessionTrackingToManual(legacyData.autoSessionTrackingEnabled)

        MigrationLogger.info("Transformation complete")

        return TransformedData(
            anonymousId = legacyData.anonymousId,
            userId = legacyData.userId,
            traitsJson = cleanedTraits,
            sessionId = legacyData.sessionId,
            lastActivityTime = legacyData.lastActivityTime,
            appVersion = legacyData.appVersion,
            appBuild = appBuild,
            isSessionManual = isSessionManual,
        )
    }

    private fun cleanTraits(traitsJson: String?): String? {
        if (traitsJson == null) return null

        return try {
            val traitsObject = JSONObject(traitsJson)

            removeIdentityFields(traitsObject)

            if (traitsObject.length() == 0) {
                MigrationLogger.debug("Traits object is empty after removing identity fields")
            }

            traitsObject.toString()
        } catch (e: Exception) {
            MigrationLogger.warn("Failed to transform traits JSON: ${e.message}")
            null
        }
    }

    private fun removeIdentityFields(traitsObject: JSONObject) {
        MigrationConstants.Legacy.Traits.FIELDS_TO_REMOVE.forEach { field ->
            traitsObject.remove(field)
        }
    }

    private fun convertAppBuild(appBuildInt: Int?): Long? {
        var appBuild: Long? = null
        if (appBuildInt != null) {
            appBuild = appBuildInt.toLong()
            MigrationLogger.debug("Converted appBuild from Int($appBuildInt) to Long($appBuild)")
        }
        return appBuild
    }

    /**
     * Converts autoSessionTrackingEnabled to isSessionManual by inverting the value.
     *
     * - Legacy `true` (auto tracking enabled) → New `false` (not manual)
     * - Legacy `false` (auto tracking disabled) → New `true` (is manual)
     *
     * @param autoSessionTrackingEnabled The legacy auto session tracking status
     * @return Inverted boolean for isSessionManual, or null if input is null
     */
    private fun convertAutoSessionTrackingToManual(autoSessionTrackingEnabled: Boolean?): Boolean? {
        if (autoSessionTrackingEnabled == null) return null

        val isSessionManual = !autoSessionTrackingEnabled
        MigrationLogger.debug(
            "Converted autoSessionTracking=$autoSessionTrackingEnabled to isSessionManual=$isSessionManual"
        )
        return isSessionManual
    }
}
