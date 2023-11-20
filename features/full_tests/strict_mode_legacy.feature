Feature: Reporting Strict Mode Violations

  Background:
    Given I clear all persistent data

  @skip_above_android_8
  Scenario: StrictMode DiscWrite violation
    When I run "StrictModeDiscScenario" and relaunch the crashed app
    And I configure Bugsnag for "StrictModeDiscScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "android.os.StrictMode$StrictModeViolation"
    And the event "metaData.StrictMode.Violation" equals "DiskWrite"
    And the event "severityReason.type" equals "strictMode"

  @skip_above_android_8
  Scenario: StrictMode Network on Main Thread violation
    When I run "StrictModeNetworkScenario" and relaunch the crashed app
    And I configure Bugsnag for "StrictModeNetworkScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "android.os.StrictMode$StrictModeViolation"
    And the event "metaData.StrictMode.Violation" equals "NetworkOperation"
    And the event "severityReason.type" equals "strictMode"
