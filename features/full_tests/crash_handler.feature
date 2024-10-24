Feature: Reporting with other exception handlers installed

  Background:
    Given I clear all persistent data

  Scenario: Other uncaught exception handler installed
    When I run "CrashHandlerScenario" and relaunch the crashed app
    And I configure Bugsnag for "CrashHandlerScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "CrashHandlerScenario"
    And the event "metaData.customHandler.invoked" is true

  Scenario: Delivers crashes synchronously when configured
    When I run "DeliverOnCrashScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "DeliverOnCrashScenario"
    And the event "usage.config.attemptDeliveryOnCrash" is true

  Scenario: OkHttpDelivery is used to deliver payloads
    When I run "OkHttpDeliveryScenario" and relaunch the crashed app
    And I configure Bugsnag for "OkHttpDeliveryScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "Unhandled Error"
