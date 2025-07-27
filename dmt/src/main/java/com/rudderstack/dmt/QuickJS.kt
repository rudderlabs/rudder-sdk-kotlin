package com.rudderstack.dmt

import com.shiqi.quickjs.QuickJS

class QuickJS {

    fun transformJson(jsonInput: String): String {
        val quickJS = QuickJS.Builder().build()
        return quickJS.createJSRuntime().use { runtime ->
            runtime.createJSContext().use { context ->
                val transformScript = """
                    function transformEvent(jsonString) {
                        try {
                            var eventData = JSON.parse(jsonString);
                            if (eventData.event) {
                                eventData.event = eventData.event + "_transform";
                            }
                            return JSON.stringify(eventData);
                        } catch (e) {
                            return jsonString;
                        }
                    }
                """.trimIndent()
                
                // Evaluate the transformation function
                context.evaluate(transformScript, "transform.js")
                
                // Execute the transformation with the input JSON
                val transformCall = "transformEvent('${jsonInput.replace("'", "\\'")}')"
                context.evaluate<String>(transformCall, "transform.js", String::class.java)
            }
        }
    }

    fun process() {
        val quickJS = QuickJS.Builder().build()
        quickJS.createJSRuntime().use { runtime ->
            runtime.createJSContext().use { context ->
                val script1 = "" +
                        "function fibonacci(n) {" +
                        "  if (n == 0 || n == 1) return n;" +
                        "  return fibonacci(n - 1) + fibonacci(n - 2);" +
                        "}"
                // Evaluate a script without return value
                context.evaluate(script1, "fibonacci.js")

                val script2 = "fibonacci(15);"
                // Evaluate a script with return value
                val result: Int = context.evaluate<Int>(script2, "fibonacci.js", Int::class.java)
                println(result)
            }
        }
    }
}
