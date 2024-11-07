package com.rudderstack.android.sdk.logger

import android.util.Log
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import io.mockk.every
import io.mockk.mockkStatic

import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test

class LoggerAnalyticsTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @Test
    fun `given android logger is provided and log level is VERBOSE, when all types of logs are called, then it should log all types of logs`() {
        val logger = spyk(AndroidLogger())
        LoggerAnalytics.setup(logger = logger, logLevel = Logger.LogLevel.VERBOSE)

        LoggerAnalytics.verbose("Verbose log")
        LoggerAnalytics.debug("Debug log")
        LoggerAnalytics.info("Info log")
        LoggerAnalytics.warn("Warn log")
        LoggerAnalytics.error("Error log")

        verifyOrder {
            logger.verbose("Verbose log")
            logger.debug("Debug log")
            logger.info("Info log")
            logger.warn("Warn log")
            logger.error("Error log")
        }
    }

    @Test
    fun `given android logger is provided and log level is INFO, when all types of logs are called, then it should log only INFO, WARN and ERROR logs`() {
        val logger = spyk(AndroidLogger())
        LoggerAnalytics.setup(logger = logger, logLevel = Logger.LogLevel.INFO)

        LoggerAnalytics.verbose("Verbose log")
        LoggerAnalytics.debug("Debug log")
        LoggerAnalytics.info("Info log")
        LoggerAnalytics.warn("Warn log")
        LoggerAnalytics.error("Error log")

        verifyOrder {
            logger.info("Info log")
            logger.warn("Warn log")
            logger.error("Error log")
        }

        verify(exactly = 0) {
            logger.verbose(any())
            logger.debug(any())
        }
    }

    @Test
    fun `given android logger is provided and log level is NONE, when all types of logs are called, then it should not log any logs`() {
        val logger = spyk(AndroidLogger())
        LoggerAnalytics.setup(logger = logger, logLevel = Logger.LogLevel.NONE)

        LoggerAnalytics.verbose("Verbose log")
        LoggerAnalytics.debug("Debug log")
        LoggerAnalytics.info("Info log")
        LoggerAnalytics.warn("Warn log")
        LoggerAnalytics.error("Error log")

        verify(exactly = 0) {
            logger.verbose(any())
            logger.debug(any())
            logger.info(any())
            logger.warn(any())
            logger.error(any())
        }
    }
}

