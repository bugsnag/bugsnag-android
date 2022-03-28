Feature: Add custom behavior through a plugin interface

  Some internal libraries may build on top of the Bugsnag Android library and
  require custom behavior prior to the library being fully initialized. This
  interface allows for installing that behavior before calling the regular
  initialization process.

  Background:
    Given I clear all persistent data

  Scenario: Changing payload notifier description
    When I run "CustomPluginNotifierDescriptionScenario"
    Then I wait to receive an error
    Then the event "context" equals "Foo Handler Library"
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"

