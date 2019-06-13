Feature: Reporting Strict Mode Exceptions

Scenario: StrictMode DiscWrite violation
    When I run "StrictModeDiscScenario" and relaunch the app
    And I configure Bugsnag for "StrictModeDiscScenario"
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "android.os.StrictMode$StrictModeViolation"
    And the event "metaData.StrictMode.Violation" equals "DiskWrite"


Scenario: StrictMode Network on Main Thread violation
    When I run "StrictModeNetworkScenario" and relaunch the app
    And I configure Bugsnag for "StrictModeNetworkScenario"
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "android.os.StrictMode$StrictModeViolation"
    And the event "metaData.StrictMode.Violation" equals "NetworkOperation"
