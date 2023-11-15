Feature: Capture native threads

  Background:
    Given I clear all persistent data

  Scenario: Reports native threads for Unhandled errors
    When I run "CXXCaptureThreadsScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXCaptureThreadsScenario"
    And I wait to receive an error
    Then the error payload contains a completed unhandled native report
    And the error payload field "events.0.threads" is a non-empty array
    And the error payload field "events.0.threads.0.state" is not null
    And the error payload field "events.0.threads.0.name" is not null
    And the event "threads.0.id" matches "^[0-9]+$"

  Scenario: Reports native threads for Unhandled errors where sendThreads is UNHANDLED_ONLY
    When I configure the app to run in the "UNHANDLED_ONLY" state
    And I run "CXXCaptureThreadsScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXCaptureThreadsScenario"
    And I wait to receive an error
    Then the error payload contains a completed unhandled native report
    And the error payload field "events.0.threads" is a non-empty array
    And the error payload field "events.0.threads.0.state" is not null
    And the error payload field "events.0.threads.0.name" is not null
    And the event "threads.0.id" matches "^[0-9]+$"

  Scenario: No threads are reported when sendThreads is NEVER
    When I configure the app to run in the "NEVER" state
    And I run "CXXCaptureThreadsScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXCaptureThreadsScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    And the error payload field "events.0.threads" is an array with 0 elements

  Scenario: No threads are reported for a handled errors where sendThreads is UNHANDLED_ONLY
    When I configure the app to run in the "UNHANDLED_ONLY" state
    And I run "CXXCaptureThreadsNotifyScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    And the error payload field "events.0.threads" is an array with 0 elements
