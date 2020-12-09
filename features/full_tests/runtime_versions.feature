Feature: Runtime versions are included in all requests

Scenario: Runtime versions included in JVM exception
    When I run "HandledExceptionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.device.runtimeVersions.androidApiLevel" is not null
    And the payload field "events.0.device.runtimeVersions.osBuild" is not null

Scenario: Runtime versions included in NDK error
    And I run "CXXNullPointerScenario" and relaunch the app
    When I configure the app to run in the "non-crashy" state
    And I configure Bugsnag for "CXXNullPointerScenario"
    Then I wait to receive a request
    And the request payload contains a completed unhandled native report
    And the payload field "events.0.device.runtimeVersions.androidApiLevel" is not null
    And the payload field "events.0.device.runtimeVersions.osBuild" is not null

Scenario: Runtime versions included in session
    When I run "ManualSessionScenario"
    Then I wait to receive a request
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "device.runtimeVersions.androidApiLevel" is not null
    And the payload field "device.runtimeVersions.osBuild" is not null
