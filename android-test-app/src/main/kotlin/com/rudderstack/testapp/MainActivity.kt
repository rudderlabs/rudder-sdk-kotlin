package com.rudderstack.testapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Minimal launcher Activity. Exists so the SUT has a visible entry point that
 * `am start` can target, and so Android process lifecycle behaves as it would in a
 * real app. No business logic, no UI beyond a label.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = "RudderStack Test SUT"
                textSize = 18f
                setPadding(48, 48, 48, 48)
            }
        )
    }
}
