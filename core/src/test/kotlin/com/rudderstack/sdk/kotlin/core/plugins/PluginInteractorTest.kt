package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

class PluginInteractorTest {

    private lateinit var pluginInteractor: PluginInteractor
    private val pluginList: CopyOnWriteArrayList<Plugin> = CopyOnWriteArrayList()

    @BeforeEach
    fun setup() {
        pluginInteractor = PluginInteractor(pluginList)
    }

    @Test
    fun `when add called, it should add plugin to list`() {
        val plugin: Plugin = mockk(relaxed = true)

        pluginInteractor.add(plugin)

        assert(pluginList.contains(plugin))
    }

    @Test
    fun `when remove called, it should remove plugin from list`() {
        val plugin: Plugin = mockk(relaxed = true)
        pluginList.add(plugin)

        pluginInteractor.remove(plugin)

        assert(!pluginList.contains(plugin))
    }

    @Test
    fun `when removeAll called, it should clear the plugin list`() {
        val plugin: Plugin = mockk(relaxed = true)
        pluginList.add(plugin)

        pluginInteractor.removeAll()

        assert(pluginList.isEmpty())
    }

    @Test
    fun `when execute called, it should execute all plugins in order`() = runTest {
        val event: Event = mockk(relaxed = true)
        val plugin1: Plugin = mockk(relaxed = true)
        val plugin2: Plugin = mockk(relaxed = true)
        val modifiedEvent: Event = mockk(relaxed = true)

        every { event.copy<Event>() } returns event
        every { modifiedEvent.copy<Event>() } returns modifiedEvent

        coEvery { plugin1.intercept(event) } returns modifiedEvent
        coEvery { plugin2.intercept(modifiedEvent) } returns modifiedEvent

        pluginList.add(plugin1)
        pluginList.add(plugin2)

        val result = pluginInteractor.execute(event)

        coVerifyOrder {
            plugin1.intercept(event)
            plugin2.intercept(modifiedEvent)
        }
        assertEquals(modifiedEvent, result)
    }

    // The plugin contract lets intercept return a newly constructed event rather than the one it
    // was handed. Such an event cannot carry the SDK's own bookkeeping - the fields are not
    // constructor parameters - so the chain has to re-establish them, or they are lost silently:
    // no compile error, no crash, just consent enforcement and override detection quietly failing.
    @Test
    fun `given a plugin returning a replacement event, when executed, then sdk-owned fields survive`() = runTest {
        val captured = mapOf("consentManagement" to JsonPrimitive("captured-at-creation"))
        val original = realEvent().also {
            it.options = RudderOption(customContext = buildJsonObject { put("library", "injected") })
            it.capturedReservedContext = captured
        }
        pluginList.add(ReplacingPlugin())

        val result = pluginInteractor.execute(original)

        assertEquals(original.messageId, result?.messageId, "messageId was not preserved")
        assertEquals(captured, result?.capturedReservedContext, "capturedReservedContext was not preserved")
        assertEquals(original.options.customContext, result?.options?.customContext, "options were not preserved")
    }

    @Test
    fun `given a plugin returning the same event, when executed, then it is passed through untouched`() = runTest {
        val original = realEvent()
        pluginList.add(object : Plugin {
            override val pluginType = Plugin.PluginType.PreProcess
            override lateinit var analytics: com.rudderstack.sdk.kotlin.core.Analytics
            override suspend fun intercept(event: Event): Event = event
        })

        val result = pluginInteractor.execute(original)

        assertEquals(original.messageId, result?.messageId)
    }

    @Test
    fun `when applyClosure called, it should apply closure to all plugins`() {
        val plugin1: Plugin = mockk(relaxed = true)
        val plugin2: Plugin = mockk(relaxed = true)
        val closure: (Plugin) -> Unit = mockk(relaxed = true)

        pluginList.add(plugin1)
        pluginList.add(plugin2)

        pluginInteractor.applyClosure(closure)

        coVerify { closure(plugin1) }
        coVerify { closure(plugin2) }
    }

    @Test
    fun `when find called with existing plugin, it should return the plugin`() {
        val plugin: Plugin = mockk(relaxed = true)
        val pluginClass: KClass<Plugin> = Plugin::class

        pluginList.add(plugin)

        val result = pluginInteractor.find(pluginClass)

        assertEquals(plugin, result)
    }

    @Test
    fun `when find called with non-existing plugin, it should return null`() {
        val pluginClass: KClass<Plugin> = Plugin::class

        val result = pluginInteractor.find(pluginClass)

        assertEquals(null, result)
    }

    @Test
    fun `when findAll called, it should return all matching plugins`() {
        val plugin1: Plugin = mockk(relaxed = true)
        val plugin2: Plugin = mockk(relaxed = true)
        val pluginClass: KClass<Plugin> = Plugin::class

        pluginList.add(plugin1)
        pluginList.add(plugin2)

        val result = pluginInteractor.findAll(pluginClass)

        assert(result.size == 2)
        assert(result.containsAll(listOf(plugin1, plugin2)))
    }

    @Test
    fun `when findAll called with no matching plugins, it should return empty list`() {
        val pluginClass: KClass<Plugin> = Plugin::class

        val result = pluginInteractor.findAll(pluginClass)

        assert(result.isEmpty())
    }

    /** A real event, not a mock - the point is that `copy()` genuinely runs. */
    private fun realEvent(): Event = TrackEvent(event = "original", properties = emptyJsonObject).also {
        it.integrations = emptyJsonObject
        it.anonymousId = "anonymous-id"
        it.channel = PlatformType.Mobile
    }
}

/** A customer plugin that returns a new event instead of mutating the one it was given. */
private class ReplacingPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: com.rudderstack.sdk.kotlin.core.Analytics

    override suspend fun intercept(event: Event): Event =
        TrackEvent(event = "replaced", properties = emptyJsonObject).also {
            it.context = event.context
            it.userId = event.userId
            it.integrations = event.integrations
            it.anonymousId = event.anonymousId
            it.channel = event.channel
        }
}
