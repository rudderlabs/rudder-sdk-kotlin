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
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun performMigrations() {
        // -1 indicates no version stored
        val currentVersion = storage.readInt(StorageKeys.DATA_SCHEMA_VERSION, -1)

        if (currentVersion == targetSchemaVersion) {
            LoggerAnalytics.debug("MigrationManager: No migration needed. Current version: $currentVersion")
            return // Already up to date
        }

        LoggerAnalytics.debug("MigrationManager: Starting migration from version $currentVersion to $targetSchemaVersion")

        val pendingMigrations = migrations
            .filter { it.fromVersion >= currentVersion && it.toVersion <= targetSchemaVersion }
            .sortedBy { it.fromVersion }

        @OptIn(UseWithCaution::class)
        for (migration in pendingMigrations) {
            try {
                LoggerAnalytics.debug(
                    "MigrationManager: Applying migration: ${migration.fromVersion} -> ${migration.toVersion}"
                )
                migration.migrate(storage)
            } catch (e: Exception) {
                LoggerAnalytics.error(
                    "MigrationManager: Migration failed at step ${migration.fromVersion} -> ${migration.toVersion}",
                    e
                )
                // if the migration fails, we clear the storage to avoid inconsistent state
                storage.delete()
                break
            }
        }

        // Finally, update to the target schema version when all migrations are done
        storage.write(StorageKeys.DATA_SCHEMA_VERSION, targetSchemaVersion)
    }
}
