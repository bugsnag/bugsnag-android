# This scenario is in its own file and folder so that it can be run first on Android 4
Feature: ANR smoke test

  Background:
    Given I clear all persistent data

  @anr
  @skip_android_10
  Scenario: ANR detection
    When I set the screen orientation to portrait
    And I clear any error dialogue
    And I run "JvmAnrLoopScenario"
    And I wait for 1 seconds
    And I tap the screen 3 times
    And I wait for 5 seconds
    And I tap the screen 3 times
    And I wait to receive an error

    # Exception details
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the exception "type" equals "android"
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "anrError"
    And the event "severityReason.unhandledOverridden" is false

    # Stacktrace validation
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" is not null
    And the event "exceptions.0.stacktrace.0.file" is not null
    And the event "exceptions.0.stacktrace.0.lineNumber" is not null

    # App data
    And the event binary arch field is valid
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "mazerunner"
    And the event "app.type" equals "android"
    And the event "app.version" equals "1.1.14"
    And the event "app.versionCode" equals 1
    And the error payload field "events.0.app.duration" is an integer
    And the error payload field "events.0.app.durationInForeground" is an integer
    And the event "app.inForeground" is true
    And the event "app.isLaunching" is false
    And the event "metaData.app.name" equals "MazeRunner"

    # Device data
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "device.jailbroken" is false
    And the event "device.id" is not null
    And the error payload field "events.0.device.id" is stored as the value "device_id"
    And the event "device.locale" is not null
    And the event "device.manufacturer" is not null
    And the event "device.model" is not null
    And the event "device.osName" equals "android"
    And the event "device.osVersion" is not null
    And the event "device.runtimeVersions" is not null
    And the event "device.runtimeVersions.androidApiLevel" is not null
    And the event "device.runtimeVersions.osBuild" is not null
    And the error payload field "events.0.device.totalMemory" is greater than 0
    And the error payload field "events.0.device.freeDisk" is greater than 0
    And the error payload field "events.0.device.freeMemory" is greater than 0
    And the event "device.orientation" matches "(portrait|landscape)"
    And the event "device.time" is a timestamp
    And the event "metaData.device.locationStatus" is not null
    And the event "metaData.device.emulator" is false
    And the event "metaData.device.networkAccess" is not null
    And the event "metaData.device.screenDensity" is not null
    And the event "metaData.device.dpi" is not null
    And the event "metaData.device.screenResolution" is not null
    And the event "metaData.device.brand" is not null

    # User
    And the event "user.id" is not null
    And the error payload field "events.0.user.id" equals the stored value "device_id"

    # Threads validation
    And the error payload field "events.0.threads" is a non-empty array
    And the event "threads.0.id" matches "^[0-9]+$"
    And the event "threads.0.name" is not null
    And the event "threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace" is a non-empty array
    And the event "threads.0.stacktrace.0.method" is not null
    And the event "threads.0.stacktrace.0.file" is not null
    And the event "threads.0.stacktrace.0.lineNumber" is not null

    # Metadata validation
    And the event "metaData.custom.global" equals "present in global metadata"
    And the event "metaData.custom.local" equals "present in local metadata"
