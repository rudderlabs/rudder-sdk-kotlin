package com.rudderstack.kotlin.sdk.internals.utils

import org.jetbrains.annotations.VisibleForTesting
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * `DateTimeUtils` is a utility object for handling date and time operations,
 * particularly for formatting `Date` objects into a specific string format.
 *
 * This utility is designed to provide consistent and thread-safe date-time formatting
 * in ISO 8601 format with millisecond precision and "Z" notation for UTC timezone.
 * It uses `ThreadLocal<SimpleDateFormat>` to ensure thread safety when formatting dates.
 *
 * The format used by this utility is: `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`
 */
object DateTimeUtils {

    /**
     * A `ThreadLocal` instance that provides thread-safe `SimpleDateFormat` objects.
     *
     * The `SimpleDateFormat` is initialized with the pattern `yyyy-MM-dd'T'HH:mm:ss'.'SSSzzz`
     * to represent dates in ISO 8601 format with millisecond precision. The default
     * timezone for the formatter is set to UTC, and any occurrence of "UTC" in the
     * formatted string is replaced with "Z" to conform to the ISO 8601 standard.
     */
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSSzzz", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Returns the current date and time formatted as an ISO 8601 string.
     *
     * This function retrieves the current date and time using the `Date()` object and
     * formats it to a string using the `ThreadLocal<SimpleDateFormat>` formatter.
     * The formatted string follows the pattern: `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`.
     *
     * @return A `String` representing the current date and time in ISO 8601 format.
     */
    fun now(): String {
        return from(Date())
    }

    /**
     * Formats the provided `Date` object into an ISO 8601 formatted string.
     *
     * This private function takes a `Date` object and uses the thread-safe
     * `SimpleDateFormat` formatter to convert it into a string representation.
     * The formatted string follows the pattern: `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`.
     *
     * @param date The `Date` object to format.
     * @return A `String` representing the provided date in ISO 8601 format.
     */
    @VisibleForTesting
    internal fun from(date: Date): String {
        return formatter.format(date).replace("UTC", "Z")
    }

    /**
     * Returns the current system time in milliseconds.
     *
     * This function retrieves the current system time using `System.currentTimeMillis()`.
     * The time is represented as the number of milliseconds since the Unix epoch.
     *
     * @return A `Long` representing the current system time in milliseconds.
     */
    fun getSystemCurrentTime() = System.currentTimeMillis()
}
