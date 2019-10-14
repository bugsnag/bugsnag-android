package com.bugsnag.android.ndk

internal fun loadJson(resourceName: String) =
    NativeCXXTest::class.java.classLoader!!.getResource(resourceName).readText()
