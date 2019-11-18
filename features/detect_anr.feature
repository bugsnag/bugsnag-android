Feature: Reporting App Not Responding events

@anr
Scenario: Sleeping the main thread with pending touch events when autoDetectAnrs = true
    When I run "AppNotRespondingScenario"
    And I tap the screen
    And I tap the screen
    And I tap the screen
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"

@anr
Scenario: Sleeping the main thread with pending touch events when autoDetectAnrs = true and autoDetectNdkCrashes = false
    When I run "AppNotRespondingDisabledNdkScenario"
    And I tap the screen
    And I tap the screen
    And I tap the screen
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "ANR"
    And the exception "message" equals "Application did not respond to UI input"

@anr
Scenario: Sleeping the main thread with pending touch events
    When I run "AppNotRespondingDisabledScenario"
    And I tap the screen
    And I tap the screen
    And I tap the screen
    Then I should receive 0 requests
