Feature: Native crash reporting with thrown objects

    Scenario: Throwing an exception in C++
        When I run "CXXExceptionScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXExceptionScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        # Fix as part of PLAT-5643
#        And the exception "errorClass" equals "PSt13runtime_error"
#        And the exception "message" equals "How about NO"
#        And the first significant stack frame methods and files should match:
#            | run_away(bool)       | | libentrypoint.so |
#            | trigger_an_exception | | libentrypoint.so |
#            | Java_com_bugsnag_android_mazerunner_scenarios_CXXExceptionScenario_crash | | libentrypoint.so |

    Scenario: Throwing an object in C++
        When I run "CXXThrowSomethingScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXThrowSomethingScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        # Fix as part of PLAT-5643
#        And the exception "errorClass" equals "i"
#        And the exception "message" equals "42"
#        And the first significant stack frame methods and files should match:
#            | run_back(int, int) | crash_abort | libentrypoint.so |
#            | throw_an_object    | | libentrypoint.so |
#            | Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowSomethingScenario_crash | | libentrypoint.so |
