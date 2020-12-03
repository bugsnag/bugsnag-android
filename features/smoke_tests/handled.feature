Feature: Handled smoke tests

Scenario: Notify caught Java exception with default configuration
    When I run "HandledJavaSmokeScenario"
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Exception details
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.IllegalStateException"
    And the exception "message" equals "HandledJavaSmokeScenario"
    And the exception "type" equals "android"
    And the event "unhandled" is false
    And the event "severity" equals "warning"
    And the event "severityReason.type" equals "handledException"

    # Stacktrace validation
    And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" ends with "HandledJavaSmokeScenario.run"
    And the exception "stacktrace.0.file" equals "HandledJavaSmokeScenario.java"
    And the event "exceptions.0.stacktrace.0.lineNumber" equals 48
    And the event "exceptions.0.stacktrace.0.inProject" is true

    # App data
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "production"
    And the event "app.type" equals "android"
    And the event "app.version" equals "1.1.14"
    And the event "app.versionCode" equals 34
    And the payload field "events.0.app.duration" is an integer
    And the payload field "events.0.app.durationInForeground" is an integer
    And the event "app.inForeground" is true
    And the payload field "events.0.metaData.app.memoryUsage" is greater than 0
    And the event "metaData.app.name" equals "MazeRunner"
    And the event "metaData.app.lowMemory" is false

    # Device data
    And the payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "device.jailbroken" is false
    And the event "device.id" is not null
    And the payload field "events.0.device.id" is stored as the value "device_id"
    And the event "device.locale" is not null
    And the event "device.manufacturer" is not null
    And the event "device.model" is not null
    And the event "device.osName" equals "android"
    And the event "device.osVersion" is not null
    And the event "device.runtimeVersions" is not null
    And the event "device.runtimeVersions.androidApiLevel" is not null
    And the event "device.runtimeVersions.osBuild" is not null
    And the payload field "events.0.device.totalMemory" is greater than 0
    And the payload field "events.0.device.freeDisk" is greater than 0
    And the payload field "events.0.device.freeMemory" is greater than 0
    And the event "device.orientation" equals "portrait"
    And the event "device.time" is a timestamp
    And the event "metaData.device.locationStatus" is not null
    And the event "metaData.device.emulator" is false
    And the event "metaData.device.networkAccess" is not null
    And the event "metaData.device.charging" is not null
    And the event "metaData.device.screenDensity" is not null
    And the event "metaData.device.dpi" is not null
    And the event "metaData.device.screenResolution" is not null
    And the event "metaData.device.brand" is not null
    And the event "metaData.device.batteryLevel" is not null

    # User
    And the event "user.id" is not null
    And the payload field "events.0.user.id" equals the stored value "device_id"

    # Breadcrumbs
    And the event has a "state" breadcrumb named "Bugsnag loaded"
    And the event has a "manual" breadcrumb named "HandledJavaSmokeScenario"
    # [PLAT-5534] A potential source of flakes
    #And the event "breadcrumbs.2.metaData.source" equals "BreadcrumbCallback"

    # MetaData
    And the event "metaData.TestData.ClientMetadata" is true
    And the event "metaData.TestData.CallbackMetadata" is true

    # Threads validation
    And the payload field "events.0.threads" is a non-empty array
    And the payload field "events.0.threads.0.id" is an integer
    And the event "threads.0.name" is not null
    And the event "threads.0.type" equals "android"
    And the payload field "events.0.threads.0.stacktrace" is a non-empty array
    And the event "threads.0.stacktrace.0.method" is not null
    And the event "threads.0.stacktrace.0.file" is not null
    And the event "threads.0.stacktrace.0.lineNumber" is not null

Scenario: Notify Kotlin exception with overwritten configuration
    When I run "HandledKotlinSmokeScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Exception details
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledKotlinSmokeScenario"
    And the exception "type" equals "android"
    And the event "unhandled" is false
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "userCallbackSetSeverity"

    # Stacktrace validation
    And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" ends with "generateScenario"
    And the exception "stacktrace.0.file" equals "Scenario.kt"
    And the event "exceptions.0.stacktrace.0.lineNumber" equals 68
    And the event "exceptions.0.stacktrace.0.inProject" is true

    # Overwritten App data
    And the event "app.releaseStage" equals "HandledKotlinSmokeScenario"
    And the event "app.type" equals "Overwritten"
    And the event "app.version" equals "9.9.9"
    And the event "app.versionCode" equals 999

    # Overwritten User
    And the event "user.id" equals "ABC"
    And the event "user.email" equals "ABC@CBA.CA"
    And the event "user.name" equals "HandledKotlinSmokeScenario"

    # Breadcrumbs
    And the event has a "manual" breadcrumb named "HandledKotlinSmokeScenario"
    # [PLAT-5534] A potential source of flakes
    # And the event "breadcrumbs.2.metaData.Source" equals "HandledKotlinSmokeScenario"

    # MetaData
    And the event "metaData.TestData.Source" equals "HandledKotlinSmokeScenario"
    And the event "metaData.TestData.Callback" is true
    And the event "metaData.TestData.redacted" equals "[REDACTED]"

    # Threads validation
    And the payload field "events.0.threads" is a non-empty array
    And the payload field "events.0.threads.0.id" is an integer
    And the event "threads.0.name" is not null
    And the event "threads.0.type" equals "android"
    And the payload field "events.0.threads.0.stacktrace" is a non-empty array
    And the event "threads.0.stacktrace.0.method" is not null
    And the event "threads.0.stacktrace.0.file" is not null
    And the event "threads.0.stacktrace.0.lineNumber" is not null

Scenario: Handled C functionality
    When I run "CXXNotifySmokeScenario"
    And I wait to receive a request

    # Exception details
    Then the request payload contains a completed handled native report
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "CXXNotifySmokeScenario"
    And the exception "message" equals "Smoke test scenario"
    And the exception "type" equals "c"
    And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "unhandled" is false
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "userCallbackSetSeverity"

    # App data
    And the event "app.buildUUID" is not null
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "production"
    And the event "app.type" equals "android"
    And the event "app.version" equals "1.1.14"
    And the event "app.versionCode" equals 34
    And the payload field "events.0.app.duration" is an integer
    And the payload field "events.0.app.durationInForeground" is an integer
    And the event "app.inForeground" is true

    # Device data
    And the payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "device.jailbroken" is false
    And the event "device.id" is not null
    And the payload field "events.0.device.id" is stored as the value "device_id"
    And the event "device.locale" is not null
    And the event "device.manufacturer" is not null
    And the event "device.model" is not null
    And the event "device.osName" equals "android"
    And the event "device.osVersion" is not null
    And the event "device.runtimeVersions" is not null
    And the event "device.runtimeVersions.androidApiLevel" is not null
    And the event "device.runtimeVersions.osBuild" is not null
    And the payload field "events.0.device.totalMemory" is greater than 0
    And the payload field "events.0.device.freeDisk" is greater than 0
    And the payload field "events.0.device.freeMemory" is greater than 0
    And the event "device.orientation" equals "portrait"
    And the event "device.time" is a timestamp

    # User
    And the event "user.id" equals "324523"
    And the event "user.name" equals "Jack Mill"

    # Breadcrumbs
    And the event has a "log" breadcrumb named "Cold beans detected"

    # MetaData
    And the event "metaData.TestData.Source" equals "ClientCallback"

    # Threads validation
    And the payload field "events.0.threads" is a non-empty array
    And the payload field "events.0.threads.0.id" is an integer
    And the event "threads.0.name" is not null
    And the event "threads.0.type" equals "android"
    And the payload field "events.0.threads.0.stacktrace" is a non-empty array
    And the event "threads.0.stacktrace.0.method" is not null
    And the event "threads.0.stacktrace.0.file" is not null
    And the event "threads.0.stacktrace.0.lineNumber" is not null