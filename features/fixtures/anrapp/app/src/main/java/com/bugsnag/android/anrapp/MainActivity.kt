package com.bugsnag.android.anrapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import kotlin.system.exitProcess

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.restart).setOnClickListener {
            val onCreateDelay =
                findViewById<EditText>(R.id.onCreateDelay).text.toString().toLong()
            val startOnBackground =
                findViewById<CheckBox>(R.id.startOnBackground).isChecked

            application.preferences
                .edit()
                .putLong("onCreateDelay", onCreateDelay)
                .putBoolean("startOnBackground", startOnBackground)
                .commit()

            exitProcess(0)
        }
    }
}