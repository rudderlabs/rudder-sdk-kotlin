package com.rudderstack.dmt

import com.shiqi.quickjs.QuickJS

internal class QuickJSWrapper {

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

    fun convertScript(script: String, convertorScript: String): String {
        return quickJS.createJSRuntime().use { runtime ->
            runtime.createJSContext().use { context ->

                // Evaluate the transformation function
                context.evaluate(convertorScript, "some.js")

                val codeLiteral = script
                    .replace("\\", "\\\\")
                    .replace("`", "\\`")
                    .replace("$", "\\$")

                val transformCall = "Babel.transform(`$codeLiteral`, { presets: ['es2015'] }).code;"
                context.evaluate(transformCall, "some.js", String::class.java)
            }
        }
    }
}
