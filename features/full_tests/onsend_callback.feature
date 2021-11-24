Feature: OnSend Callbacks can alter Events before upload

  Scenario: Handled exception with altered by OnSendCallback
    When I run "OnSendCallbackScenario" and relaunch the app
    And I configure the app to run in the "start-only" state
    And I configure Bugsnag for "OnSendCallbackScenario"
    Then I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the exception "message" equals "Unhandled Error"
    And the event "metaData.mazerunner.onSendCallback" equals "true"