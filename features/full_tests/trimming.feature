Feature: Excess data is trimmed when the payload is too big

  Background:
    Given I clear all persistent data

  # This scenario may flake if the payload structure changes significantly because it relies upon overflowing
  # the max payload size by a certain amount. If it fails after a structural change, verify
  # maze_output/failed/Payload_is_too_big_by_3_breadcrumbs/errors.log to make sure it's behaving as expected,
  # and then modify "When I configure the app to run in the "100" state" to a number that generates 3 breadcrumbs
  # worth of data too much.

  Scenario: Payload is too big by 3 breadcrumbs, handled exception that failed initial delivery
    When I set the HTTP status code for the next request to 500
    And I configure the app to run in the "handled, 10000, 100" state
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "EventTooBigScenario"
    And the event has 98 breadcrumbs
    And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 3
    And the event "usage.system.breadcrumbBytesRemoved" is not null
    And I close and relaunch the app
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "EventTooBigScenario"
    And the event has 98 breadcrumbs
    And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 3
    And the event "usage.system.breadcrumbBytesRemoved" is not null

  Scenario: Payload is too big by 3 breadcrumbs, handled exception
    When I configure the app to run in the "handled, 10000, 100" state
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "EventTooBigScenario"
    And the event has 98 breadcrumbs
    And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 3
    And the event "usage.system.breadcrumbBytesRemoved" is not null

  Scenario: Payload is too big by 3 breadcrumbs, jvm exception
    When I configure the app to run in the "jvm, 10000, 100" state
    And I run "EventTooBigScenario" and relaunch the crashed app
    And I configure the app to run in the "none, 10000, 100" state
    And I configure Bugsnag for "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Empty list doesn't contain element at index 0."
    And the event has 98 breadcrumbs
    And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 3
    And the event "usage.system.breadcrumbBytesRemoved" is not null

  # Note: Disabled until native hard limits are removed.
  # Scenario: Payload is too big by 3 breadcrumbs, native crash
  #   When I configure the app to run in the "native, 10000, 100" state
  #   And I run "EventTooBigScenario" and relaunch the crashed app
  #   And I configure the app to run in the "none, 10000, 100" state
  #   And I configure Bugsnag for "EventTooBigScenario"
  #   Then I wait to receive an error
  #   And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
  #   And the exception "message" equals "Segmentation violation (invalid memory reference)"
  #   And the event has 98 breadcrumbs
  #   And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"
  #   And the event "usage.system.breadcrumbsRemoved" equals 3
  #   And the event "usage.system.breadcrumbBytesRemoved" is not null

  # ===========================================================================

  Scenario: Breadcrumb is too big, handled exception
    When I configure the app to run in the "handled, 1100000, 1" state
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "EventTooBigScenario"
    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.name" equals "Removed, along with 1 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 2
    And the event "usage.system.breadcrumbBytesRemoved" is not null

  Scenario: Breadcrumb is too big, jvm exception
    When I configure the app to run in the "jvm, 1100000, 1" state
    And I run "EventTooBigScenario" and relaunch the crashed app
    And I configure the app to run in the "none, 1100000, 1" state
    And I configure Bugsnag for "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Empty list doesn't contain element at index 0."
    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.name" equals "Removed, along with 1 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 2
    And the event "usage.system.breadcrumbBytesRemoved" is not null

  # Note: Disabled until native hard limits are removed.
  # Scenario: Breadcrumb is too big, native crash
  #   When I configure the app to run in the "native, 1100000, 1" state
  #   And I run "EventTooBigScenario" and relaunch the crashed app
  #   And I configure the app to run in the "none, 1100000, 1" state
  #   And I configure Bugsnag for "EventTooBigScenario"
  #   Then I wait to receive an error
  #   And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
  #   And the exception "message" equals "Segmentation violation (invalid memory reference)"
  #   And the event has 1 breadcrumbs
  #   And the event "breadcrumbs.0.name" equals "Removed, along with 1 older breadcrumbs, to reduce payload size"
  #   And the event "usage.system.breadcrumbsRemoved" equals 2
  #   And the event "usage.system.breadcrumbBytesRemoved" is not null
