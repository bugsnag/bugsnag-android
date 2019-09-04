Feature: Runtime versions are included in all requests

Scenario: Runtime versions included in JVM exception
    When I run "HandledExceptionScenario"
    Then I should receive a request
    And the request is valid for the error reporting API
    And the payload field "events.0.device.runtimeVersions.androidApiLevel" is not null
    And the payload field "events.0.device.runtimeVersions.osBuild" is not null

Scenario: Runtime versions included in NDK error
    When I run "CXXNullPointerScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive a request
    And the request payload contains a completed unhandled native report
    And the payload field "events.0.device.runtimeVersions.androidApiLevel" is not null
    And the payload field "events.0.device.runtimeVersions.osBuild" is not null

Scenario: Runtime versions included in session
    When I run "ManualSessionScenario"
    Then I should receive a request
    And the request is valid for the session tracking API
    And the payload field "device.runtimeVersions.androidApiLevel" is not null
    And the payload field "device.runtimeVersions.osBuild" is not null
