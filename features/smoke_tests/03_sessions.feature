Feature: Session functionality smoke tests

  Background:
    Given I clear all persistent data

  @debug-safe
  Scenario: Automated sessions send
    When I run "AutoSessionSmokeScenario"
    And I wait to receive a session

    # Session payload
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the session payload field "sessions" is an array with 1 elements
    And the session "user.id" is not null
    And the session payload field "sessions.0.user.id" is stored as the value "automated_user_id"
    And the session "id" is not null
    And the session payload field "sessions.0.id" is stored as the value "automated_session_id"
    And the session "startedAt" is not null

    # App data
    And the session payload field "app.buildUUID" equals "test-7.5.3"
    And the session payload field "app.id" equals "com.bugsnag.android.mazerunner"
    And the session payload field "app.releaseStage" equals "mazerunner"
    And the session payload field "app.type" equals "android"
    And the session payload field "app.version" equals "1.1.14"
    And the session payload field "app.versionCode" equals 1

    # Device data
    And the session payload field "device.cpuAbi" is a non-empty array
    And the session payload field "device.jailbroken" is false
    And the session payload field "device.id" is not null
    And the session payload field "device.id" equals the stored value "automated_user_id"
    And the session payload field "device.locale" is not null
    And the session payload field "device.manufacturer" is not null
    And the session payload field "device.model" is not null
    And the session payload field "device.osName" equals "android"
    And the session payload field "device.osVersion" is not null
    And the session payload field "device.runtimeVersions" is not null
    And the session payload field "device.totalMemory" is greater than 0

    # Error payload
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "AutoSessionSmokeScenario"
    And the event "context" equals "SecondActivity"
    And the event "metaData.app.activeScreen" equals "SecondActivity"
    And the event "user.id" is not null
    And the event "device.id" is not null
    And the error payload field "events.0.user.id" equals the stored value "automated_user_id"
    And the error payload field "events.0.device.id" equals the stored value "automated_user_id"
    And the event "session.id" is not null
    And the error payload field "events.0.session.id" equals the stored value "automated_session_id"
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 0
    And the event "severityReason.unhandledOverridden" is false

    And the event has a "state" breadcrumb named "SecondActivity#onCreate()"
    And the breadcrumb named "SecondActivity#onCreate()" has "metaData.action" equal to "com.bugsnag.android.mazerunner.UPDATE_CONTEXT"
    And the breadcrumb named "SecondActivity#onCreate()" has "metaData.hasBundle" is false
    And the breadcrumb named "SecondActivity#onCreate()" has "metaData.hasExtras" is false

  @debug-safe
  Scenario: Manual session control works
    When I run "ManualSessionSmokeScenario" and relaunch the crashed app
    And I configure Bugsnag for "ManualSessionSmokeScenario"
    And I wait to receive a session

    # Session payload
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session payload field "device.runtimeVersions.androidApiLevel" is not null
    And the session payload field "device.runtimeVersions.osBuild" is not null
    And the session payload field "sessions.0.id" is stored as the value "manual_session_id"
    And the session "user.id" equals "123"
    And the session "user.email" equals "ABC.CBA.CA"
    And the session "user.name" equals "ManualSessionSmokeScenario"

    Then I wait to receive 3 errors

    # First handled request
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ManualSessionSmokeScenario"
    And the event "unhandled" is false
    And the error payload field "events.0.session.id" equals the stored value "manual_session_id"
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 0
    And the event "severityReason.unhandledOverridden" is false
    And the event "user.id" equals "123"
    And the event "user.email" equals "ABC.CBA.CA"
    And the event "user.name" equals "ManualSessionSmokeScenario"
    And I discard the oldest error

    # Second handled request
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ManualSessionSmokeScenario"
    And the event "unhandled" is false
    And the event "session" is null
    And the event "user.id" equals "123"
    And the event "user.email" equals "ABC.CBA.CA"
    And the event "user.name" equals "ManualSessionSmokeScenario"
    And I discard the oldest error

    # First unhandled request
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ManualSessionSmokeScenario"
    And the event "unhandled" is true
    And the error payload field "events.0.session.id" equals the stored value "manual_session_id"
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 1
    And the event "severityReason.unhandledOverridden" is false
    And the event "user.id" equals "123"
    And the event "user.email" equals "ABC.CBA.CA"
    And the event "user.name" equals "ManualSessionSmokeScenario"

  Scenario: Start session in auto mode
    When I clear any error dialogue
    And I run "StartSessionAutoModeScenario"
    And I relaunch the app after a crash
    Then I wait to receive a session