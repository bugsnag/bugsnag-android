Feature: Reporting handled Exceptions

Scenario: Test handled Kotlin Exception
    When I run "HandledExceptionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionScenario"
    And the payload field "events.0.device.cpuAbi" is a non-empty array

Scenario: Report a handled exception without a message
    When I run "HandledExceptionWithoutMessageScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "com.bugsnag.android.mazerunner.SomeException"
    And the event "exceptions.0.message" is null
    And the payload field "events.0.device.cpuAbi" is a non-empty array

Scenario: Test handled Java Exception
    When I run "HandledExceptionJavaScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionJavaScenario"

Scenario: Test handled Exception with Session
    When I run "HandledExceptionSessionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionSessionScenario"
    And the event "session" is not null
    And the event "session.id" is not null
    And the event "session.startedAt" is not null
    And the event "session.events" is not null
    And the payload field "events.0.session.events.handled" equals 1
    And the payload field "events.0.session.events.unhandled" equals 0
