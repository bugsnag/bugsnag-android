Feature: Metadata is trimmed

  Background:
    Given I clear all persistent data

  Scenario: 1 metadata char truncated
    When I configure the app to run in the "39,abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "MetadataStringsTooLargeScenario"
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 2

  Scenario: 2 metadata chars truncated
    When I configure the app to run in the "38,abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "MetadataStringsTooLargeScenario"
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 4

  # This scenario may flake if the payload structure changes significantly because it relies upon overflowing
  # the max payload size by a certain amount. If it fails after a structural change, verify
  # maze_output/failed/Payload_is_too_big_by_3_breadcrumbs/errors.log to make sure it's behaving as expected,
  # and then modify "When I configure the app to run in the "100" state" to a number that generates 3 breadcrumbs
  # worth of data too much.
  Scenario: Payload is too big by 3 breadcrumbs
    When I configure the app to run in the "100" state
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "EventTooBigScenario"
    And the event has 98 breadcrumbs
    And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"

  Scenario: Breadcrumb is too big
    When I run "BreadcrumbTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "BreadcrumbTooBigScenario"
    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.name" equals "Removed, along with 1 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 2
    And the event "usage.system.breadcrumbBytesRemoved" is not null
