Feature: Using custom API clients for reporting errors

Scenario: Set a custom session client and flush a stored session
    When I run "CustomClientSessionFlushScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "CustomClientSessionFlushScenario"
    And I set environment variable "EVENT_METADATA" to "DeliverSessions"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 2 requests
    And the "Custom-Client" header equals "Hello World" for request 0
    And the "Custom-Client" header equals "Hello World" for request 1

Scenario: Set a custom error API client and notify an error
    When I run "CustomClientErrorScenario" with the defaults
    Then I should receive 1 request
    And the "Custom-Client" header equals "Hello World"
    And the request is a valid for the error reporting API

Scenario: Set a custom session API client and start a session
    When I run "CustomClientSessionScenario" with the defaults
    Then I should receive 1 request
    And the "Custom-Client" header equals "Hello World"
    And the request is a valid for the session tracking API

Scenario: Set a custom error client and flush a stored error
    When I run "CustomClientErrorFlushScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "CustomClientErrorFlushScenario"
    And I set environment variable "EVENT_METADATA" to "DeliverReports"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 1 request
    And the "Custom-Client" header equals "Hello World"
