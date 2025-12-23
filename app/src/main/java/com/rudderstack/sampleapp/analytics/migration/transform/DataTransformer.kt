package com.abhishek.sanitykotlin.migration.transform

import android.os.SystemClock
import com.abhishek.sanitykotlin.migration.MigratableValue
import com.abhishek.sanitykotlin.migration.MigrationConstants
import com.abhishek.sanitykotlin.migration.MigrationLogger
import com.abhishek.sanitykotlin.migration.model.LegacyData
import com.abhishek.sanitykotlin.migration.model.TransformedData
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

        // Convert lastActivityTime
        val lastActivityTime = convertLastActivityTime(legacyData.lastActivityTime)

        // If lastActivityTime is null (reboot detected), also reset sessionId
        val sessionId = if (lastActivityTime == null && legacyData.lastActivityTime != null) {
            MigrationLogger.warn("Resetting sessionId due to reboot detection. Session will start fresh.")
            null
        } else {
            legacyData.sessionId
        }

        // Invert autoSessionTrackingEnabled to isSessionManual
        val isSessionManual = convertAutoSessionTrackingToManual(legacyData.autoSessionTrackingEnabled)

        MigrationLogger.info("Transformation complete")

        return TransformedData(
            anonymousId = legacyData.anonymousId,
            userId = legacyData.userId,
            traitsJson = cleanedTraits,
            sessionId = sessionId,
            lastActivityTime = lastActivityTime,
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

    /**
     * Converts lastActivityTime from legacy wall-clock format to new SDK monotonic format.
     *
     * ## Important: Time Base Difference
     * - Legacy SDK stores: `System.currentTimeMillis()` (milliseconds since Unix epoch 1970)
     * - New SDK expects: `SystemClock.elapsedRealtime()` (milliseconds since device boot)
     *
     * ## Conversion Logic
     * 1. Calculate elapsed time since last activity: `currentWallClock - legacyTimestamp`
     * 2. Subtract from current monotonic time: `currentMonotonic - elapsed`
     * 3. If result is negative (device rebooted), return null to start fresh session
     *
     * ## Timeline Example - No Reboot (Successful Conversion)
     * ```
     * 10:00 AM - Legacy SDK stores: lastActivityTime = 1735732800000 (wall-clock)
     * 11:00 AM - Migration runs:
     *   currentWallClock = 1735736400000        // 11:00 AM epoch
     *   currentMonotonic = 21600000             // 6 hours (360 min) since boot
     *   legacyTimestamp  = 1735732800000        // 10:00 AM epoch
     *
     *   elapsedSinceLastActivity = 1735736400000 - 1735732800000
     *                            = 3600000 ms   // 60 minutes (11:00 AM - 10:00 AM)
     *
     *   convertedTime = 21600000 - 3600000      // 6 hrs - 1 hr
     *                 = 18000000 (monotonic)    // 5 hours since boot
     *
     *   Result: Returns 18000000 ✅ (represents "1 hour ago" in monotonic time)
     * ```
     *
     * ## Timeline Example - Reboot Detected (Returns null)
     * ```
     * 10:00 AM - Legacy SDK stores: lastActivityTime = 1735732800000 (wall-clock)
     * 10:30 AM - Device reboots (monotonic resets to 0)
     * 11:00 AM - Migration runs (30 min after reboot):
     *   currentWallClock = 1735736400000        // 11:00 AM epoch
     *   currentMonotonic = 1800000              // 30 min since reboot (NOT 6 hours!)
     *   legacyTimestamp  = 1735732800000        // 10:00 AM epoch
     *
     *   elapsedSinceLastActivity = 1735736400000 - 1735732800000
     *                            = 3600000 ms   // 60 minutes
     *
     *   convertedTime = 1800000 - 3600000       // 30 min - 60 min
     *                 = -1800000 ❌ NEGATIVE!   // Cannot subtract 60 min from 30 min uptime
     *
     *   Result: Returns null ✅ (triggers fresh session)
     * ```
     *
     * ## Why Reboot Returns null
     * - Monotonic time resets to 0 on reboot
     * - Cannot represent "60 min ago" on a timeline that's only 30 min old
     * - Safe default: Start fresh session rather than carry stale data
     *
     * @param legacyTimestamp Wall-clock timestamp from legacy SDK (epoch ms)
     * @return Monotonic timestamp for new SDK, or null if conversion not possible (reboot detected)
     *
     * @see IMPLEMENTATION_GUIDE.md for detailed documentation
     */
    private fun convertLastActivityTime(legacyTimestamp: Long?): Long? {
        if (legacyTimestamp == null) return null

        val currentWallClock = System.currentTimeMillis()
        val currentMonotonic = SystemClock.elapsedRealtime()

        // Calculate how long ago the last activity was
        val elapsedSinceLastActivity = currentWallClock - legacyTimestamp

        // Convert to monotonic time base
        val convertedTime = currentMonotonic - elapsedSinceLastActivity

        // If negative (device rebooted since last activity), start fresh session
        if (convertedTime < 0) {
            MigrationLogger.warn(
                "lastActivityTime conversion resulted in negative value. " +
                    "Device may have rebooted. Session will start fresh."
            )
            return null
        }

        MigrationLogger.debug(
            "Converted lastActivityTime: " +
                "legacy=$legacyTimestamp (epoch) -> $convertedTime (monotonic), " +
                "elapsed=${elapsedSinceLastActivity}ms"
        )
        return convertedTime
    }
}
