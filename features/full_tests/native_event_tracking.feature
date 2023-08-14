Feature: Synchronizing app/device metadata in the native layer

  Background:
    Given I clear all persistent data

  Scenario: Capture foreground state while in the foreground
    When I run "CXXDelayedNotifyScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    And the event "app.inForeground" is true
    And the event "app.duration" is greater than 0
    And the event "unhandled" is false
    And the "codeIdentifier" of stack frame 0 is not null

  # TODO: Skipped pending PLAT-10634
  @skip
  Scenario: Capture foreground state while in the background
    When I run "CXXBackgroundNotifyScenario"
    And I send the app to the background for 5 seconds
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    # PLAT-9155 Flaky: the event "app.inForeground" is false
    And the error is correct for "CXXBackgroundNotifyScenario" or I allow a retry
    And the event "app.durationInForeground" equals 0
    And the event "app.duration" is greater than 0
    And the event "context" string is empty
    And the event "unhandled" is false
    And the "codeIdentifier" of stack frame 0 is not null

  Scenario: Capture foreground state while in a foreground crash
    When I run "CXXTrapScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXStartSessionScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    And the event "app.inForeground" is true
    And the event "app.durationInForeground" is not null
    And the event "app.duration" is not null
    And the event "unhandled" is true

  # Skipped pending PLAT-9155
  @skip
  Scenario: Capture foreground state while in a background crash
    When I run "CXXDelayedCrashScenario"
    And I send the app to the background for 10 seconds
    And I relaunch the app after a crash
    And I configure Bugsnag for "CXXDelayedCrashScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    # PLAT-9155 Flaky: the event "app.inForeground" is false
    And the error is correct for "CXXDelayedCrashScenario" or I allow a retry
    And the event "app.duration" is greater than 0
    And the event "context" string is empty
    And the event "unhandled" is true
