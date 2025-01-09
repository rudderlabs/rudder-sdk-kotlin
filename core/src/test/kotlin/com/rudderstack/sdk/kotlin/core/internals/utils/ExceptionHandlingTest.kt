package com.rudderstack.sdk.kotlin.core.internals.utils

import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class ExceptionHandlingTest {

    @Test
    fun `given exception occurs, when certain block is executed safely, then it should not throw any exception`() {
        val exceptionBlock = createSpyExceptionBlock()

        safelyExecute(
            block = { exceptionBlock.execute() },
        )

        verify { exceptionBlock.execute() }
    }

    @Test
    fun `given exception occurs and default block is provided, when certain block is executed safely, then default block is executed on exception`() {
        val exceptionBlock = createSpyExceptionBlock()
        val safeBlock = createSpySafeBlock()

        safelyExecute(
            block = { exceptionBlock.execute() },
            onException = { safeBlock.execute() },
        )

        verify { exceptionBlock.execute() }
        verify { safeBlock.execute() }
    }

    private fun createSpyExceptionBlock(): ExceptionBlock {
        return spyk(ExceptionBlock())
    }

    private fun createSpySafeBlock(): SafeBlock {
        return spyk(SafeBlock())
    }
}

// As Mockk doesn't support spying on suspend lambda function, we need to create a class for the same.
private class ExceptionBlock {

    fun execute() {
        throw Exception("Exception occurred")
    }
}

// As Mockk doesn't support spying on suspend lambda function, we need to create a class for the same.
private class SafeBlock {

    fun execute() {
        // Execute safely
    }
}
