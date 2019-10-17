Feature: Verifies autoDetectNdkCrashes controls when NDK crashes are reported

Scenario: Crash reported when autoDetectNdkCrashes enabled
    When I run "AutoDetectNdkEnabledScenario" and relaunch the app
    And I configure Bugsnag for "AutoDetectNdkEnabledScenario"
    Then I wait to receive a request

Scenario: No crash reported when autoDetectNdkCrashes disabled
    When I run "AutoDetectNdkDisabledScenario" and relaunch the app
    And I configure Bugsnag for "AutoDetectNdkDisabledScenario"
    And I wait for 2 seconds
    Then I should receive no requests
