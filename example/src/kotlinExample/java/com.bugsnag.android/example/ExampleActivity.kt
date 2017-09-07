package com.bugsnag.android.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.bugsnag.android.*
import com.bugsnag.android.other.Other
import java.util.*

class ExampleActivity : AppCompatActivity() {

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        setupToolbarLogo()

        // Execute some code before every bugsnag notification
        Bugsnag.beforeNotify { error ->
            println(String.format("In beforeNotify - %s", error.exceptionName))
            true
        }

        // Set the user information
        Bugsnag.setUser("123456", "james@example.com", "James Smith")

        Bugsnag.setProjectPackages("com.bugsnag.android.example", "com.bugsnag.android.other")

        // Add some global metaData
        Bugsnag.addToTab("user", "age", 31)
        Bugsnag.addToTab("custom", "account", "something")

        Bugsnag.leaveBreadcrumb("onCreate", BreadcrumbType.NAVIGATION, HashMap<String, String>())

        Thread(object : Runnable {
            override fun run() {
                try {
                    sleepSoundly()
                } catch (e: java.lang.InterruptedException) {
                }
            }

            @Throws(java.lang.InterruptedException::class)
            private fun sleepSoundly() {
                Thread.sleep(100000)
            }
        }).start()
    }

    private fun setupToolbarLogo() {
        val actionBar = supportActionBar
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setIcon(R.drawable.ic_bugsnag_svg)
    }

    fun sendErrorWithCallback(callback: Callback) {
        Bugsnag.notify(RuntimeException(), callback)
    }

    fun sendError(view: View) {
        actuallySendError()
    }

    private fun actuallySendError() {
        Bugsnag.notify(RuntimeException("Non-fatal error"), Severity.ERROR)
        Toast.makeText(this, "Sent error", LENGTH_SHORT).show()
    }

    fun sendWarning(view: View) {
        actuallySendWarning()
    }

    private fun actuallySendWarning() {
        Bugsnag.notify(RuntimeException("Non-fatal warning"), Severity.WARNING)
        Toast.makeText(this, "Sent warning", LENGTH_SHORT).show()
    }

    fun sendInfo(view: View) {
        Bugsnag.notify(RuntimeException("Non-fatal info"), Severity.INFO)
        Toast.makeText(this, "Sent info", LENGTH_SHORT).show()
    }

    fun sendErrorWithMetaData(view: View) {
        val nested = HashMap<String, String>()
        nested.put("normalkey", "normalvalue")
        nested.put("password", "s3cr3t")

        val list = ArrayList<Map<String, String>>()
        list.add(nested)

        val metaData = MetaData()
        metaData.addToTab("user", "payingCustomer", true)
        metaData.addToTab("user", "password", "p4ssw0rd")
        metaData.addToTab("user", "credentials", nested)
        metaData.addToTab("user", "more", list)

        Bugsnag.notify(RuntimeException("Non-fatal error with metaData"), Severity.ERROR, metaData)
        Toast.makeText(this, "Sent error with metaData", LENGTH_SHORT).show()
    }

    fun crash(view: View) {
        val other = Other()
        other.meow()
    }

    fun readDocs(view: View) {
        val uri = Uri.parse("https://docs.bugsnag.com/platforms/android/sdk/")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}
