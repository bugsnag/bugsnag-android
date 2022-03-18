package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

internal class DiscardOldEventsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
    }

    fun setEventFileTimestamp(file: File, timestamp: Long) {
        val name = file.name
        val suffix = name.substring(name.indexOf("_"))
        val newName = "" + timestamp + suffix
        val result = file.renameTo(File(file.parent, newName))
        System.out.println("### Rename " + file.canonicalPath + " to " + File(file.parent, newName).canonicalPath + ": " + result)
    }

    override fun startScenario() {
        System.out.println("### START")
        super.startScenario()
        System.out.println("### MR 1")
        Bugsnag.markLaunchCompleted()
        System.out.println("### MR 2")
        Bugsnag.notify(MyThrowable("DiscardOldEventsScenario"))
        System.out.println("### MR 3")
        Thread.sleep(700)
        System.out.println("### MR 4")
        val files = File(context.cacheDir, "bugsnag-errors").listFiles()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -60)
        cal.add(Calendar.MINUTE, -1)
        val timestamp = cal.timeInMillis
//        val simpleDateFormat = SimpleDateFormat("MMddHHmm.ss")
//        val dateString = simpleDateFormat.format(cal.time)

        for (file in files!!) {
            System.out.println("### MR 5")
            setEventFileTimestamp(file, timestamp)
//            val proc = Runtime.getRuntime().exec("touch -t " + dateString + " " + file.canonicalPath);
//            proc.waitFor()
//            if (proc.exitValue() != 0) {
//                throw RuntimeException("Touch failed")
//            }
//            file.setLastModified(timestamp)
        }
        System.out.println("### MR 6")
    }
}
