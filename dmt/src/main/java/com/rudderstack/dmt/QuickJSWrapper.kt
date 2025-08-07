package com.rudderstack.dmt

import com.shiqi.quickjs.QuickJS

class QuickJSWrapper {

    private val quickJS = QuickJS.Builder().build()

    fun processEvent(event: String, script: String): String {
        return quickJS.createJSRuntime().use { runtime ->
            runtime.createJSContext().use { context ->

                // Evaluate the transformation function
                context.evaluate(script, "some.js")

                // Parse JSON in JavaScript and execute the transformation
                val escapedJson = event
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")

                val transformCall = "JSON.stringify(transformEvent(JSON.parse('$escapedJson')));"
                context.evaluate(transformCall, "some.js", String::class.java)
            }
        }
    }
}
