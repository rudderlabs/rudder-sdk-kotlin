package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.provideSpyBlock
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExceptionHandlingTest {

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

        val result = safelyExecute(
            block = { exceptionBlock.executeAndThrowException() },
        )

        assertNull(result)
    }
}
