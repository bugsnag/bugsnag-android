Feature: Using custom API clients for reporting errors

Scenario: Set a custom error client and flush a stored error
    When I run "CustomClientErrorFlushScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "CustomClientErrorFlushScenario"
    And I set environment variable "EVENT_METADATA" to "DeliverReports"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 1 request
    And the "Custom-Client" header equals "Hello World"
