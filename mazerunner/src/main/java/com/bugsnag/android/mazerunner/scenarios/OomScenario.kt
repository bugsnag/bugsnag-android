package com.bugsnag.android.mazerunner.scenarios

import java.util.*

/**
 * Triggers an OutOfMemoryError by allocating new Strings and retaining references
 */
internal class OomScenario : Scenario() {

    private val queue = LinkedList<Array<String>>()

    override fun run() {
        while (true) {
            val array = Array(Int.MAX_VALUE, {
                val input = "It's Supercalifragilisticexpialidocious! \n" +
                        "Even though the memory allocation\n" +
                        "Is really quite atrocious "
                String(input.toByteArray()) // ensures new object created
            })
            queue.add(array)
        }
    }

}
