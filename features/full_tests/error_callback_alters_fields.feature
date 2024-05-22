Feature: When the api key is altered in an Event the JSON payload reflects this

  Background:
    Given I clear all persistent data

  Scenario: Crash exception with altered event details
    When I configure the app to run in the "crash" state
    And I run "UnhandledExceptionEventDetailChangeScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledExceptionApiKeyChangeScenario"
    And I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the exception "message" equals "something broke"
    And the error payload field "apiKey" equals "0000111122223333aaaabbbbcccc9999"
    And the error "Bugsnag-Api-Key" header equals "0000111122223333aaaabbbbcccc9999"
    And the event "severity" equals "error"
    And the event "context" equals "new-context"
    And the event "groupingHash" equals "groupingHash1"
    And the event "user.id" equals "abc"
    And the event "user.email" equals "joe@test.com"
    And the event "user.name" equals "Joe"
    And the event "metaData.custom_data2.test_data" equals "this is test"
    And the event "metaData.custom_data1" is null
    And the event "metaData.custom_data2.data" is null
    And the event "metaData.custom_data3.test data" equals "divert all available power to the crash reporter"
    And event 0 contains the feature flag "beta" with variant "b"
    And event 0 does not contain the feature flag "alpha"
    And event 0 contains the feature flag "gamma" with no variant
    And event 0 does not contain the feature flag "test1"
    And event 0 contains the feature flag "test2" with no variant

    # app fields
    And the event "unhandled" is false
    And the event "app.binaryArch" equals "x86"
    And the event "app.id" equals "12345"
    And the event "app.releaseStage" equals "custom"
    And the event "app.version" equals "1.2.3"
    And the event "app.buildUUID" equals "12345678"
    And the event "app.type" equals "android_custom"
    And the event "app.versionCode" equals 123
    And the event "app.duration" equals 123456
    And the event "app.durationInForeground" equals 123456
    And the event "app.inForeground" is false
    And the event "app.isLaunching" is false

    # device fields
    And the event "device.id" equals "12345"
    And the event "device.jailbroken" is true
    And the event "device.locale" equals "en-UK"
    And the event "device.totalMemory" equals 123456
    And the event "device.runtimeVersions.androidApiLevel" equals "30"
    And the event "device.freeDisk" equals 123456
    And the event "device.freeMemory" equals 123456
    And the event "device.orientation" equals "portrait"

    # breadcrumbs fields
    And the event "breadcrumbs.0.type" equals "error"
    And the event "breadcrumbs.0.name" equals "new breadcrumb message"
    And the event "breadcrumbs.0.metaData.foo" equals "data"
    And the event "breadcrumbs.1.type" equals "error"
    And the event "breadcrumbs.1.name" equals "Second breadcrumb message"

  Scenario: Unhandled exception with altered event details
    When I configure the app to run in the "notify" state
    And I run "UnhandledExceptionEventDetailChangeScenario"
    And I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the exception "message" equals "UnhandledExceptionEventDetailChangeScenario"
    And the error payload field "apiKey" equals "0000111122223333aaaabbbbcccc9999"
    And the error "Bugsnag-Api-Key" header equals "0000111122223333aaaabbbbcccc9999"
    And the event "severity" equals "error"
    And the event "context" equals "new-context"
    And the event "groupingHash" equals "groupingHash1"
    And the event "user.id" equals "abc"
    And the event "user.email" equals "joe@test.com"
    And the event "user.name" equals "Joe"
    And the event "metaData.custom_data2.test_data" equals "this is test"
    And the event "metaData.custom_data1" is null
    And the event "metaData.custom_data2.data" is null
    And the event "metaData.custom_data3.test data" equals "divert all available power to the crash reporter"
    And event 0 contains the feature flag "beta" with variant "b"
    And event 0 does not contain the feature flag "alpha"
    And event 0 contains the feature flag "gamma" with no variant
    And event 0 does not contain the feature flag "test1"
    And event 0 contains the feature flag "test2" with no variant

    # app fields
    And the event "unhandled" is false
    And the event "app.binaryArch" equals "x86"
    And the event "app.id" equals "12345"
    And the event "app.releaseStage" equals "custom"
    And the event "app.version" equals "1.2.3"
    And the event "app.buildUUID" equals "12345678"
    And the event "app.type" equals "android_custom"
    And the event "app.versionCode" equals 123
    And the event "app.duration" equals 123456
    And the event "app.durationInForeground" equals 123456
    And the event "app.inForeground" is false
    And the event "app.isLaunching" is false

    # device fields
    And the event "device.id" equals "12345"
    And the event "device.jailbroken" is true
    And the event "device.locale" equals "en-UK"
    And the event "device.totalMemory" equals 123456
    And the event "device.runtimeVersions.androidApiLevel" equals "30"
    And the event "device.freeDisk" equals 123456
    And the event "device.freeMemory" equals 123456
    And the event "device.orientation" equals "portrait"

    # breadcrumbs fields
    And the event "breadcrumbs.0.type" equals "error"
    And the event "breadcrumbs.0.name" equals "new breadcrumb message"
    And the event "breadcrumbs.0.metaData.foo" equals "data"
    And the event "breadcrumbs.1.type" equals "error"
    And the event "breadcrumbs.1.name" equals "Second breadcrumb message"