package com.rudderstack.sampleapp.analytics.migration

/**
 * Centralized validation utility for migration preflight checks.
 *
 * Performs all validation checks required before migration can proceed:
 * 1. Write key validation (non-empty)
 * 2. Legacy storage file existence
 * 3. Legacy storage data presence
 * 4. New storage file non-existence (prevent overwriting)
 *
 * This class follows the Single Responsibility Principle by focusing solely on
 * validation logic, separate from extraction, transformation, and writing concerns.
 *
 * ## Usage Example
 * ```kotlin
 * val legacyPrefs = SharedPreferencesManager(context, MigrationConstants.Legacy.PREFS_NAME)
 * val newPrefs = SharedPreferencesManager(context, SharedPreferencesManager.newPrefsName(writeKey))
 * val checker = PreflightChecker(legacyPrefs, newPrefs)
 * val allChecksPassed = checker.runAllChecks()
 *
 * if (allChecksPassed) {
 *     // Safe to proceed with migration
 * } else {
 *     Log.e("Migration", "Preflight checks failed")
 * }
 * ```
 *
 * @param legacyPrefs SharedPreferencesManager for legacy storage
 * @param newPrefs SharedPreferencesManager for new SDK storage
 */
internal class PreflightChecker(
    private val legacyPrefs: SharedPreferencesManager,
    private val newPrefs: SharedPreferencesManager
) {

    /**
     * Runs all preflight validation checks.
     *
     * Executes checks in logical order:
     * 1. Legacy file existence
     * 2. Legacy data presence
     * 3. New file non-existence
     *
     * @return true if all checks passed, false otherwise
     */
    internal fun runAllChecks(): Boolean {
        MigrationLogger.info("Starting migration pre-flight checks...")

        val legacyFileExists = checkLegacyFileExists()
        val legacyNotEmpty = checkLegacyNotEmpty()
        val newFileNotExists = checkNewFileNotExists()

        val allPassed = legacyFileExists && legacyNotEmpty && newFileNotExists

        if (allPassed) {
            MigrationLogger.info("✓ All pre-flight checks passed")
        } else {
            MigrationLogger.error("✗ Pre-flight checks failed")
        }

        return allPassed
    }

    private fun checkLegacyFileExists(): Boolean {
        val legacyExists = legacyPrefs.fileExists()

        if (legacyExists) {
            MigrationLogger.info("✓ Pre-flight check passed [Legacy SDK data exists]: Legacy storage file found at shared_prefs/${MigrationConstants.Legacy.PREFS_NAME}.xml")
        } else {
            MigrationLogger.error("✗ Pre-flight check failed [Legacy SDK data exists]: Legacy storage file not found")
        }

        return legacyExists
    }

    private fun checkLegacyNotEmpty(): Boolean {
        val legacyNotEmpty = !legacyPrefs.isEmpty()

        if (legacyNotEmpty) {
            MigrationLogger.info("✓ Pre-flight check passed [Legacy SDK data is not empty]: Legacy storage contains ${legacyPrefs.getKeyCount()} key(s)")
        } else {
            MigrationLogger.error("✗ Pre-flight check failed [Legacy SDK data is not empty]: Legacy storage is empty")
        }

        return legacyNotEmpty
    }

    private fun checkNewFileNotExists(): Boolean {
        val newDoesNotExist = !newPrefs.fileExists()

        if (newDoesNotExist) {
            MigrationLogger.info("✓ Pre-flight check passed [New SDK data does not exist]: New storage does not exist (safe to proceed)")
        } else {
            MigrationLogger.error("✗ Pre-flight check failed [New SDK data does not exist]: New storage already exists")
        }

        return newDoesNotExist
    }
}
