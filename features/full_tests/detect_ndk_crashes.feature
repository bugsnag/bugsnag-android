Feature: Verifies autoDetectNdkCrashes controls when NDK crashes are reported

  Background:
    Given I clear all persistent data

  Scenario: No crash reported when autoDetectNdkCrashes disabled
    When I run "AutoDetectNdkDisabledScenario" and relaunch the crashed app
    And I configure Bugsnag for "AutoDetectNdkDisabledScenario"
    Then I should receive no requests
