Feature: Native on error callbacks are invoked

  Background:
    Given I clear all persistent data

  Scenario: on_error returning false prevents C signal being reported
    When I run "CXXSignalOnErrorFalseScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSignalOnErrorFalseScenario"
    Then Bugsnag confirms it has no errors to send

  Scenario: on_error returning false prevents C++ exception being reported
    When I run "CXXExceptionOnErrorFalseScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXExceptionOnErrorFalseScenario"
    Then Bugsnag confirms it has no errors to send

  Scenario: Removing on_error callback
    When I run "CXXRemoveOnErrorScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXRemoveOnErrorScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    And the event "user.id" equals "default"
    And the event "user.email" equals "default@default.df"
    And the event "user.name" equals "default"
