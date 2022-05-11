Feature: Switching automatic error detection on/off for Unity

  Background:
    Given I clear all persistent data

  Scenario: Starting Bugsnag & calling it on separate threads
    When I run "MultiThreadedStartupScenario" and relaunch the crashed app
    And I configure Bugsnag for "MultiThreadedStartupScenario"
    Then I wait to receive an error
    And the error is correct for "MultiThreadedStartupScenario" or I allow a retry
