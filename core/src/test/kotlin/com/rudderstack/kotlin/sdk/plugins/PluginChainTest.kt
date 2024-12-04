package com.rudderstack.kotlin.sdk.plugins

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.plugins.PluginChain
import com.rudderstack.kotlin.sdk.internals.plugins.PluginInteractor
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class PluginChainTest {

    private lateinit var pluginChain: PluginChain
    private val analytics: Analytics = mockk(relaxed = true)
    private val preProcessInteractor: PluginInteractor = mockk(relaxed = true)
    private val onProcessInteractor: PluginInteractor = mockk(relaxed = true)
    private val destinationInteractor: PluginInteractor = mockk(relaxed = true)
    private val pluginList = mapOf(
        Plugin.PluginType.PreProcess to preProcessInteractor,
        Plugin.PluginType.OnProcess to onProcessInteractor,
        Plugin.PluginType.Destination to destinationInteractor
    )

    @Before
    fun setup() {
        pluginChain = PluginChain(pluginList)
        pluginChain.analytics = analytics
    }

    @Test
    fun `when process called, then it should execute plugins in correct order`() = runTest {
        val initialMessage: Message = mockk(relaxed = true)
        val preProcessedMessage: Message = mockk(relaxed = true)
        val onProcessedMessage: Message = mockk(relaxed = true)

        coEvery { preProcessInteractor.execute(initialMessage) } returns preProcessedMessage
        coEvery { onProcessInteractor.execute(preProcessedMessage) } returns onProcessedMessage

        pluginChain.process(initialMessage)

        coVerifyOrder {
            preProcessInteractor.execute(initialMessage)
            onProcessInteractor.execute(preProcessedMessage)
            destinationInteractor.execute(onProcessedMessage)
        }
    }

    @Test
    fun `when add called, then it should setup plugin and add it to correct interactor`() {
        val plugin: Plugin = mockk(relaxed = true)
        every { plugin.pluginType } returns Plugin.PluginType.PreProcess

        pluginChain.add(plugin)

        verify { plugin.setup(analytics) }
        verify { preProcessInteractor.add(plugin) }
    }

    @Test
    fun `when remove called, it should teardown plugin and remove it from all interactors`() {
        val plugin: Plugin = mockk(relaxed = true)

        every { preProcessInteractor.remove(plugin) } returns true
        every { onProcessInteractor.remove(plugin) } returns false

        pluginChain.remove(plugin)

        verify { plugin.teardown() }
        verify { preProcessInteractor.remove(plugin) }
        verify { onProcessInteractor.remove(plugin) }
    }

    @Test
    fun `when applyClosure called, then it should apply closure to all plugins`() {
        val closure: (Plugin) -> Unit = mockk(relaxed = true)

        pluginChain.applyClosure(closure)

        verify { preProcessInteractor.applyClosure(closure) }
        verify { onProcessInteractor.applyClosure(closure) }
        verify { destinationInteractor.applyClosure(closure) }
    }

    @Test
    fun `when removeAll called, it should teardown all plugins and clear interactors`() {
        pluginChain.removeAll()

        verify { preProcessInteractor.removeAll() }
        verify { onProcessInteractor.removeAll() }
        verify { destinationInteractor.removeAll() }
    }
}
