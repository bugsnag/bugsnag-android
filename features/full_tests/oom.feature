Feature: Reporting OOMs

  Background:
    Given I clear all persistent data

  Scenario: Out of Memory Error captured
    When I run "OomScenario" and relaunch the crashed app
    And I configure Bugsnag for "OomScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.OutOfMemoryError"

  Scenario: NativeOutOfMemoryPlugin captures OutOfMemoryError
    When I run "NativeOOMHandlerScenario" and relaunch the crashed app
    And I configure Bugsnag for "NativeOOMHandlerScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.OutOfMemoryError"
    # Currently this is the easiest way to know the event came from the NativeOutOfMemoryPlugin
    # once thread stacktraces are also captured the scenario will need another way to check
    And the error payload field "events.0.threads" is an array with 0 elements