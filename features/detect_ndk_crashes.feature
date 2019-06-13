Feature: Verifies detectNdkCrashes controls when NDK crashes are reported

Scenario: Crash reported when detectNdkCrashes enabled
    When I run "DetectNdkEnabledScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 1 request

Scenario: No crash reported when detectNdkCrashes disabled
    When I run "DetectNdkDisabledScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests
