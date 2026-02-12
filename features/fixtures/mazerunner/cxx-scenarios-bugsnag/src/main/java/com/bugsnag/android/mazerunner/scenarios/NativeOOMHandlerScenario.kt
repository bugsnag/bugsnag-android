package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.NativeOutOfMemoryPlugin
import java.util.LinkedList

class NativeOOMHandlerScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    private val queue = LinkedList<Array<String>>()

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

        while (true) {
            val array = Array(Int.MAX_VALUE) {
                val input = "It's Supercalifragilisticexpialidocious! \n" +
                    "Even though the memory allocation\n" +
                    "Is really quite atrocious "
                String(input.toByteArray()) // ensures new object created
            }

            queue.add(array)
        }
    }
}
