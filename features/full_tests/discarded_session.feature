Feature: Discarding sessions

  Background:
    Given I clear all persistent data

  Scenario: Discard an on-disk session that failed to send and is too old
    # Part 1 sets a bogus session endpoint so that all attempts to send the session fail.
    # Note: The app will make two attempts.
    When I run "DiscardOldSessionScenario"

    # Part 1 sleeps to let bg tasks run, then manually renames the session file to oldify it.

    # Send an error to keep maze-runner from shutting down the app prematurely.
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest error

    # The important part is that no sessions are received at this point.
    Then I should receive no sessions

    # Part 2 loads the session, fails to send it, then discards it because it's too old.
    Then I close and relaunch the app
    And I run "DiscardOldSessionScenarioPart2"

    # Send an error to keep maze-runner from shutting down the app prematurely.
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest error

    # Part 2 will always fail to send sessions.
    Then I should receive no sessions

    # Part 3 has no sending impediments, but there are no more session files so nothing to send.
    Then I close and relaunch the app
    And I configure Bugsnag for "DiscardOldSessionScenarioPart3"
    Then I should receive no sessions
