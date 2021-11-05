Feature: Reporting with other exception handlers installed

Scenario: Other uncaught exception handler installed
    When I run "CrashHandlerScenario" and relaunch the app
    And I configure Bugsnag for "CrashHandlerScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "CrashHandlerScenario"
    And the event "metaData.customHandler.invoked" is true
