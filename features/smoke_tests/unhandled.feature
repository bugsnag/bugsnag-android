Feature: Unhandled smoke tests

Scenario: Unhandled Java Exception with loaded configuration
    When I run "UnhandledJavaLoadedConfigScenario" and relaunch the app
    And I configure Bugsnag for "UnhandledJavaLoadedConfigScenario"
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Exception details
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledJavaLoadedConfigScenario"
    And the exception "type" equals "android"
    And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "unhandledException"

    # App data
    And the event "app.buildUUID" equals "tests-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "production"
    And the event "app.type" equals "android"
    And the event "app.version" equals "1.1.14"
    And the event "app.versionCode" equals 34
    And the event "app.duration" is not null
    And the event "app.durationInForeground" is not null
    And the event "app.inForeground" is true
    And the payload field "events.0.metaData.app.memoryUsage" is greater than 0
    And the event "metaData.app.name" equals "MazeRunner"
    And the event "metaData.app.lowMemory" is not null

    # Device data
    And the payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "device.jailbroken" is false
    And the event "device.id" is not null
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

Scenario: Signal exception with overwritten config
    When I run "CXXSignalSmokeScenario" and relaunch the app
    And I configure Bugsnag for "CXXSignalSmokeScenario"
    And I wait to receive a request
    Then the request payload contains a completed unhandled native report

    # Exception details
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledJavaLoadedConfigScenario"
    And the exception "type" equals "android"
    And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "unhandledException"

    # App data
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "Overwritten"
    And the event "app.type" equals "android"
    And the event "app.version" equals "9.9.9"
    And the event "app.versionCode" equals 999
    And the event "app.duration" is not null
    And the event "app.durationInForeground" is not null
    And the event "app.inForeground" is true
    And the event "metaData.app.name" equals "MazeRunner"

    # Device data
    And the payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "device.jailbroken" is false
    And the event "device.id" is not null
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
    And the event "metaData.device.screenDensity" is not null
    And the event "metaData.device.dpi" is not null
    And the event "metaData.device.screenResolution" is not null
    And the event "metaData.device.brand" is not null

    # User
    And the event "user.id" equals "ABC"
    And the event "user.email" equals "ABC@CBA.CA"
    And the event "user.name" equals "CXXSignalSmokeScenario"

    # Breadcrumbs
    And the event has a "state" breadcrumb named "Bugsnag loaded"
    And the event has a "manual" breadcrumb named "CXXSignalSmokeScenario"
    And the event "breadcrumbs.2.metaData.Source" equals "CXXSignalSmokeScenario"

@skip_android_8_1
Scenario: ANR detection
    When I run "AppNotRespondingScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    And I wait to receive a request

    # Exception details
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the exception "type" equals "android"
    And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "AndroidNotResponding"

    # App data
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "Overwritten"
    And the event "app.type" equals "android"
    And the event "app.version" equals "9.9.9"
    And the event "app.versionCode" equals 999
    And the event "app.duration" is not null
    And the event "app.durationInForeground" is not null
    And the event "app.inForeground" is true
    And the event "metaData.app.name" equals "MazeRunner"

    # Device data
    And the payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "device.jailbroken" is false
    And the event "device.id" is not null
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
    And the event "metaData.device.screenDensity" is not null
    And the event "metaData.device.dpi" is not null
    And the event "metaData.device.screenResolution" is not null
    And the event "metaData.device.brand" is not null