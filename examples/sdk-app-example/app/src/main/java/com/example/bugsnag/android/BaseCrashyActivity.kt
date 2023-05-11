package com.example.bugsnag.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Severity
import com.example.foo.CrashyClass
import com.google.android.material.snackbar.Snackbar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.notify
import java.io.IOException
import java.util.Date


open class BaseCrashyActivity : AppCompatActivity() {

    companion object {
        init {
            System.loadLibrary("entrypoint")
        }
    }

    private external fun crashFromCXX()

    private external fun anrFromCXX()

    private external fun notifyFromCXX()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        setupToolbarLogo()

        val view: View = findViewById(R.id.btn_fatal_crash)
        view.setOnClickListener(::crashUnhandled)

        val nativeBtn: View = findViewById(R.id.btn_native_crash)
        nativeBtn.setOnClickListener {
            crashFromCXX()
        }

        findViewById<View>(R.id.btn_anr).setOnClickListener {
            Thread.sleep(10000)
            showSnackbar()
        }

        findViewById<View>(R.id.btn_cxx_anr).setOnClickListener {
            anrFromCXX()
            showSnackbar()
        }
    }

    override fun onResume() {
        val members = listOf(
            mapOf("Group Members 1" to "Adam"),
            mapOf("Group Members 2" to "Alice")
        )
        val lastResumeTime = mapOf("Last Resume Time" to Date())

        Bugsnag.addMetadata("Custom Data", "members", members)
        Bugsnag.addMetadata("Last Resume Time", "Member Last Resume Time", lastResumeTime)

        super.onResume()
    }


    /**
     * Throws an unhandled Exception. Bugsnag will automatically capture any uncaught exceptions
     * in your app and send an error report.
     */
    @Suppress("UNUSED_PARAMETER")
    fun crashUnhandled(view: View) {
        throw CrashyClass.crash("Fatal Crash")
    }

    /**
     * You can call [Bugsnag.notify] to send an error report for exceptions
     * which are already handled by your app.
     */
    @Suppress("UNUSED_PARAMETER")
    fun crashHandled(view: View) {
        try {
            throw RuntimeException("Non-Fatal Crash")
        } catch (e: RuntimeException) {
            Bugsnag.notify(e) {
                showSnackbar()
                true
            }
        }
    }

    /**
     * Delivers an error notification from native (C/C++) code
     */
    @Suppress("UNUSED_PARAMETER")
    fun notifyNativeHandled(view: View) {
        notifyFromCXX()
        showSnackbar()
    }

    /**
     * The severity of error reports can be altered. This can be useful for capturing handled
     * exceptions which occur often but are not visible to the user.
     */
    @Suppress("UNUSED_PARAMETER")
    fun crashWithCustomSeverity(view: View) {
        val e = RuntimeException("Error Report with altered Severity")
        Bugsnag.notify(e) {
            it.severity = Severity.ERROR
            showSnackbar()
            true
        }
    }

    /**
     * User details can be added globally, which will then appear in all error reports sent
     * to the Bugsnag dashboard.
     */
    @Suppress("UNUSED_PARAMETER")
    fun crashWithUserDetails(view: View) {
        Bugsnag.addFeatureFlag("Report User Details", "User Details")
        Bugsnag.setUser("123456", "joebloggs@example.com", "Joe Bloggs")
        val e = RuntimeException("Error Report with User Info")
        Bugsnag.notify(e) {
            showSnackbar()
            true
        }
    }

    /**
     * Additional metadata can be attached to crash reports. This can be achieved by calling
     * [Bugsnag.notify], as shown below, or registering a global callback
     * with [Configuration.addOnError] that adds metadata to the report.
     */
    @Suppress("UNUSED_PARAMETER")
    fun crashWithMetadata(view: View) {
        val e = RuntimeException("Error report with Additional Metadata")

        Bugsnag.notify(e) { event ->
            event.severity = Severity.ERROR
            event.addMetadata("CustomMetadata", "HasLaunchedGameTutorial", true)
            showSnackbar()
            true
        }
    }

    /**
     * Breadcrumbs help track down the cause of an error, by displaying events that happened leading
     * up to a crash. You can log your own breadcrumbs which will display on the Bugsnag Dashboard -
     * activity lifecycle callbacks and system intents are also captured automatically.
     */
    @Suppress("UNUSED_PARAMETER")
    fun crashWithBreadcrumbs(view: View) {
        Bugsnag.leaveBreadcrumb("LoginButtonClick")

        val metadata = mapOf(Pair("reason", "incorrect password"))
        Bugsnag.leaveBreadcrumb("WebAuthFailure", metadata, BreadcrumbType.ERROR)

        val e = RuntimeException("Error Report with Breadcrumbs")
        Bugsnag.notify(e) {
            showSnackbar()
            true
        }
    }

    /**
     * When sending a handled error, a callback can be registered, which allows the Error Report
     * to be modified before it is sent.
     */
    @Suppress("UNUSED_PARAMETER")
    fun crashWithCallback(view: View) {
        val e = RuntimeException("Customized Error Report")

        Bugsnag.notify(e) { event ->
            // modify the report
            val completedLevels = listOf("Level 1 - The Beginning", "Level 2 - Tower Defence")
            val userDetails = hashMapOf("playerName" to "Joe Bloggs the Invincible")

            event.addMetadata("CustomMetadata", "HasLaunchedGameTutorial", true)
            event.addMetadata("CustomMetadata", "UserDetails", userDetails)
            event.addMetadata("CustomMetadata", "CompletedLevels", completedLevels)
            showSnackbar()
            true
        }
    }

    /**
     * Starts an activity in a different process.
     */
    @Suppress("UNUSED_PARAMETER")
    fun startMultiProcessActivity(view: View) {
        startActivity(Intent(this, MultiProcessActivity::class.java))
    }

    private fun showSnackbar() {
        val rootView = findViewById<View>(R.id.example_root)
        Snackbar.make(rootView, getString(R.string.trigger_err_msg), Snackbar.LENGTH_SHORT).show()
    }

    private fun setupToolbarLogo() {
        val supportActionBar = supportActionBar

        if (supportActionBar != null) {
            supportActionBar.setDisplayShowHomeEnabled(true)
            supportActionBar.setIcon(R.drawable.ic_bugsnag_svg)
            supportActionBar.title = null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun readDocs(view: View) {
        val uri = Uri.parse("https://docs.bugsnag.com/platforms/android/sdk/")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    /**
     * Network call information will appear in all error reports sent
     * to the Bugsnag dashboard.
     */
    @Suppress("UNUSED_PARAMETER")
    fun notifyNetworkCallComplete() {
        Bugsnag.notify(IOException("Network Failure")) {
            runOnUiThread { showSnackbar() }
            true
        }
    }

    fun networkExceptionWithBreadcrumbs(view: View) {
        val httpClient = (application as ExampleApplication).httpClient

        val call = httpClient.newCall(
            Request.Builder()
                .url("https://android.com")
                .build()
        )

        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.d("ExampleApp", "Read ${response.body?.bytes()?.size} bytes")
                notifyNetworkCallComplete()
            }

            override fun onFailure(call: Call, e: IOException) {
                notifyNetworkCallComplete()
            }
        })
    }

}
