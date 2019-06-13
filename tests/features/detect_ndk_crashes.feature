Feature: Verifies detectNdkCrashes controls when NDK crashes are reported

Scenario: Crash reported when detectNdkCrashes enabled
    When I run "DetectNdkEnabledScenario" and relaunch the app
    And I configure Bugsnag for "DetectNdkEnabledScenario"
    Then I wait to receive a request

Scenario: No crash reported when detectNdkCrashes disabled
    When I run "DetectNdkDisabledScenario" and relaunch the app
    And I configure Bugsnag for "DetectNdkDisabledScenario"
    And I wait for 2 seconds
    Then I should receive no requests
