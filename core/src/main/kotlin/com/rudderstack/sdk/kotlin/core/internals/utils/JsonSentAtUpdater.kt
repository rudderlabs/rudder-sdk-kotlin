package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.DEFAULT_SENT_AT_TIMESTAMP

private const val SENT_AT_PATTERN = """"sentAt":"$DEFAULT_SENT_AT_TIMESTAMP""""

internal class JsonSentAtUpdater {

    fun updateSentAt(jsonString: String): String {
        val latestTimestamp = DateTimeUtils.now()

        val updatedJsonString = jsonString.replace(
            SENT_AT_PATTERN,
            """"sentAt":"$latestTimestamp""""
        )

        return updatedJsonString
    }
}
