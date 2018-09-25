# For handled exceptions, the thread trace should be reported in the threads array.
# For unhandled exceptions, the exception trace should be reported instead.

Feature: Error Reporting Thread

Scenario: Only 1 thread is flagged as the error reporting thread for handled exceptions
    When I run "HandledExceptionScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the thread with name "main" contains the error reporting flag
    And the "method" of stack frame 0 equals "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
    And the payload field "events.0.threads.0.stacktrace.0.method" ends with "getThreadStackTrace"

Scenario: Only 1 thread is flagged as the error reporting thread for unhandled exceptions
    When I run "UnhandledExceptionScenario" with the defaults
    Then I should receive 1 request
    And the request is a valid for the error reporting API
    And the thread with name "main" contains the error reporting flag
    And the "method" of stack frame 0 equals "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
    And the payload field "events.0.threads.0.stacktrace.0.method" equals "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
