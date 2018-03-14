Feature: Reporting Strict Mode Exceptions

Scenario: StrictMode DiscWrite violation
    When I run "StrictModeDiscScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "android.os.StrictMode$StrictModeViolation"
    And the exception "message" equals "policy=262145 violation=1"
    And the event "metaData.StrictMode.Violation" equals "DiskWrite"


Scenario: StrictMode Network on Main Thread violation
    When I run "StrictModeNetworkScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "android.os.StrictMode$StrictModeViolation"
    And the exception "message" equals "policy=262148 violation=4"
    And the event "metaData.StrictMode.Violation" equals "NetworkOperation"
