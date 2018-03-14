Feature: Reporting Stack overflow

Scenario: Stack Overflow sends
    When I run "StackOverflowScenario" with the defaults
#   Need to wait a while to trigger this in release mode
    And I wait for 30 seconds
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.StackOverflowError"
    And the "method" of stack frame 0 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
    And the "method" of stack frame 1 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
    And the "method" of stack frame 2 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
    And the "method" of stack frame 3 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
