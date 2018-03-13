Feature: Android support

Scenario: Test handled Android Exception
    When I run "HandledExceptionSessionScenario" with the defaults
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
