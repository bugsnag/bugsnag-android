Feature: Verifies autoDetectNdkCrashes controls when NDK crashes are reported

Scenario: No crash reported when autoDetectNdkCrashes disabled
    When I run "AutoDetectNdkDisabledScenario" and relaunch the app
    And I configure Bugsnag for "AutoDetectNdkDisabledScenario"
    And I wait for 2 seconds
    Then I should receive no requests
