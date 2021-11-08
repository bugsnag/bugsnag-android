Feature: Legacy struct is converted to an error payload

    Scenario: Sending a legacy NDK struct
        When I run "CXXPersistLegacyStructScenario"
        And I wait to receive an error
        And the error payload field "apiKey" equals "5d1e5fbd39a74caa1200142706a90b20"
        And the event "unhandled" is true
        And the event "context" equals "SomeActivity"
        And the event "groupingHash" equals "foo-hash"
        And the event "severity" equals "error"

        # error
        And the event "exceptions.0.errorClass" equals "SIGBUS"
        And the event "exceptions.0.message" equals "POSIX is serious about oncoming traffic"
        And the event "exceptions.0.stacktrace.0.frameAddress" equals 454379
        And the event "exceptions.0.stacktrace.1.frameAddress" equals 342334
        And the event "exceptions.0.stacktrace.0.method" equals "makinBacon"
        And the event "exceptions.0.type" equals "c"

        # app
        And the event "app.id" equals "com.example.PhotoSnapPlus"
        And the event "app.type" equals "android"
        And the event "app.binaryArch" equals "x86"
        And the event "app.releaseStage" equals "リリース"
        And the event "app.version" equals "2.0.52"
        And the event "app.buildUUID" equals "1234-9876-adfe"
        And the event "app.versionCode" equals 57
        # TODO See PLAT-7585
        #And the event "app.duration" equals 6502
        #And the event "app.durationInForeground" equals 3822
        And the event "app.inForeground" is true
        And the event "app.isLaunching" is true

        # device
        And the event "device.manufacturer" equals "HI-TEC™"
        And the event "device.model" equals "Rasseur"
        And the event "device.locale" equals "en_AU#Melbun"
        And the event "device.id" equals "device-id-123"
        And the event "device.orientation" equals "landscape"
        And the event "device.osName" equals "android"
        And the event "device.osVersion" equals "11.50.2"
        And the event "device.totalMemory" equals 234678100
        And the event "device.jailbroken" is true
        And the event "device.time" equals "2017-10-27T13:00:34Z"
        And the event "device.cpuAbi.0" equals "x86"
        And the event "device.runtimeVersions.androidApiLevel" equals "27"
        And the event "device.runtimeVersions.osBuild" equals "custom_build"

        # user
        And the event "user.id" equals "fex"
        And the event "user.email" equals "fenton@io.example.com"
        And the event "user.name" equals "Fenton"

        # metadata
        And the event "metaData.app.activeScreen" equals "ExampleActivity"
        And the event "metaData.metrics.experimentX" is false
        And the event "metaData.metrics.subject" equals "percy"
        And the event "metaData.app.weather" equals "rain"

        # session
        And the event "session.events.handled" equals 2
        And the event "session.events.unhandled" equals 1
        And the event "session.id" equals "f1ab"
        And the event "session.startedAt" equals "2019-03-19T12:58:19+00:00"

        # breadcrumbs
        And the event "breadcrumbs.0.name" equals "decrease torque"
        And the event "breadcrumbs.0.type" equals "state"
        And the event "breadcrumbs.0.timestamp" equals "2018-08-29T21:41:39Z"
        And the event "breadcrumbs.0.metaData.message" equals "Moving laterally 26º"
        And the event "breadcrumbs.1" is null
