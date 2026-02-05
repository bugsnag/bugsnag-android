package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.NativeOutOfMemoryPlugin
import com.bugsnag.android.mazerunner.log
import java.nio.ByteBuffer
import kotlin.random.Random

private const val MB = 1024 * 1024
private const val MB10 = 10 * MB

class NativeOOMHandlerScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios-bugsnag")
        }
    }

    init {
        config.addPlugin(NativeOutOfMemoryPlugin())
    }

    external fun configure()

    override fun startScenario() {
        super.startScenario()

        configure()

        val buffers = mutableListOf<ByteBuffer>()
        while (true) {
            val newBuffer = ByteBuffer.allocate(MB10)
            Random.nextBytes(newBuffer.array())
            buffers.add(newBuffer)

            log("Allocated 10mb of memory. Now retaining ${buffers.size * 10}mb of memory")
        }
    }
}
