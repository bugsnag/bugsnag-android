Feature: Detects ANR

Scenario: Test ANR detected with default timing
    When I run "AppNotRespondingScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "ANR"
    And the exception "message" equals "Application did not respond for at least 5000 ms"

Scenario: Test ANR not detected when disabled
    When I run "AppNotRespondingDisabledScenario"
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
