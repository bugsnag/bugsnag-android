Feature: Reporting App Not Responding events

@anr
Scenario: Sleeping the main thread with pending touch events when detectAnrs = true
    When I run "AppNotRespondingScenario"
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

Scenario: Test ANR not detected under response time
    When I run "AppNotRespondingShortScenario"
    Then I should receive 0 requests

Scenario: Test ANR wait time can be set to under default time
    When I run "AppNotRespondingShorterThresholdScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "ANR"
    And the exception "message" equals "Application did not respond for at least 2000 ms"

Scenario: Test ANR wait time can be set to over default time
    When I run "AppNotRespondingLongerThresholdScenario"
    Then I should receive 0 requests

Scenario: Test does not capture ANRs by default
    When I run "AppNotRespondingDefaultsScenario"
    Then I should receive 0 requests
