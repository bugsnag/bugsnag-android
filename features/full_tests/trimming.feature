Feature: Metadata is trimmed

  Background:
    Given I clear all persistent data

  Scenario: 1 metadata char truncated, handled exception
    When I configure the app to run in the "handled, 39, abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "MetadataStringsTooLargeScenario"
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 2

  Scenario: 1 metadata char truncated, JVM exception
    When I configure the app to run in the "jvm, 39, abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario" and relaunch the crashed app
    And I configure Bugsnag for "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Empty list doesn't contain element at index 0."
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 2

  Scenario: 1 metadata char truncated, native exception
    When I configure the app to run in the "native, 39, abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario" and relaunch the crashed app
    And I configure Bugsnag for "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijklm***<1> CHAR TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 2

  # ===========================================================================

  Scenario: 2 metadata chars truncated, handled exception
    When I configure the app to run in the "handled, 38, abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "MetadataStringsTooLargeScenario"
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 4

  Scenario: 2 metadata chars truncated, jvm exception
    When I configure the app to run in the "jvm, 38, abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "MetadataStringsTooLargeScenario"
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 4

  Scenario: 2 metadata chars truncated, native crash
    When I configure the app to run in the "native, 38, abcdefghijklmnopqrstuvwxyzabcdefghijklmn" state
    And I run "MetadataStringsTooLargeScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the event "metaData.custom.foo" equals "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the breadcrumb named "test" has "metaData.a" equal to "abcdefghijklmnopqrstuvwxyzabcdefghijkl***<2> CHARS TRUNCATED***"
    And the event "usage.system.stringsTruncated" equals 2
    And the event "usage.system.stringCharsTruncated" equals 4

  # ===========================================================================

  # This scenario may flake if the payload structure changes significantly because it relies upon overflowing
  # the max payload size by a certain amount. If it fails after a structural change, verify
  # maze_output/failed/Payload_is_too_big_by_3_breadcrumbs/errors.log to make sure it's behaving as expected,
  # and then modify "When I configure the app to run in the "100" state" to a number that generates 3 breadcrumbs
  # worth of data too much.

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
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "EventTooBigScenario"
    And the event has 98 breadcrumbs
    And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 3
    And the event "usage.system.breadcrumbBytesRemoved" is not null

  Scenario: Payload is too big by 3 breadcrumbs, native crash
    When I configure the app to run in the "native, 10000, 100" state
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the event has 98 breadcrumbs
    And the event "breadcrumbs.97.name" equals "Removed, along with 2 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 3
    And the event "usage.system.breadcrumbBytesRemoved" is not null

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
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "EventTooBigScenario"
    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.name" equals "Removed, along with 1 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 2
    And the event "usage.system.breadcrumbBytesRemoved" is not null

  Scenario: Breadcrumb is too big, native crash
    When I configure the app to run in the "native, 1100000, 1" state
    And I run "EventTooBigScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.name" equals "Removed, along with 1 older breadcrumbs, to reduce payload size"
    And the event "usage.system.breadcrumbsRemoved" equals 2
    And the event "usage.system.breadcrumbBytesRemoved" is not null
