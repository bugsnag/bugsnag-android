package com.example.bugsnag.android

import android.os.Bundle
import android.view.View

class MultiProcessActivity : BaseCrashyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.multi_process_title).visibility = View.VISIBLE
    }
}
