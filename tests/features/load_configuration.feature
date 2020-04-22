Feature: Loading values into the configuration

Scenario: Load configuration initialised from the Manifest
    When I configure the app to run in the "skipBugsnag" state
    And I run "LoadConfigurationFromManifestScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "abc12312312312312312312312312312"
    And the exception "message" equals "LoadConfigurationFromManifestScenario"
    And the event "app.releaseStage" equals "testing"
    And the payload field "events.0.breadcrumbs" is an array with 1 elements
    And the event "metaData.test.foo" equals "bar"
    And the event "metaData.test.filter_me" equals "[REDACTED]"
    And the event "app.versionCode" equals 753
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.version" equals "7.5.3"
    And the event "app.type" equals "test"
    And the payload field "events.0.threads" is a non-empty array

Scenario: Load configuration initialised with Kotlin
    When I configure the app to run in the "skipBugsnag" state
    And I run "LoadConfigurationKotlinScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "45645645645645645645645645645645"
    And the exception "message" equals "LoadConfigurationKotlinScenario"
    And the event "app.releaseStage" equals "kotlin"
    And the payload field "events.0.breadcrumbs" is an array with 1 elements
    And the event "metaData.test.filter_me" equals "bar"
    And the event "metaData.test.filter_me_two" equals "[REDACTED]"
    And the event "app.versionCode" equals 98
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.version" equals "0.9.8"
    And the event "app.type" equals "kotlin"
    And the payload field "events.0.threads" is an array with 0 elements

Scenario: Load configuration initialised with nulls
    When I configure the app to run in the "skipBugsnag" state
    And I run "LoadConfigurationNullsScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "12312312312312312312312312312312"
    And the exception "message" equals "LoadConfigurationNullsScenario"
    And the event "app.releaseStage" equals "production"
    And the event "metaData.test.foo" equals "bar"
    And the event "metaData.test.filter_me" equals "foobar"
    And the event "app.versionCode" equals 34
    And the event "app.buildUUID" is not null
    And the event "app.version" equals "1.1.14"
    And the event "app.type" is null
    And the event "user.id" is not null
    And the event "user.name" is null
    And the event "user.email" is null
    And the payload field "events.0.threads" is a non-empty array

