Feature: Reporting errors in multi process apps

  Background:
    Given I clear all persistent data

  Scenario: Handled JVM error
    When I run "MultiProcessHandledExceptionScenario"
    Then I wait to receive 2 errors
    And I sort the errors by the payload field "events.0.metaData.app.processName"
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "MultiProcessHandledExceptionScenario"
    And the event "unhandled" is false
    And the error payload field "events.0.metaData.app.processName" equals "com.bugsnag.android.mazerunner"
    And the error payload field "events.0.device.id" is stored as the value "first_device_id"
    And the error payload field "events.0.user.id" equals "2"
    And the error payload field "events.0.user.name" equals "MultiProcessHandledExceptionScenario"
    And the error payload field "events.0.user.email" equals "background@example.com"

    Then I discard the oldest error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "MultiProcessHandledExceptionScenario"
    And the event "unhandled" is false
    And the error payload field "events.0.metaData.app.processName" equals "com.example.bugsnag.android.mazerunner.multiprocess"

    # device ID is shared between processes
    And the error payload field "events.0.device.id" equals the stored value "first_device_id"
    And the error payload field "events.0.user.id" equals "1"
    And the error payload field "events.0.user.name" equals "MultiProcessHandledExceptionScenario"
    And the error payload field "events.0.user.email" equals "foreground@example.com"

  # Skipped pending PLAT-12145
  @skip
  Scenario: Unhandled JVM error
    And I configure the app to run in the "main-activity" state
    When I run "MultiProcessUnhandledExceptionScenario" and relaunch the crashed app
    And I configure the app to run in the "multi-process-service" state
    And I run "MultiProcessUnhandledExceptionScenario" and relaunch the crashed app
    And I run "MultiProcessUnhandledExceptionScenario"
    Then I wait to receive 2 errors
    And I sort the errors by the payload field "events.0.metaData.app.processName"
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "MultiProcessUnhandledExceptionScenario"
    And the event "unhandled" is true
    And the error payload field "events.0.metaData.app.processName" equals "com.bugsnag.android.mazerunner"

    Then I discard the oldest error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "MultiProcessUnhandledExceptionScenario"
    And the event "unhandled" is true
    And the error payload field "events.0.metaData.app.processName" equals "com.example.bugsnag.android.mazerunner.multiprocess"

  Scenario: Handled NDK error
    When I run "MultiProcessHandledCXXErrorScenario"
    Then I wait to receive 2 errors
    And I sort the errors by the payload field "events.0.metaData.app.processName"
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload contains a completed handled native report
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "activate"
    And the exception "message" equals "MultiProcessHandledCXXErrorScenario"
    And the event "unhandled" is false
    And the error payload field "events.0.metaData.app.processName" equals "com.bugsnag.android.mazerunner"
    And the error payload field "events.0.device.id" is stored as the value "first_device_id"
    And the error payload field "events.0.user.id" equals the stored value "first_device_id"

    Then I discard the oldest error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload contains a completed handled native report
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "activate"
    And the exception "message" equals "MultiProcessHandledCXXErrorScenario"
    And the event "unhandled" is false
    And the error payload field "events.0.metaData.app.processName" equals "com.example.bugsnag.android.mazerunner.multiprocess"

    # device ID is shared between processes
    And the error payload field "events.0.device.id" equals the stored value "first_device_id"
    And the error payload field "events.0.user.id" equals the stored value "first_device_id"

  Scenario: Unhandled NDK error
    And I configure the app to run in the "main-activity" state
    When I run "MultiProcessUnhandledCXXErrorScenario" and relaunch the crashed app
    And I configure the app to run in the "multi-process-service" state
    And I run "MultiProcessUnhandledCXXErrorScenario" and relaunch the crashed app
    And I run "MultiProcessUnhandledCXXErrorScenario"
    Then I wait to receive 2 errors
    And I sort the errors by the payload field "events.0.metaData.app.processName"
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload contains a completed handled native report
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGABRT"
    And the exception "message" equals "Abort program"
    And the event "unhandled" is true
    And the error payload field "events.0.metaData.app.processName" equals "com.bugsnag.android.mazerunner"
    And the error payload field "events.0.device.id" is stored as the value "first_device_id"
    And the error payload field "events.0.user.id" equals "2"
    And the error payload field "events.0.user.name" equals "MultiProcessUnhandledCXXErrorScenario"
    And the error payload field "events.0.user.email" equals "2@example.com"

    Then I discard the oldest error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload contains a completed handled native report
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGABRT"
    And the exception "message" equals "Abort program"
    And the event "unhandled" is true
    And the error payload field "events.0.metaData.app.processName" equals "com.example.bugsnag.android.mazerunner.multiprocess"
    And the error payload field "events.0.device.id" equals the stored value "first_device_id"
    And the error payload field "events.0.user.id" equals "1"
    And the error payload field "events.0.user.name" equals "MultiProcessUnhandledCXXErrorScenario"
    And the error payload field "events.0.user.email" equals "1@test.com"

  Scenario: User/device information is migrated from SharedPreferences
    When I run "SharedPrefMigrationScenario"
    Then I wait to receive 2 errors
    And I sort the errors by the payload field "events.0.metaData.app.processName"
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "SharedPrefMigrationScenario"
    And the event "unhandled" is false
    And the error payload field "events.0.metaData.app.processName" equals "com.bugsnag.android.mazerunner"
    And the error payload field "events.0.device.id" equals "267160a7-5cf2-42d4-be21-969f1573ecb0"
    And the error payload field "events.0.user.id" equals "3"
    And the error payload field "events.0.user.name" equals "SharedPrefMigrationScenario"
    And the error payload field "events.0.user.email" equals "3@example.com"

    Then I discard the oldest error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "SharedPrefMigrationScenario"
    And the event "unhandled" is false
    And the error payload field "events.0.metaData.app.processName" equals "com.example.bugsnag.android.mazerunner.multiprocess"

    # device ID is shared between processes
    And the error payload field "events.0.device.id" equals "267160a7-5cf2-42d4-be21-969f1573ecb0"
    And the error payload field "events.0.user.id" equals "4"
    And the error payload field "events.0.user.name" equals "SharedPrefMigrationScenario"
    And the error payload field "events.0.user.email" equals "4@example.com"
