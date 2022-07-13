Feature: OnSend Callbacks can alter Events before upload

  Background:
    Given I clear all persistent data

  Scenario: Unhandled exception altered by OnSendCallback
    When I run "OnSendCallbackScenario" and relaunch the crashed app
    And I configure the app to run in the "start-only" state
    And I configure Bugsnag for "OnSendCallbackScenario"
    Then I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the error payload field "apiKey" equals "99999999999999909999999999999999"
    And the exception "message" equals "Unhandled Error"
    And the event "metaData.mazerunner.onSendCallback" equals "true"
    And the event "featureFlags.0.featureFlag" equals "fromStartup"
    And the event "featureFlags.0.variant" equals "b"

  Scenario: Handled exception altered by OnSendCallback
    When I run "HandledOnSendCallbackScenario"
    And I wait to receive an error
    Then the error payload field "apiKey" equals "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbba"
    And the error payload field "events" is an array with 1 elements
    And the event "metaData.mazerunner.onSendCallback" equals "true"
