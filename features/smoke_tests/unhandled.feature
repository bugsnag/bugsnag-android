Feature: Unhandled smoke tests

Scenario: Unhandled Java Exception with loaded configuration
    When I run "UnhandledJavaLoadedConfigScenario" and relaunch the app
    And I configure Bugsnag for "UnhandledJavaLoadedConfigScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Exception details
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledJavaLoadedConfigScenario"
    And the exception "type" equals "android"
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "unhandledException"
    And the event "severityReason.unhandledOverridden" is false
    And the error payload field "events.0.projectPackages" is a non-empty array
    And the event "projectPackages.0" equals "com.bugsnag.android.mazerunner"

    # Stacktrace validation
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" ends with "UnhandledJavaLoadedConfigScenario.startScenario"
    And the exception "stacktrace.0.file" equals "UnhandledJavaLoadedConfigScenario.java"
    # R8 minification alters the lineNumber, see the mapping file/source code for the original value
    And the event "exceptions.0.stacktrace.0.lineNumber" equals 7
    And the event "exceptions.0.stacktrace.0.inProject" is true

    And the thread with name "main" contains the error reporting flag
    And the "method" of stack frame 0 equals "com.bugsnag.android.mazerunner.scenarios.UnhandledJavaLoadedConfigScenario.startScenario"
    And the error payload field "events.0.threads.0.stacktrace.0.method" equals "com.bugsnag.android.mazerunner.scenarios.UnhandledJavaLoadedConfigScenario.startScenario"

    # App data
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "mazerunner"
    And the event "app.type" equals "android"
    And the event "app.version" equals "1.1.14"
    And the event "app.versionCode" equals 34
    And the error payload field "events.0.app.duration" is an integer
    And the error payload field "events.0.app.durationInForeground" is an integer
    And the event "app.inForeground" is true
    And the event "app.isLaunching" is true
    And the error payload field "events.0.metaData.app.memoryUsage" is greater than 0

    # Metadata
    And the event "metaData.app.name" equals "MazeRunner"
    And the event "metaData.app.lowMemory" is false
    And the event "metaData.TestData.password" equals "[REDACTED]"

    # Device data
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
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
    And the error payload field "events.0.device.totalMemory" is greater than 0
    And the error payload field "events.0.device.freeDisk" is greater than 0
    And the error payload field "events.0.device.freeMemory" is greater than 0
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

    # Breadcrumbs
    And the event has a "state" breadcrumb named "Bugsnag loaded"

    # Threads validation
    And the error payload field "events.0.threads" is a non-empty array
    And the error payload field "events.0.threads.0.id" is an integer
    And the event "threads.0.name" is not null
    And the event "threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace" is a non-empty array
    And the event "threads.0.stacktrace.0.method" is not null
    And the event "threads.0.stacktrace.0.file" is not null
    And the event "threads.0.stacktrace.0.lineNumber" is not null

Scenario: Signal raised with overwritten config
    When I run "CXXSignalSmokeScenario" and relaunch the app
    And I configure Bugsnag for "CXXSignalSmokeScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Exception details
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "signal"
    And the event "severityReason.attributes.signalType" equals "SIGSEGV"
    And the event "severityReason.type" equals "signal"
    And the event "severityReason.attributes.signalType" equals "SIGSEGV"
    And the event "severityReason.unhandledOverridden" is false
    And the event "session.events.handled" equals 0
    And the event "session.events.unhandled" equals 1

    # Stacktrace validation
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" is not null
    And the event "exceptions.0.stacktrace.0.file" is not null
    And the error payload field "events.0.exceptions.0.stacktrace.0.frameAddress" is greater than 0
    And the error payload field "events.0.exceptions.0.stacktrace.0.symbolAddress" is greater than 0
    And the error payload field "events.0.exceptions.0.stacktrace.0.loadAddress" is greater than 0
    And the error payload field "events.0.exceptions.0.stacktrace.0.lineNumber" is greater than 0

    # App data
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "CXXSignalSmokeScenario"
    And the event "app.type" equals "Overwritten"
    And the event "app.version" equals "9.9.9"
    And the event "app.versionCode" equals 999
    And the error payload field "events.0.app.duration" is an integer
    And the error payload field "events.0.app.durationInForeground" is an integer
    And the event "app.inForeground" is true
    And the event "app.isLaunching" is true
    And the event "metaData.app.name" equals "MazeRunner"

    # Device data
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
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
    And the error payload field "events.0.device.totalMemory" is greater than 0
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
    And the event has a "manual" breadcrumb named "CXXSignalSmokeScenario"
    And the event has a "request" breadcrumb named "Substandard nacho error"

    # Native context override
    And the event "context" equals "Some custom context"

    # Metadata
    And the event "metaData.Riker Ipsum.examples" equals "I'll be sure to note that in my log. You enjoyed that. They wer"
    And the event "metaData.fruit.apple" equals "gala"
    And the event "metaData.fruit.ripe" is true
    And the event "metaData.fruit.counters" equals 47

Scenario: C++ exception thrown with overwritten config
    When I run "CXXExceptionSmokeScenario" and relaunch the app
    And I configure Bugsnag for "CXXExceptionSmokeScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    # Exception details
    And the error payload field "events" is an array with 1 elements
    And the exception "message" equals "How about NO"
    And the exception "type" equals "c"
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "signal"
    And the event "severityReason.unhandledOverridden" is false
    And the event "session.events.handled" equals 0
    And the event "session.events.unhandled" equals 1

    # Stacktrace validation
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" is not null
    And the event "exceptions.0.stacktrace.0.file" is not null
    And the error payload field "events.0.exceptions.0.stacktrace.0.frameAddress" is greater than 0
    And the error payload field "events.0.exceptions.0.stacktrace.0.symbolAddress" is greater than 0
    And the error payload field "events.0.exceptions.0.stacktrace.0.loadAddress" is greater than 0
    And the error payload field "events.0.exceptions.0.stacktrace.0.lineNumber" is greater than 0

    # App data
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "CXXExceptionSmokeScenario"
    And the event "app.type" equals "Overwritten"
    And the event "app.version" equals "9.9.9"
    And the event "app.versionCode" equals 999
    And the error payload field "events.0.app.duration" is an integer
    And the error payload field "events.0.app.durationInForeground" is an integer
    And the event "app.inForeground" is true
    And the event "app.isLaunching" is true
    And the event "metaData.app.name" equals "MazeRunner"

    # Device data
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
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
    And the error payload field "events.0.device.totalMemory" is greater than 0
    And the event "device.orientation" equals "portrait"
    And the event "device.time" is a timestamp

    # Metadata
    And the event "metaData.device.locationStatus" is not null
    And the event "metaData.device.emulator" is false
    And the event "metaData.device.networkAccess" is not null
    And the event "metaData.device.screenDensity" is not null
    And the event "metaData.device.dpi" is not null
    And the event "metaData.device.screenResolution" is not null
    And the event "metaData.device.brand" is not null

    # Context
    And the event "context" equals "Everest"

    # User
    And the event "user.id" equals "ABC"
    And the event "user.email" equals "ABC@CBA.CA"
    And the event "user.name" equals "CXXExceptionSmokeScenario"

    # Breadcrumbs
    And the event has a "manual" breadcrumb named "CXXExceptionSmokeScenario"

@skip_android_8_1
Scenario: ANR detection
    When I run "JvmAnrLoopScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
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
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.id" equals "com.bugsnag.android.mazerunner"
    And the event "app.releaseStage" equals "mazerunner"
    And the event "app.type" equals "android"
    And the event "app.version" equals "1.1.14"
    And the event "app.versionCode" equals 34
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
    And the event "user.id" is not null
    And the error payload field "events.0.user.id" equals the stored value "device_id"

    # Threads validation
    And the error payload field "events.0.threads" is a non-empty array
    And the error payload field "events.0.threads.0.id" is an integer
    And the event "threads.0.name" is not null
    And the event "threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace" is a non-empty array
    And the event "threads.0.stacktrace.0.method" is not null
    And the event "threads.0.stacktrace.0.file" is not null
    And the event "threads.0.stacktrace.0.lineNumber" is not null

    # Metadata validation
    And the event "metaData.custom.global" equals "present in global metadata"
    And the event "metaData.custom.local" equals "present in local metadata"
