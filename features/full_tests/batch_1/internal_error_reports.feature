Feature: Cached Error Reports

Scenario: If an exception is thrown when sending errors/sessions then internal error reports should be sent
    When I set the screen orientation to portrait
    And I run "InternalErrorScenario"
    And I wait to receive 1 errors

    # Validate internal error report for error serialization
    Then the error "Bugsnag-Internal-Error" header equals "true"

    # Exception details
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.IllegalStateException"
    And the exception "message" equals "Mazerunner threw exception serializing error"
    And the exception "type" equals "android"
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "unhandledException"

    # Internal error specific metadata
    And the event "metaData.BugsnagDiagnostics.filename" is not null
    And the event "metaData.BugsnagDiagnostics.notifierName" equals "Android Bugsnag Notifier"
    And the event "metaData.BugsnagDiagnostics.apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the event "metaData.BugsnagDiagnostics.canRead" is true
    And the event "metaData.BugsnagDiagnostics.exists" is true
    And the event "metaData.BugsnagDiagnostics.canWrite" is true
    And the error payload field "events.0.metaData.BugsnagDiagnostics.usableSpace" is an integer
    And the event "metaData.BugsnagDiagnostics.notifierVersion" is not null
    And the event "metaData.BugsnagDiagnostics.fileLength" equals 0

    # User
    And the event "user.id" is null

    # App data
    And the event "app.buildUUID" is not null
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "mazerunner"
    And the event "app.type" equals "android"
    And the event "app.version" equals "1.1.14"
    And the event "app.versionCode" equals 34
    And the error payload field "events.0.app.duration" is an integer
    And the error payload field "events.0.app.durationInForeground" is an integer
    And the event "app.inForeground" is true

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
    And the event "device.orientation" equals "portrait"
    And the event "device.time" is a timestamp

    # Threads validation
    And the error payload field "events.0.threads" is a non-empty array
    And the error payload field "events.0.threads.0.id" is an integer
    And the event "threads.0.name" is not null
    And the event "threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace" is a non-empty array
    And the event "threads.0.stacktrace.0.method" is not null
    And the event "threads.0.stacktrace.0.file" is not null
    And the event "threads.0.stacktrace.0.lineNumber" is not null
