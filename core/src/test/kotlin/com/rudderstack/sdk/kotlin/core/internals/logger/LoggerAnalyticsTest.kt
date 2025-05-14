package com.rudderstack.sdk.kotlin.core.internals.logger

import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

class LoggerAnalyticsTest {

    @Test
    fun `given kotlin logger is provided and log level is VERBOSE, when all types of logs are called, then it should log all types of logs`() {
        val logger = spyk(KotlinLogger())
        LoggerAnalytics.setLogger(logger)
        LoggerAnalytics.logLevel = Logger.LogLevel.VERBOSE

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
    fun `given kotlin logger is provided and log level is INFO, when all types of logs are called, then it should log only INFO, WARN and ERROR logs`() {
        val logger = spyk(KotlinLogger())
        LoggerAnalytics.setLogger(logger)
        LoggerAnalytics.logLevel = Logger.LogLevel.INFO

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
    fun `given kotlin logger is provided and log level is NONE, when all types of logs are called, then it should not log any logs`() {
        val logger = spyk(KotlinLogger())
        LoggerAnalytics.setLogger(logger)
        LoggerAnalytics.logLevel = Logger.LogLevel.NONE

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
