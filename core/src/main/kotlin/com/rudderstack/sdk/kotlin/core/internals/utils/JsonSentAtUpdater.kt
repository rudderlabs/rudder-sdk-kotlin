package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP

private const val SENT_AT_PATTERN = """"sentAt":"$DEFAULT_SENT_AT_TIMESTAMP""""

internal object JsonSentAtUpdater {

    internal fun updateSentAt(jsonString: String): String {
        val latestTimestamp = DateTimeUtils.now()
        LoggerAnalytics.verbose("Updating sentAt in JSON string to $latestTimestamp")

        val updatedJsonString = jsonString.replace(
            SENT_AT_PATTERN,
            """"sentAt":"$latestTimestamp""""
        )

        return updatedJsonString
    }
}
