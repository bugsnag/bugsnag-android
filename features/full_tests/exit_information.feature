Feature: Application exitInfo is reported in crashes

  Background:
    Given I clear all persistent data

  @skip_below_android_11
  Scenario: Application exitInfo is reported in crashes
    When I set the screen orientation to portrait
    And I run "ExitInfoScenario" and relaunch the crashed app
    And I configure Bugsnag for "ExitInfoScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "metaData.app.processImportance" equals "foreground"
    And the event "metaData.app.exitReason" is not null
