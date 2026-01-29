package com.rudderstack.sdk.kotlin.core.internals.migration

import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Defines a migration step from one data schema version to another.
 */
@InternalRudderApi
interface Migration {

    /**
     * The older version for the migration.
     */
    val oldVersion: Int

    /**
     * The newer version for the migration.
     */
    val newVersion: Int

    /**
     * Executes the migration logic from old version to new version.
     * @param storage The storage instance to read/write data.
     */
    suspend fun migrateOldToNew(storage: Storage)

    /**
     * Executes the migration logic from new version to old version.
     * @param storage The storage instance to read/write data.
     */
    suspend fun migrateNewToOld(storage: Storage)
}
