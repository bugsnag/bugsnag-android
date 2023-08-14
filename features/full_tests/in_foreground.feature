Feature: In foreground field populates correctly

  Background:
    Given I clear all persistent data

  # TODO: Skipped pending PLAT-10634
  @skip
  Scenario: Test handled exception in background
    When I run "InForegroundScenario"
    And I send the app to the background for 5 seconds
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    # PLAT-9155 Flaky: the event "app.inForeground" is false
    And the error is correct for "InForegroundScenario" or I allow a retry
