package com.rudderstack.kotlin.sdk

import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.models.LoggerManager
import java.io.BufferedReader

fun Any.readFileAsString(fileName: String): String {
    val inputStream = this::class.java.classLoader.getResourceAsStream(fileName)
    return inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
}

fun Any.readFileTrimmed(fileName: String) =
    this.readFileAsString(fileName).cleanJsonString()

private fun String.cleanJsonString(): String {
    val regex = Regex("""(?<!\\)"[^"]*?"|[\s\n\r]+""")
    return this.replace(regex) { matchResult ->
        val matchedText = matchResult.value
        if (matchedText.startsWith("\"") && matchedText.endsWith("\"")) {
            // Keep the quoted text as is
            matchedText
        } else {
            // Remove spaces and newlines
            ""
        }
    }
}

fun setupLogger(logger: Logger, level: Logger.LogLevel = Logger.LogLevel.VERBOSE) {
    LoggerManager.setLogger(logger = logger, level = level)
}
