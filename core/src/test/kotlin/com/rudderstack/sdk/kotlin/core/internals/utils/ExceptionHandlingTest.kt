package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.provideSpyBlock
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExceptionHandlingTest {

    private val mockLogger: Logger = mockk(relaxed = true)

    @Test
    fun `given exception occurs and onException block is provided, when certain block is executed safely, then onException block is executed`() {
        val block = provideSpyBlock()
        val onExceptionBlock = provideSpyBlock()

        safelyExecute(
            block = { block.executeAndThrowException() },
            onException = { onExceptionBlock.execute() },
        )

        verify { block.executeAndThrowException() }
        verify { onExceptionBlock.execute() }
    }

    @Test
    fun `given exception occurs and finally block is provided, when certain block is executed safely, then finally block is executed`() {
        val exceptionBlock = provideSpyBlock()
        val finallyBlock = provideSpyBlock()

        safelyExecute(
            block = { exceptionBlock.executeAndThrowException() },
            onException = {},
            onFinally = { finallyBlock.execute() },
        )

        verify { exceptionBlock.executeAndThrowException() }
        verify { finallyBlock.execute() }
    }

    @Test
    fun `given exception occurs, when certain block is executed safely, then exception is handled by default exception handler`() {
        val exceptionBlock = provideSpyBlock()

        val result = safelyExecute(mockLogger) {
            exceptionBlock.executeAndThrowException()
        }

        assertNull(result)
    }

    @Test
    fun `given the coroutine is cancelled, when a block is executed safely, then the cancellation is rethrown and not logged`() {
        assertThrows<CancellationException> {
            safelyExecute(mockLogger) { throw CancellationException("cancelled") }
        }

        verify(exactly = 0) { mockLogger.error(any(), any()) }
    }
}
