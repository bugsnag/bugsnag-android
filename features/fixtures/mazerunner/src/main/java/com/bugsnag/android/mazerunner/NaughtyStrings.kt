package com.bugsnag.android.mazerunner

import com.bugsnag.android.Bugsnag

fun addNaughtyStringMetadata(clz: Class<Any>) {
    val stream = clz.classLoader!!.getResourceAsStream("naughty_strings.txt")
    var count = 1
    stream.reader().forEachLine { line ->
        Bugsnag.addMetadata("custom", "val_$count", line)
        count += 1
    }
}
