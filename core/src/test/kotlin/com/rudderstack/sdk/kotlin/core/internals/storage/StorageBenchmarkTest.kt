package com.rudderstack.sdk.kotlin.core.internals.storage

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.storage.inmemory.InMemoryStorage
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.system.measureNanoTime

/**
 * Spike benchmark for SDK-4436. Not run by default gradle test; invoke explicitly:
 *   ./gradlew :core:test --tests "*StorageBenchmarkTest*"
 */
@Suppress("MagicNumber", "LongMethod", "PrintStackTrace", "MaxLineLength")
class StorageBenchmarkTest {

    private val mockLogger: Logger = mockk(relaxed = true)
    private val createdDirs = mutableListOf<File>()

    @BeforeEach
    fun setUp() {
        println()
        println("┌────────────────────────────────────────────────────────────────────────────┐")
        println("│ Storage benchmark                                                          │")
        println("│ payload=${PAYLOAD.length}B  warmup=$WARMUP_OPS ops  measure=$MEASURE_OPS ops                     │")
        println("└────────────────────────────────────────────────────────────────────────────┘")
    }

    @AfterEach
    fun tearDown() {
        createdDirs.forEach { it.deleteRecursively() }
        createdDirs.clear()
    }

    @Test
    fun `BasicStorage — 1 producer`() {
        val storage = newBasicStorage()
        runBenchmark("BasicStorage (file, /tmp) — 1 producer", storage, producers = 1)
    }

    @Test
    fun `BasicStorage — 4 producers`() {
        val storage = newBasicStorage()
        runBenchmark("BasicStorage (file, /tmp) — 4 producers", storage, producers = 4)
    }

    @Test
    fun `InMemoryStorage — 1 producer`() {
        val storage = newInMemoryStorage()
        runBenchmark("InMemoryStorage — 1 producer", storage, producers = 1)
    }

    @Test
    fun `InMemoryStorage — 4 producers`() {
        val storage = newInMemoryStorage()
        runBenchmark("InMemoryStorage — 4 producers", storage, producers = 4)
    }

    private fun newBasicStorage(): Storage {
        val writeKey = "bench-${UUID.randomUUID()}"
        val dir = File("/tmp/rudderstack-analytics-kotlin-bench/$writeKey")
        createdDirs += dir
        return BasicStorage(
            writeKey = writeKey,
            platformType = PlatformType.Server,
            logger = mockLogger,
            storageDirectory = dir,
        )
    }

    private fun newInMemoryStorage(): Storage {
        val writeKey = "bench-${UUID.randomUUID()}"
        return InMemoryStorage(writeKey = writeKey, logger = mockLogger)
    }

    private fun runBenchmark(name: String, storage: Storage, producers: Int) {
        val opsPerProducer = MEASURE_OPS / producers

        // Warmup — trigger JIT, populate file handles, prime caches
        runBlocking {
            repeat(WARMUP_OPS) { storage.write(StorageKeys.EVENT, PAYLOAD) }
            storage.rollover()
        }

        val latenciesPerProducer = Array(producers) { LongArray(opsPerProducer) }

        val totalNanos = measureNanoTime {
            runBlocking {
                val jobs = (0 until producers).map { p ->
                    launch(Dispatchers.Default) {
                        val latencies = latenciesPerProducer[p]
                        for (i in 0 until opsPerProducer) {
                            val start = System.nanoTime()
                            storage.write(StorageKeys.EVENT, PAYLOAD)
                            latencies[i] = System.nanoTime() - start
                        }
                    }
                }
                jobs.joinAll()
            }
        }

        val totalOps = producers * opsPerProducer
        val throughput = totalOps.toDouble() * 1_000_000_000.0 / totalNanos

        val allLatencies = LongArray(totalOps)
        var idx = 0
        for (arr in latenciesPerProducer) {
            System.arraycopy(arr, 0, allLatencies, idx, arr.size)
            idx += arr.size
        }
        allLatencies.sort()

        fun p(pct: Double): Long = allLatencies[((allLatencies.size - 1) * pct).toInt()] / 1000
        val maxUs = allLatencies.last() / 1000
        val meanUs = allLatencies.average().toLong() / 1000

        println()
        println("── $name ──")
        println("  producers      : $producers")
        println("  ops/producer   : $opsPerProducer")
        println("  total ops      : $totalOps")
        println("  wall time      : ${totalNanos / 1_000_000} ms")
        println("  throughput     : ${"%,d".format(throughput.toLong())} ops/sec")
        println("  latency (µs)   : mean=$meanUs  p50=${p(0.50)}  p95=${p(0.95)}  p99=${p(0.99)}  p99.9=${p(0.999)}  max=$maxUs")

        val target10k = throughput >= 10_000
        val target100k = throughput >= 100_000
        println("  10k/s target   : ${if (target10k) "PASS" else "FAIL"}")
        println("  100k/s target  : ${if (target100k) "PASS" else "FAIL"}")
    }

    companion object {
        private const val WARMUP_OPS = 5_000
        private const val MEASURE_OPS = 100_000

        // ~500B representative track-event payload (typical track event size).
        private val PAYLOAD: String = buildPayload()

        private fun buildPayload(): String {
            val prefix = """{"type":"track","event":"benchmark","userId":"u-12345678","anonymousId":"a-12345678","properties":{"filler":""""
            val suffix = """"},"context":{"library":{"name":"com.rudderstack.sdk.kotlin.core","version":"1.6.0"}}}"""
            val target = 500
            val fillerSize = (target - prefix.length - suffix.length).coerceAtLeast(0)
            return prefix + "x".repeat(fillerSize) + suffix
        }
    }
}
