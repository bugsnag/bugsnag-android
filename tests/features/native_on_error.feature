Feature: Native on error callbacks are invoked

  Scenario: on_error returning false prevents C signal being reported
    When I run "CXXSignalOnErrorFalseScenario" and relaunch the app
    And I configure Bugsnag for "CXXSignalOnErrorFalseScenario"
    Then I should receive no requests

  Scenario: on_error returning false prevents C++ exception being reported
    When I run "CXXExceptionOnErrorFalseScenario" and relaunch the app
    And I configure Bugsnag for "CXXExceptionOnErrorFalseScenario"
    Then I should receive no requests

  Scenario: C signal with on_error that modifies data
    When I run "CXXSignalOnErrorTrueScenario" and relaunch the app
    And I configure Bugsnag for "CXXSignalOnErrorTrueScenario"
    And I wait to receive a request
    And the request payload contains a completed unhandled native report
    And the event "context" equals "Some custom context"

  Scenario: Cxx exception with on_error that modifies data
    When I run "CXXExceptionOnErrorTrueScenario" and relaunch the app
    And I configure Bugsnag for "CXXExceptionOnErrorTrueScenario"
    And I wait to receive a request
    And the request payload contains a completed unhandled native report
    And the event "context" equals "Some custom context"

  Scenario: Removing on_error callback
    When I run "CXXRemoveOnErrorScenario" and relaunch the app
    And I configure Bugsnag for "CXXRemoveOnErrorScenario"
    And I wait to receive a request
    Then the request payload contains a completed handled native report
    And the event "user.id" equals "default"
    And the event "user.email" equals "default@default.df"
    And the event "user.name" equals "default"
