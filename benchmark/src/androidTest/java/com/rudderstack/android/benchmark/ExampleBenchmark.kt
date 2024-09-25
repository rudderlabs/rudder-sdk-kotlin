package com.rudderstack.android.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rudderstack.android.Configuration
import com.rudderstack.core.Analytics
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    // https://developer.android.com/topic/performance/benchmarking/microbenchmark-write
    @Test
    fun log() {
        val application = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()

        val analytics = Analytics(
            configuration = Configuration(
                trackApplicationLifecycleEvents = true,
                writeKey = "<WRITE_KEY>",
                application = application,
                dataPlaneUrl = "<DATA_PLANE_URL",

                )
        )

        benchmarkRule.measureRepeated {
//            Log.d("LogBenchmark", "the cost of writing this log method will be measured")
//            Thread.sleep(5000)
            analytics.track("test event")
        }
//        benchmarkRule.
    }
}
