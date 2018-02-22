Feature: Android support

Scenario: Stack Overflow
    When I run "StackOverflowScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.OutOfMemoryError"
    And the "method" of stack frame 0 equals "java.lang"
#method, file, lineNumber
