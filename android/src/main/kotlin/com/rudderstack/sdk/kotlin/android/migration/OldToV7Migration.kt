package com.rudderstack.sdk.kotlin.android.migration

import android.os.SystemClock
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.migration.Migration
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys

private const val UPDATED_VERSION = 7

internal class OldToV7Migration(private val sessionConfiguration: SessionConfiguration) : Migration {

    override val fromVersion: Int = -1

    override val toVersion: Int = UPDATED_VERSION

    override suspend fun migrate(storage: Storage) {
        if (!sessionConfiguration.automaticSessionTracking) {
            return
        }

        val storedLastActivityTime = storage.readLong(StorageKeys.LAST_ACTIVITY_TIME, -1L)

        if (storedLastActivityTime == -1L) {
            // No last activity time stored, nothing to migrate
            return
        }

        // Calculate time difference
        val currentTimeDifference = SystemClock.elapsedRealtime() - storedLastActivityTime

        if (currentTimeDifference > 0 && currentTimeDifference < sessionConfiguration.sessionTimeoutInMillis) {
            // session is not expired, therefore migrate
            val newEpochTime = System.currentTimeMillis()
            storage.write(StorageKeys.LAST_ACTIVITY_TIME, newEpochTime)
            LoggerAnalytics.debug("Migrated lastActivityTime to Epoch: $newEpochTime")
        } else {
            // session is expired, clear the last activity time, so that session expiry logic can work reliably
            storage.remove(StorageKeys.LAST_ACTIVITY_TIME)
        }
    }
}
