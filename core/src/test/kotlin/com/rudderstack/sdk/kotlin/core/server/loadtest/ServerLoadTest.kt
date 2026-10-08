package com.rudderstack.sdk.kotlin.core.server.loadtest

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.server.DropReason
import com.rudderstack.sdk.kotlin.core.server.FakeDataPlane
import com.rudderstack.sdk.kotlin.core.server.ReceivedBatch
import com.rudderstack.sdk.kotlin.core.server.ServerAnalytics
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.system.exitProcess

private const val DEFAULT_EVENTS_PER_SECOND = 10_000
private const val DEFAULT_DURATION_IN_SECONDS = 300
private const val PRODUCER_THREADS = 4
private const val TICK_IN_MILLIS = 10L
private const val TICKS_PER_SECOND = 100
private const val USER_COUNT = 1_000
private const val CREATED_AT = "createdAt"
private const val MAX_TRACKED_DELAY_IN_MILLIS = 120_000
private const val SHUTDOWN_TIMEOUT_IN_MILLIS = 60_000L
private const val PROGRESS_INTERVAL_IN_SECONDS = 10
private const val BYTES_IN_MEGABYTE = 1024 * 1024
private const val MILLIS_IN_SECOND = 1000L
private const val NANOS_IN_MILLI = 1_000_000L

/**
 * A load test for server mode. It is a manual tool and is not part of CI.
 *
 * Run: `./gradlew :core:serverLoadTest -PloadTestEventsPerSecond=10000 -PloadTestDurationSeconds=300`
 */
fun main(args: Array<String>) {
    val eventsPerSecond = args.getOrNull(0)?.toIntOrNull() ?: DEFAULT_EVENTS_PER_SECOND
    val durationInSeconds = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_DURATION_IN_SECONDS

    val hasPassed = ServerLoadTest(eventsPerSecond, durationInSeconds).run()

    exitProcess(if (hasPassed) 0 else 1)
}

private class ServerLoadTest(private val eventsPerSecond: Int, private val durationInSeconds: Int) {

    private val sentEvents = AtomicLong()
    private val receivedEvents = AtomicLong()
    private val receivedBatches = AtomicLong()
    private val receivedCharacters = AtomicLong()
    private val minEventsInBatch = AtomicInteger(Int.MAX_VALUE)
    private val maxEventsInBatch = AtomicInteger()
    private val delayHistogram = AtomicLongArray(MAX_TRACKED_DELAY_IN_MILLIS + 1)
    private val maxUsedHeapInBytes = AtomicLong()
    private val drops = ConcurrentHashMap<DropReason, AtomicLong>()

    fun run(): Boolean = FakeDataPlane(onBatch = ::record).use { dataPlane ->
        val serverAnalytics = ServerAnalytics(
            ServerConfiguration(
                writeKey = "<write-key>",
                dataPlaneUrl = dataPlane.url,
                logLevel = Logger.LogLevel.WARN,
                dropListener = { reason, eventCount ->
                    drops.computeIfAbsent(reason) { AtomicLong() }.addAndGet(eventCount.toLong())
                },
            )
        )
        println("Load test: $eventsPerSecond events per second for $durationInSeconds seconds")
        val monitor = startMonitor()

        produceEvents(serverAnalytics)
        val shutdownStartedAtInNanos = System.nanoTime()
        val isDrained = serverAnalytics.shutdownBlocking(SHUTDOWN_TIMEOUT_IN_MILLIS)
        val shutdownTimeInMillis = (System.nanoTime() - shutdownStartedAtInNanos) / NANOS_IN_MILLI
        monitor.shutdownNow()

        printReport(isDrained, shutdownTimeInMillis)
        isDrained && receivedEvents.get() == sentEvents.get() && drops.isEmpty()
    }

    private fun produceEvents(serverAnalytics: ServerAnalytics) {
        val eventsPerTick = eventsPerSecond / (TICKS_PER_SECOND * PRODUCER_THREADS)
        val producers = Executors.newScheduledThreadPool(PRODUCER_THREADS)
        repeat(PRODUCER_THREADS) {
            producers.scheduleAtFixedRate(
                { repeat(eventsPerTick) { trackOneEvent(serverAnalytics) } },
                0,
                TICK_IN_MILLIS,
                TimeUnit.MILLISECONDS,
            )
        }
        Thread.sleep(durationInSeconds * MILLIS_IN_SECOND)
        producers.shutdown()
        producers.awaitTermination(SHUTDOWN_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS)
    }

    private fun trackOneEvent(serverAnalytics: ServerAnalytics) {
        val sequence = sentEvents.incrementAndGet()
        serverAnalytics.track(
            name = "Load Test Event",
            userId = "user-${sequence % USER_COUNT}",
            properties = buildJsonObject {
                put(CREATED_AT, System.currentTimeMillis())
                put("sequence", sequence)
            },
        )
    }

    private fun record(batch: ReceivedBatch) {
        val receivedAtInMillis = System.currentTimeMillis()
        val eventCount = batch.events.size
        receivedBatches.incrementAndGet()
        receivedEvents.addAndGet(eventCount.toLong())
        receivedCharacters.addAndGet(batch.body.length.toLong())
        minEventsInBatch.accumulateAndGet(eventCount, ::minOf)
        maxEventsInBatch.accumulateAndGet(eventCount, ::maxOf)
        batch.events.forEach { event ->
            val createdAtInMillis = event.getValue("properties").jsonObject.getValue(CREATED_AT).jsonPrimitive.long
            val delayInMillis = (receivedAtInMillis - createdAtInMillis).coerceIn(0, MAX_TRACKED_DELAY_IN_MILLIS.toLong())
            delayHistogram.incrementAndGet(delayInMillis.toInt())
        }
    }

    private fun startMonitor() = Executors.newSingleThreadScheduledExecutor().apply {
        val elapsedSeconds = AtomicInteger()
        scheduleAtFixedRate(
            {
                val usedHeapInBytes = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
                maxUsedHeapInBytes.accumulateAndGet(usedHeapInBytes, ::maxOf)
                if (elapsedSeconds.incrementAndGet() % PROGRESS_INTERVAL_IN_SECONDS == 0) {
                    println(
                        "  ${elapsedSeconds.get()} s: sent=${sentEvents.get()} received=${receivedEvents.get()} " +
                            "heap=${usedHeapInBytes / BYTES_IN_MEGABYTE} MB drops=$drops"
                    )
                }
            },
            1,
            1,
            TimeUnit.SECONDS,
        )
    }

    private fun printReport(isDrained: Boolean, shutdownTimeInMillis: Long) {
        val batches = receivedBatches.get().coerceAtLeast(1)
        println(
            """
            |Load test report
            |  events sent:             ${sentEvents.get()} (${sentEvents.get() / durationInSeconds} per second)
            |  events received:         ${receivedEvents.get()}
            |  events dropped:          $drops
            |  events not accounted:    ${sentEvents.get() - receivedEvents.get() - drops.values.sumOf { it.get() }}
            |  batches:                 ${receivedBatches.get()}
            |  events per batch:        min ${minEventsInBatch.get()}, mean ${receivedEvents.get() / batches}, max ${maxEventsInBatch.get()}
            |  mean batch size:         ${receivedCharacters.get() / batches / 1024} KB (JSON, before GZIP)
            |  upload delay:            p50 ${delayAtPercentile(50)} ms, p95 ${delayAtPercentile(95)} ms, p99 ${delayAtPercentile(99)} ms, max ${delayAtPercentile(100)} ms
            |  max used heap:           ${maxUsedHeapInBytes.get() / BYTES_IN_MEGABYTE} MB of ${Runtime.getRuntime().maxMemory() / BYTES_IN_MEGABYTE} MB
            |  shutdown:                drained=$isDrained in $shutdownTimeInMillis ms
            """.trimMargin()
        )
    }

    private fun delayAtPercentile(percentile: Int): Int {
        val targetCount = receivedEvents.get() * percentile / 100
        var cumulativeCount = 0L
        for (delayInMillis in 0..MAX_TRACKED_DELAY_IN_MILLIS) {
            cumulativeCount += delayHistogram.get(delayInMillis)
            if (cumulativeCount >= targetCount && cumulativeCount > 0) return delayInMillis
        }
        return MAX_TRACKED_DELAY_IN_MILLIS
    }
}
