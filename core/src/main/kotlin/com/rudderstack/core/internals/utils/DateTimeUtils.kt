package com.rudderstack.core.internals.utils

import java.text.SimpleDateFormat
import java.util.*

class DateTimeInstant {
    companion object {

        private val formatters = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSSzzz", Locale.ROOT).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
        }

        fun now(): String {
            return from(Date())
        }

        private fun from(date: Date): String {
            return formatters.get().format(date).replace("UTC", "Z")
        }
    }
}
