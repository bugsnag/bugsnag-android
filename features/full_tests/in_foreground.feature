Feature: In foreground field populates correctly

  Background:
    Given I clear all persistent data

  Scenario: Test handled exception in background
    When I run "InForegroundScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    # PLAT-9155 Flaky: the event "app.inForeground" is false
    And the error is correct for "InForegroundScenario" or I allow a retry
