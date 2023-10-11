Feature: Discarding events

  Background:
    Given I clear all persistent data

  Scenario: Discard an on-disk error that failed to send and is too old
    # Fail to send initial handled error. Client stores it to disk.
    When I set the HTTP status code for the next request to 500
    And I run "DiscardOldEventsScenario"

    # The second error to keeps maze-runner from shutting down the app prematurely.
    And I wait to receive 2 errors
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "DiscardOldEventsScenario"
    And I discard the oldest error

    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "MazeRunner KeepAlive"
    And I discard the oldest error

    # Fail to send event that was reloaded from disk. Event is too old, so the client discards it.
    And I set the HTTP status code for the next request to 500
    And I close and relaunch the app
    And I configure Bugsnag for "DiscardOldEventsScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "DiscardOldEventsScenario"

    # Check that Bugsnag is discarding the event
    And I wait to receive a log
    And the log payload field "level" equals "warning"
    And the log payload field "message" matches the regex "Discarding historical event \(from.*\) after failed delivery"

  Scenario: Discard an on-disk error that received 500 and is too big
    # Fail to send initial handled error due to 500 error. Client stores it to disk.
    When I set the HTTP status code for the next request to 500
    And I run "DiscardBigEventsScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest error

    # Fail to send event that was reloaded from disk. Event is too big, so the client discards it.
    And I set the HTTP status code for the next request to 500
    And I close and relaunch the app

    And I configure the app to run in the "delete-wait" state
    And I configure Bugsnag for "DiscardBigEventsScenario"

    And I wait to receive 2 errors

    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "DiscardBigEventsScenario"

    And I discard the oldest error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "ErrorsDirectoryEmpty"

    And I discard the oldest error

    # Now there is no event on disk, so there's nothing to send.
    And I close and relaunch the app
    And I configure Bugsnag for "DiscardBigEventsScenario"
    Then I should receive no errors

  Scenario: Discard an on-disk error that received 400 and is too big
    # Fail to send initial handled error due to 400 error. Client discards it immediately.
    When I set the HTTP status code for the next request to 400
    And I run "DiscardBigEventsScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest error

    # Now there is no event on disk, so there's nothing to send.
    And I close and relaunch the app
    And I configure Bugsnag for "DiscardBigEventsScenario"
    Then Bugsnag confirms it has no errors to send
