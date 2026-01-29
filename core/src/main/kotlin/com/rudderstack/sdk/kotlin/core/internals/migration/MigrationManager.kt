package com.rudderstack.sdk.kotlin.core.internals.migration

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution

/**
 * Manages and performs data schema migrations for the storage system.
 */
@InternalRudderApi
class MigrationManager(
    private val storage: Storage,
    private val targetSchemaVersion: Int,
    private val migrations: List<Migration>
) {

    /**
     * Performs necessary migrations to update the data schema to the target version.
     * Handles both forward migrations (when target > current) and backward migrations (when target < current).
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun performMigrations() {
        // -1 indicates no version stored
        val currentSchemaVersion = storage.readInt(StorageKeys.DATA_SCHEMA_VERSION, -1)

        if (currentSchemaVersion == targetSchemaVersion) {
            LoggerAnalytics.debug("MigrationManager: No migration needed. Current version: $currentSchemaVersion")
            return // Already up to date
        }

        when {
            currentSchemaVersion < targetSchemaVersion -> performForwardMigrations(currentSchemaVersion)
            else -> performBackwardMigrations(currentSchemaVersion)
        }

        // Finally, update to the target schema version when all migrations are done
        storage.write(StorageKeys.DATA_SCHEMA_VERSION, targetSchemaVersion)
    }

    /**
     * Performs forward migrations from current version to target version.
     */
    @OptIn(UseWithCaution::class)
    private suspend fun performForwardMigrations(currentSchemaVersion: Int) {
        LoggerAnalytics.debug(
            "MigrationManager: Starting forward migration from version $currentSchemaVersion to $targetSchemaVersion"
        )

        val pendingMigrations = migrations
            .filter { it.oldVersion >= currentSchemaVersion && it.newVersion <= targetSchemaVersion }
            .sortedBy { it.oldVersion }

        for (migration in pendingMigrations) {
            try {
                LoggerAnalytics.debug(
                    "MigrationManager: Applying migration: ${migration.oldVersion} -> ${migration.newVersion}"
                )
                migration.migrateOldToNew(storage)
            } catch (e: Exception) {
                LoggerAnalytics.error(
                    "MigrationManager: Migration failed at step ${migration.oldVersion} -> ${migration.newVersion}",
                    e
                )
                // if the migration fails, we clear the storage to avoid inconsistent state
                storage.delete()
                break
            }
        }
    }

    /**
     * Performs backward migrations from current version to target version.
     */
    @OptIn(UseWithCaution::class)
    private suspend fun performBackwardMigrations(currentSchemaVersion: Int) {
        LoggerAnalytics.debug(
            "MigrationManager: Starting backward migration from version $currentSchemaVersion to $targetSchemaVersion"
        )

        val pendingMigrations = migrations
            .filter { it.newVersion <= currentSchemaVersion && it.oldVersion >= targetSchemaVersion }
            .sortedByDescending { it.newVersion } // Sort in reverse order for back migration

        for (migration in pendingMigrations) {
            try {
                LoggerAnalytics.debug(
                    "MigrationManager: Applying migration back: ${migration.newVersion} -> ${migration.oldVersion}"
                )
                migration.migrateNewToOld(storage)
            } catch (e: Exception) {
                LoggerAnalytics.error(
                    "MigrationManager: Migration back failed at step ${migration.newVersion} -> ${migration.oldVersion}",
                    e
                )
                // if the migration back fails, we clear the storage to avoid inconsistent state
                storage.delete()
                break
            }
        }
    }
}
