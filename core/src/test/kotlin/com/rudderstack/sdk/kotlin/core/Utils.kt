package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.policies.DEFAULT_FLUSH_INTERVAL_IN_MILLIS
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import java.io.BufferedReader

const val ANONYMOUS_ID = "<anonymous-id>"

fun mockAnalytics(testScope: TestScope, testDispatcher: TestDispatcher): Analytics {
    val mock = mockk<Analytics>(relaxed = true)
    every { mock.analyticsScope } returns testScope
    every { mock.analyticsDispatcher } returns testDispatcher
    every { mock.storageDispatcher } returns testDispatcher
    every { mock.networkDispatcher } returns testDispatcher
    return mock
}

fun Event.applyMockedValues() {
    this.originalTimestamp = "<original-timestamp>"
    this.context = emptyJsonObject
    this.messageId = "<message-id>"
}

internal fun provideOnlyAnonymousIdState(): UserIdentity {
    return UserIdentity(
        anonymousId = ANONYMOUS_ID,
        userId = String.empty(),
        traits = emptyJsonObject,
        externalIds = emptyList()
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
fun TestDispatcher.advanceTimeBy(timeInMillis: Long = DEFAULT_FLUSH_INTERVAL_IN_MILLIS) {
    this.scheduler.advanceTimeBy(timeInMillis)
    this.scheduler.runCurrent()
}

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
    LoggerAnalytics.setup(logger = logger, logLevel = level)
}

// As Mockk doesn't seems to support spying on lambda function, we need to create a class for the same.
class Block {

    fun execute() {
        // Do nothing
    }

    fun executeAndThrowException() {
        throw Exception("Exception occurred")
    }
}

fun provideSpyBlock(): Block {
    return spyk(Block())
}
