# For handled exceptions, the thread trace should be reported in the threads array.
# For unhandled exceptions, the exception trace should be reported instead.

Feature: Error Reporting Thread

Scenario: Only 1 thread is flagged as the error reporting thread for handled exceptions
    When I run "HandledExceptionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the thread with name "main" contains the error reporting flag
    And the "method" of stack frame 0 equals "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
    And the payload field "events.0.threads.0.stacktrace.0.method" ends with "getThreadStackTrace"

Scenario: Only 1 thread is flagged as the error reporting thread for unhandled exceptions
    When I run "UnhandledExceptionScenario" and relaunch the app
    And I configure Bugsnag for "UnhandledExceptionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the thread with name "main" contains the error reporting flag
    And the "method" of stack frame 0 equals "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
    And the payload field "events.0.threads.0.stacktrace.0.method" equals "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
