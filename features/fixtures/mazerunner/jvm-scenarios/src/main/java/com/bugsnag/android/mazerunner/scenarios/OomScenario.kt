package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import java.util.LinkedList

/**
 * Triggers an OutOfMemoryError by allocating new Strings and retaining references
 */
internal class OomScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    private val queue = LinkedList<Array<String>>()

    init {
        config.enabledErrorTypes.anrs = false
    }

    override fun startScenario() {
        super.startScenario()
        while (true) {
            val array = Array(
                Int.MAX_VALUE,
                {
                    val input = "It's Supercalifragilisticexpialidocious! \n" +
                        "Even though the memory allocation\n" +
                        "Is really quite atrocious "
                    String(input.toByteArray()) // ensures new object created
                }
            )
            queue.add(array)
        }
    }
}
