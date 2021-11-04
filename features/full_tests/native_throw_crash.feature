Feature: Native crash reporting with thrown objects

    Scenario: Throwing an exception in C++
        When I run "CXXExceptionScenario" and relaunch the app
        And I configure Bugsnag for "CXXExceptionScenario"
        And I wait to receive an error
        And the error payload contains a completed unhandled native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the exception "errorClass" equals "SIGABRT"
        And the exception "message" equals "Abort program"

    Scenario: Throwing an object in C++
        When I run "CXXThrowSomethingScenario" and relaunch the app
        And I configure Bugsnag for "CXXThrowSomethingScenario"
        And I wait to receive an error
        And the error payload contains a completed unhandled native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the exception "errorClass" equals "SIGABRT"
        And the exception "message" equals "Abort program"
