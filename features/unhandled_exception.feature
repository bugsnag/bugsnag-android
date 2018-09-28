Feature: Reporting Unhandled Exceptions

Scenario: Test Unhandled Kotlin Exception without Session
    When I run "UnhandledExceptionScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledExceptionScenario"
    And the event "session" is null

Scenario: Test Unhandled Java Exception with Session
    When I run "UnhandledExceptionJavaScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledExceptionJavaScenario"

Scenario: Test handled Kotlin Exception with Session
    When I run "UnhandledExceptionSessionScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledExceptionSessionScenario"
    And the event "session" is not null
    And the event "session.id" is not null
    And the event "session.startedAt" is not null
    And the event "session.events" is not null
    And the payload field "events.0.session.events.handled" equals 0
    And the payload field "events.0.session.events.unhandled" equals 1

Scenario: Test cached Unhandled Exception with Session sends
    When I run "ReportCacheScenario"
    Then I should receive no requests

    When I configure the app to run in the "online" state
    And I relaunch the app
    Then I should receive a request

    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ReportCacheScenario"
