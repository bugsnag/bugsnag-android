package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery
import com.bugsnag.android.mazerunner.log

/**
 * Sends an exception after pausing the session
 */
internal class SessionStoppingScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    private var stateMachine: ScenarioState = ScenarioState.FIRST_SESSION

    init {
        config.delivery = InterceptingDelivery(createDefaultDelivery()) { status ->
            check(status == DeliveryStatus.DELIVERED) {
                "Request failed, aborting scenario. status=$status"
            }

            // a request has been delivered, trigger the next action that sends a request
            if (stateMachine != ScenarioState.END) {
                stateMachine = stateMachine.performAction()
                log("Running scenario state $stateMachine")
            }
        }
    }

    override fun startScenario() {
        super.startScenario()
        // initiate sending requests to bugsnag, which triggers the state machine
        log("Sending initial session")
        Bugsnag.startSession()
    }

    /**
     * A simple state machine for the scenario to ensure that requests are
     * received in a deterministic order.
     *
     * The scenario sends the following requests:
     *
     * 1. Start a session
     * 2. Send an error with the session
     * 3. Pause the session and send an error without the session
     * 4. Resume the session and send an error with the session
     * 5. Start a new session
     * 6. Send an error with the new session
     */
    private enum class ScenarioState {
        FIRST_SESSION {
            override fun performAction(): ScenarioState {
                Bugsnag.notify(RuntimeException("First error with session"))
                return ERROR_WITH_PAUSED_SESSION
            }
        },
        ERROR_WITH_PAUSED_SESSION {
            override fun performAction(): ScenarioState {
                Bugsnag.pauseSession()
                Bugsnag.notify(RuntimeException("Second error with paused session"))
                return ERROR_WITH_RESUMED_SESSION
            }
        },
        ERROR_WITH_RESUMED_SESSION {
            override fun performAction(): ScenarioState {
                Bugsnag.resumeSession()
                Bugsnag.notify(RuntimeException("Third error with resumed session"))
                return SECOND_SESSION
            }
        },
        SECOND_SESSION {
            override fun performAction(): ScenarioState {
                Bugsnag.startSession()
                return ERROR_WITH_SECOND_SESSION
            }
        },
        ERROR_WITH_SECOND_SESSION {
            override fun performAction(): ScenarioState {
                Bugsnag.notify(RuntimeException("Fourth error with new session"))
                return END
            }
        },
        END {
            @Suppress("UseCheckOrError")
            override fun performAction(): ScenarioState =
                throw IllegalStateException("One too many delivery attempts")
        };

        /**
         * Performs an action that sends a request to Bugsnag, then returns the next state
         * that can send a request.
         */
        abstract fun performAction(): ScenarioState
    }
}
