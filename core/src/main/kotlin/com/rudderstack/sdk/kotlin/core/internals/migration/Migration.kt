package com.rudderstack.sdk.kotlin.core.internals.migration

import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Defines a migration step from one data schema version to another.
 */
@InternalRudderApi
interface Migration {

    /**
     * The version number this migration starts from.
     */
    val fromVersion: Int

    /**
     * The version number this migration upgrades to.
     */
    val toVersion: Int

    /**
     * Executes the migration logic.
     * @param storage The storage instance to read/write data.
     */
    suspend fun migrate(storage: Storage)
}
