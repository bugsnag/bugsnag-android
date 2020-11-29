Feature: Session functionality smoke tests

Scenario: Automated sessions send
    When I run "AutoSessionSmokeScenario"
    And I wait to receive 2 requests

    # Session payload
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "sessions" is an array with 1 elements
    And the session "user.id" is not null
    And the payload field "sessions.0.user.id" is stored as the value "automated_user_id"
    And the session "id" is not null
    And the payload field "sessions.0.id" is stored as the value "automated_session_id"
    And the session "startedAt" is not null

    # App data
    And the payload field "app.buildUUID" is not null
    And the payload field "app.id" equals "com.bugsnag.android.mazerunner"
    And the payload field "app.releaseStage" equals "production"
    And the payload field "app.type" equals "android"
    And the payload field "app.version" equals "1.1.14"
    And the payload field "app.versionCode" equals 34

    # Device data
    And the payload field "device.cpuAbi" is a non-empty array
    And the payload field "device.jailbroken" is false
    And the payload field "device.id" is not null
    And the payload field "device.id" equals the stored value "automated_user_id"
    And the payload field "device.locale" is not null
    And the payload field "device.manufacturer" is not null
    And the payload field "device.model" is not null
    And the payload field "device.osName" equals "android"
    And the payload field "device.osVersion" is not null
    And the payload field "device.runtimeVersions" is not null
    And I discard the oldest request

    # Error payload
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "AutoSessionSmokeScenario"
    And the event "user.id" is not null
    And the event "device.id" is not null
    And the payload field "events.0.user.id" equals the stored value "automated_user_id"
    And the payload field "events.0.device.id" equals the stored value "automated_user_id"
    And the event "session.id" is not null
    And the payload field "events.0.session.id" equals the stored value "automated_session_id"
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 0

Scenario: Manual session control works
    When I run "ManualSessionSmokeScenario"
    And I wait for 5 seconds
    And I relaunch the app
    And I configure Bugsnag for "ManualSessionSmokeScenario"
    And I wait to receive 4 requests

    # Session payload
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions.0.id" is stored as the value "manual_session_id"
    And the session "user.id" equals "123"
    And the session "user.email" equals "ABC.CBA.CA"
    And the session "user.name" equals "ManualSessionSmokeScenario"
    And I discard the oldest request

    # First handled request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ManualSessionSmokeScenario"
    And the event "unhandled" is false
    And the payload field "events.0.session.id" equals the stored value "manual_session_id"
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 0
    And the event "user.id" equals "123"
    And the event "user.email" equals "ABC.CBA.CA"
    And the event "user.name" equals "ManualSessionSmokeScenario"
    And I discard the oldest request

    # Second handled request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ManualSessionSmokeScenario"
    And the event "unhandled" is false
    And the event "session" is null
    And the event "user.id" equals "123"
    And the event "user.email" equals "ABC.CBA.CA"
    And the event "user.name" equals "ManualSessionSmokeScenario"
    And I discard the oldest request

    # First unhandled request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ManualSessionSmokeScenario"
    And the event "unhandled" is true
    And the payload field "events.0.session.id" equals the stored value "manual_session_id"
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 1
    And the event "user.id" equals "123"
    And the event "user.email" equals "ABC.CBA.CA"
    And the event "user.name" equals "ManualSessionSmokeScenario"