package com.rudderstack.sdk.kotlin.android.migration

import android.os.SystemClock
import com.rudderstack.sdk.kotlin.android.SessionConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.migration.Migration
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys

private const val OLD_VERSION = 6
private const val NEW_VERSION = 7

internal class V6ToV7Migration(private val sessionConfiguration: SessionConfiguration) : Migration {

    override val oldVersion: Int = OLD_VERSION

    override val newVersion: Int = NEW_VERSION

    override suspend fun migrateOldToNew(storage: Storage) {
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

    override suspend fun migrateNewToOld(storage: Storage) {
        if (!sessionConfiguration.automaticSessionTracking) {
            return
        }

        val storedLastActivityTime = storage.readLong(StorageKeys.LAST_ACTIVITY_TIME, -1L)

        if (storedLastActivityTime == -1L) {
            // No last activity time stored, nothing to migrate back
            return
        }

        // Check if the stored epoch time represents a valid session
        val currentEpochTime = System.currentTimeMillis()
        val timeDifference = currentEpochTime - storedLastActivityTime

        if (timeDifference >= 0 && timeDifference < sessionConfiguration.sessionTimeoutInMillis) {
            // Session is still valid, set current SystemClock.elapsedRealtime() as the new last activity time
            val currentElapsedTime = SystemClock.elapsedRealtime()
            storage.write(StorageKeys.LAST_ACTIVITY_TIME, currentElapsedTime)
            LoggerAnalytics.debug("Migrated back lastActivityTime from Epoch to SystemClock: $currentElapsedTime")
        } else {
            // Session is expired, clear the last activity time
            storage.remove(StorageKeys.LAST_ACTIVITY_TIME)
            LoggerAnalytics.debug("Removed expired lastActivityTime during migration back")
        }
    }
}
