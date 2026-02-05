package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.NativeOutOfMemoryPlugin
import java.nio.ByteBuffer
import kotlin.random.Random

private const val MB = 1024 * 1024

class NativeOOMHandlerScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    init {
        config.addPlugin(NativeOutOfMemoryPlugin())
    }

    override fun startScenario() {
        super.startScenario()

        val buffers = mutableListOf<ByteBuffer>()
        while (true) {
            val newBuffer = ByteBuffer.allocate(MB)
            Random.nextBytes(newBuffer.array())
            buffers.add(newBuffer)
        }
    }
}
