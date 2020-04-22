Feature: Verifies autoDetectNdkCrashes controls when NDK crashes are reported

Scenario: Crash reported when autoDetectNdkCrashes enabled
    When I run "AutoDetectNdkEnabledScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 1 request

Scenario: No crash reported when autoDetectNdkCrashes disabled
    When I run "AutoDetectNdkDisabledScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests
