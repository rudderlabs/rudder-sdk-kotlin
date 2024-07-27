package com.rudderstack.core.internals.utils

import java.util.Base64
import java.util.Locale

fun String.encodeToBase64(): String {
    val formattedString = String.format(Locale.US, "%s:", this)
    val bytes = formattedString.toByteArray(Charsets.UTF_8)
    return Base64.getEncoder().encodeToString(bytes)
}

fun String.toAndroidPrefsKey(writeKey: String): String {
    return "$this-$writeKey"
}

fun String.toPropertiesFileName(prefix: String, suffix: String): String {
    return "$prefix-$this$suffix"
}

fun String.toFileDirectory(directory: String): String {
    return "$directory$this"
}

fun String?.parseFilePaths(): List<String> {
    return if (this.isNullOrEmpty()) {
        emptyList()
    } else {
        this.split(",").map { it.trim() }
    }
}

fun String.Companion.empty(): String = ""
