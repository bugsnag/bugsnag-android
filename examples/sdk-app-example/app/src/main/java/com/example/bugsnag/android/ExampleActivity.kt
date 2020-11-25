package com.example.bugsnag.android

import android.os.Bundle
import android.view.View

class ExampleActivity : BaseCrashyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.multi_process_header).visibility = View.VISIBLE
        findViewById<View>(R.id.multi_process_start_btn).visibility = View.VISIBLE
    }
}
