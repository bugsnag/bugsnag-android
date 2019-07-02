Feature: Reporting handled Exceptions

Scenario: Test handled Kotlin Exception
    When I run "HandledExceptionScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionScenario"
    And the payload field "events.0.device.cpuAbi" is a non-empty array for request 0

Scenario: Report a handled exception without a message
    When I run "HandledExceptionWithoutMessageScenario"
    Then I should receive a request
    And the request is valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "com.bugsnag.android.mazerunner.SomeException"
    And the event "exceptions.0.message" is null
    And the payload field "events.0.device.cpuAbi" is a non-empty array for request 0

Scenario: Test handled Java Exception
    When I run "HandledExceptionJavaScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionJavaScenario"

Scenario: Test handled Exception with Session
    When I run "HandledExceptionSessionScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionSessionScenario"
    And the event "session" is not null
    And the event "session.id" is not null
    And the event "session.startedAt" is not null
    And the event "session.events" is not null
    And the payload field "events.0.session.events.handled" equals 1
    And the payload field "events.0.session.events.unhandled" equals 0
