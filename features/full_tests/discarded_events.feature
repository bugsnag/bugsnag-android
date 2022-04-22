Feature: Discarding events

  Background:
    Given I clear all persistent data

  Scenario: Discard an on-disk error that failed to send and is too old
    # Fail to send initial handled error. Client stores it to disk.
    When I set the HTTP status code for the next request to 500
    And I run "DiscardOldEventsScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Fail to send event that was reloaded from disk. Event is too old, so the client discards it.
    Then I discard the oldest error
    # We send another error to keep maze-runner from shutting down the app prematurely.
    And I wait to receive an error
    And I discard the oldest error
    And I set the HTTP status code for the next request to 500
    And I close and relaunch the app
    And I configure Bugsnag for "DiscardOldEventsScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Now there is no event on disk, so there's nothing to send.
    Then I discard the oldest error
    And I close and relaunch the app
    And I configure Bugsnag for "DiscardOldEventsScenario"
    Then I should receive no requests

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
    And I configure Bugsnag for "DiscardBigEventsScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest error

    # Now there is no event on disk, so there's nothing to send.
    And I close and relaunch the app
    And I configure Bugsnag for "DiscardBigEventsScenario"
    Then I should receive no requests
