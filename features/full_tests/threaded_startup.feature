Feature: Switching automatic error detection on/off for Unity

  Background:
    Given I clear all persistent data

  Scenario: Starting Bugsnag & calling it on separate threads
    When I run "MultiThreadedStartupScenario" and relaunch the crashed app
    And I configure Bugsnag for "MultiThreadedStartupScenario"
    And I wait for 2 seconds
    Then I should receive no error
