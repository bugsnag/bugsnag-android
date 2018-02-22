Feature: Android support

Scenario: Test handled Android Exception
    When I run "StrictModeNetworkScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "android.os.StrictMode$StrictModeViolation"
    And the exception "message" equals "policy=262148 violation=4"
    And the event "metaData.StrictMode.Violation" equals "NetworkOperation"
