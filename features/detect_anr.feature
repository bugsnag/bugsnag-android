Feature: Detects ANR

Scenario: Test ANR detected with default timing
    When I run "AppNotRespondingScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" equals "Application did not respond for at least 5000 ms"

Scenario: Test ANR not detected when disabled
    When I run "AppNotRespondingDisabledScenario"
    And I wait for 2 seconds
    Then I should receive no requests

Scenario: Test ANR not detected under response time
    When I run "AppNotRespondingShortScenario"
    And I wait for 2 seconds
    Then I should receive no requests

Scenario: Test ANR wait time can be set to under default time
    When I run "AppNotRespondingShorterThresholdScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" equals "Application did not respond for at least 2000 ms"

Scenario: Test ANR wait time can be set to over default time
    When I run "AppNotRespondingLongerThresholdScenario"
    And I wait for 2 seconds
    Then I should receive no requests
