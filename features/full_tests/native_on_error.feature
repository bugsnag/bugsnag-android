Feature: Native on error callbacks are invoked

    Scenario: on_error alters error report information
        When I run "CXXSignalOnErrorTrueScenario" and relaunch the app
        And I configure Bugsnag for "CXXSignalOnErrorTrueScenario"
        And I wait to receive an error

        # app
        And the event "app.binaryArch" equals "custom_binary_arch"
        And the event "app.buildUUID" equals "custom_build_uuid"
        # TODO See PLAT-7585
        #And the event "app.duration" equals 100
        #And the event "app.durationInForeground" equals 200
        And the event "app.id" equals "custom_app_id"
        And the event "app.inForeground" is false
        And the event "app.isLaunching" is false
        And the event "app.releaseStage" equals "custom_release_stage"
        And the event "app.type" equals "custom_type"
        And the event "app.version" equals "custom_version"
        And the event "app.versionCode" equals "0x38"

        # device
        And the event "device.jailbroken" is true
        And the event "device.id" equals "custom_device_id"
        And the event "device.locale" equals "zh_HK"
        And the event "device.manufacturer" equals "custom_manufacturer"
        And the event "device.model" equals "custom_model"
        And the event "device.osVersion" equals "custom_os_version"
        # TODO PLAT-7497
        # And the event "device.totalMemory" equals 99999999
        And the event "device.orientation" equals "custom_orientation"
        And the event "device.time" is a timestamp
        And the event "device.osName" equals "custom_os_name"

        # user
        And the event "user.id" equals "custom_id"
        And the event "user.email" equals "custom_email"
        And the event "user.name" equals "custom_name"

        # metadata
        And the event "metaData.custom.double" equals 5
        And the event "metaData.custom2.string" equals "some_value"
        And the event "metaData.custom3.bool" is false

        # stacktrace
        And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "exceptions.0.stacktrace.0.method" equals "bar()"
        And the event "exceptions.0.stacktrace.0.file" equals "foo.cpp"
        And the error payload field "events.0.exceptions.0.stacktrace.0.frameAddress" equals 20
        And the error payload field "events.0.exceptions.0.stacktrace.0.loadAddress" equals 40
        And the error payload field "events.0.exceptions.0.stacktrace.0.symbolAddress" equals 60
        And the error payload field "events.0.exceptions.0.stacktrace.0.lineNumber" equals 28

        # error
        And the exception "errorClass" equals "custom_error_class"
        And the exception "message" equals "custom_error_message"

        # misc
        And the event "severity" equals "info"
        And the event "unhandled" is false
        And the event "groupingHash" equals "custom_grouping_hash"

    Scenario: on_error returning false prevents C signal being reported
        When I run "CXXSignalOnErrorFalseScenario" and relaunch the app
        And I configure Bugsnag for "CXXSignalOnErrorFalseScenario"
        Then Bugsnag confirms it has no errors to send

    Scenario: on_error returning false prevents C++ exception being reported
        When I run "CXXExceptionOnErrorFalseScenario" and relaunch the app
        And I configure Bugsnag for "CXXExceptionOnErrorFalseScenario"
        Then Bugsnag confirms it has no errors to send

    Scenario: Removing on_error callback
        When I run "CXXRemoveOnErrorScenario" and relaunch the app
        And I configure Bugsnag for "CXXRemoveOnErrorScenario"
        And I wait to receive an error
        Then the error payload contains a completed handled native report
        And the event "user.id" equals "default"
        And the event "user.email" equals "default@default.df"
        And the event "user.name" equals "default"
