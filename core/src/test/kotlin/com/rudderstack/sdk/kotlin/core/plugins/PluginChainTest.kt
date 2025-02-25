package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginInteractor
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    @BeforeEach
    fun setup() {
        pluginChain = PluginChain(pluginList)
        pluginChain.analytics = analytics
    }

    @Test
    fun `when process called, then it should execute plugins in correct order`() = runTest {
        val initialEvent: Event = mockk(relaxed = true)
        val preProcessedEvent: Event = mockk(relaxed = true)
        val onProcessedEvent: Event = mockk(relaxed = true)

        coEvery { preProcessInteractor.execute(initialEvent) } returns preProcessedEvent
        coEvery { onProcessInteractor.execute(preProcessedEvent) } returns onProcessedEvent

        pluginChain.process(initialEvent)

        coVerifyOrder {
            preProcessInteractor.execute(initialEvent)
            onProcessInteractor.execute(preProcessedEvent)
            destinationInteractor.execute(onProcessedEvent)
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
