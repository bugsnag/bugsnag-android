Feature: Native on error callbacks are invoked

  Scenario: on_error returning false prevents C signal being reported
    When I run "CXXSignalOnErrorFalseScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive no requests

  Scenario: on_error returning false prevents C++ exception being reported
    When I run "CXXExceptionOnErrorFalseScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive no requests

  Scenario: C signal with on_error that modifies data
    When I run "CXXSignalOnErrorTrueScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive a request
    And the request payload contains a completed unhandled native report
    And the event "context" equals "Some custom context"

  Scenario: Cxx exception with on_error that modifies data
    When I run "CXXExceptionOnErrorTrueScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive a request
    And the request payload contains a completed unhandled native report
    And the event "context" equals "Some custom context"
