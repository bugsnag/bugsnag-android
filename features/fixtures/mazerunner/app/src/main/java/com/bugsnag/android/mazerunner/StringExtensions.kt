package com.bugsnag.android.mazerunner

import kotlin.math.max

val String.width
    get() =
        lineSequence().fold(0) { maxWidth, line -> max(maxWidth, line.length) }
