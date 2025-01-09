package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.provideSpyBlock
import io.mockk.verify
import org.junit.Test

class ExceptionHandlingTest {

    @Test
    fun `given exception occurs, when certain block is executed safely, then it should not throw any exception`() {
        val exceptionBlock = provideSpyBlock()

        safelyExecute(
            block = { exceptionBlock.executeAndThrowException() },
        )

        verify { exceptionBlock.executeAndThrowException() }
    }

    @Test
    fun `given exception occurs and default block is provided, when certain block is executed safely, then default block is executed on exception`() {
        val exceptionBlock = provideSpyBlock()
        val safeBlock = provideSpyBlock()

        safelyExecute(
            block = { exceptionBlock.executeAndThrowException() },
            onException = { safeBlock.execute() },
        )

        verify { exceptionBlock.executeAndThrowException() }
        verify { safeBlock.execute() }
    }
}
