package com.rudderstack.sdk.kotlin.core.internals.logger

import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

class AnalyticsLoggerTest {

    @Test
    fun `given log level is VERBOSE, when all types of logs are called, then it should log all types of logs`() {
        val logger = spyk(KotlinLogger())
        val analyticsLogger = AnalyticsLogger(logger = logger, logLevel = Logger.LogLevel.VERBOSE)

        analyticsLogger.verbose("Verbose log")
        analyticsLogger.debug("Debug log")
        analyticsLogger.info("Info log")
        analyticsLogger.warn("Warn log")
        analyticsLogger.error("Error log", null)

        verifyOrder {
            logger.verbose("Verbose log")
            logger.debug("Debug log")
            logger.info("Info log")
            logger.warn("Warn log")
            logger.error("Error log", null)
        }
    }

    @Test
    fun `given log level is DEBUG, when all types of logs are called, then it should log only DEBUG, INFO, WARN and ERROR logs`() {
        val logger = spyk(KotlinLogger())
        val analyticsLogger = AnalyticsLogger(logger = logger, logLevel = Logger.LogLevel.DEBUG)

        analyticsLogger.verbose("Verbose log")
        analyticsLogger.debug("Debug log")
        analyticsLogger.info("Info log")
        analyticsLogger.warn("Warn log")
        analyticsLogger.error("Error log", null)

        verifyOrder {
            logger.debug("Debug log")
            logger.info("Info log")
            logger.warn("Warn log")
            logger.error("Error log", null)
        }

        verify(exactly = 0) {
            logger.verbose(any())
        }
    }

    @Test
    fun `given log level is INFO, when all types of logs are called, then it should log only INFO, WARN and ERROR logs`() {
        val logger = spyk(KotlinLogger())
        val analyticsLogger = AnalyticsLogger(logger = logger, logLevel = Logger.LogLevel.INFO)

        analyticsLogger.verbose("Verbose log")
        analyticsLogger.debug("Debug log")
        analyticsLogger.info("Info log")
        analyticsLogger.warn("Warn log")
        analyticsLogger.error("Error log", null)

        verifyOrder {
            logger.info("Info log")
            logger.warn("Warn log")
            logger.error("Error log", null)
        }

        verify(exactly = 0) {
            logger.verbose(any())
            logger.debug(any())
        }
    }

    @Test
    fun `given log level is WARN, when all types of logs are called, then it should log only WARN and ERROR logs`() {
        val logger = spyk(KotlinLogger())
        val analyticsLogger = AnalyticsLogger(logger = logger, logLevel = Logger.LogLevel.WARN)

        analyticsLogger.verbose("Verbose log")
        analyticsLogger.debug("Debug log")
        analyticsLogger.info("Info log")
        analyticsLogger.warn("Warn log")
        analyticsLogger.error("Error log", null)

        verifyOrder {
            logger.warn("Warn log")
            logger.error("Error log", null)
        }

        verify(exactly = 0) {
            logger.verbose(any())
            logger.debug(any())
            logger.info(any())
        }
    }

    @Test
    fun `given log level is ERROR, when all types of logs are called, then it should log only ERROR logs`() {
        val logger = spyk(KotlinLogger())
        val analyticsLogger = AnalyticsLogger(logger = logger, logLevel = Logger.LogLevel.ERROR)

        analyticsLogger.verbose("Verbose log")
        analyticsLogger.debug("Debug log")
        analyticsLogger.info("Info log")
        analyticsLogger.warn("Warn log")
        analyticsLogger.error("Error log", null)

        verify {
            logger.error("Error log", null)
        }

        verify(exactly = 0) {
            logger.verbose(any())
            logger.debug(any())
            logger.info(any())
            logger.warn(any())
        }
    }

    @Test
    fun `given log level is NONE, when all types of logs are called, then it should not log any logs`() {
        val logger = spyk(KotlinLogger())
        val analyticsLogger = AnalyticsLogger(logger = logger, logLevel = Logger.LogLevel.NONE)

        analyticsLogger.verbose("Verbose log")
        analyticsLogger.debug("Debug log")
        analyticsLogger.info("Info log")
        analyticsLogger.warn("Warn log")
        analyticsLogger.error("Error log", null)

        verify(exactly = 0) {
            logger.verbose(any())
            logger.debug(any())
            logger.info(any())
            logger.warn(any())
            logger.error(any(), any())
        }
    }

    @Test
    fun `given log level is ERROR, when error is called with throwable, then it should pass throwable to underlying logger`() {
        val logger = spyk(KotlinLogger())
        val analyticsLogger = AnalyticsLogger(logger = logger, logLevel = Logger.LogLevel.VERBOSE)
        val throwable = RuntimeException("Test exception")

        analyticsLogger.error("Error log", throwable)

        verify {
            logger.error("Error log", throwable)
        }
    }
}
