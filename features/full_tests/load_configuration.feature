Feature: Loading values into the configuration

  Background:
    Given I clear all persistent data

  Scenario: Load configuration initialised from the Manifest
    When I run "LoadConfigurationFromManifestScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "abc12312312312312312312312312312"
    And the exception "message" equals "LoadConfigurationFromManifestScenario"
    And the event "app.releaseStage" equals "testing"
    And the error payload field "events.0.breadcrumbs" is an array with 1 elements
    And the event "metaData.endpoints.notify" equals "manifest-notify-endpoint"
    And the event "metaData.endpoints.sessions" equals "manifest-sessions-endpoint"
    And the event "metaData.test.foo" equals "bar"
    And the event "metaData.test.filter_me" equals "[REDACTED]"
    And the event "app.versionCode" equals 753
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.version" equals "7.5.3"
    And the event "app.type" equals "test"
    And the error payload field "events.0.threads" is a non-empty array

  Scenario: Load configuration initialised with Kotlin
    When I run "LoadConfigurationKotlinScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "45645645645645645645645645645645"
    And the exception "message" equals "LoadConfigurationKotlinScenario"
    And the event "app.releaseStage" equals "kotlin"
    And the error payload field "events.0.breadcrumbs" is an array with 1 elements
    And the event "metaData.test.filter_me" equals "bar"
    And the event "metaData.test.filter_me_two" equals "[REDACTED]"
    And the event "app.versionCode" equals 98
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.version" equals "0.9.8"
    And the event "app.type" equals "kotlin"
    And the error payload field "events.0.threads" is an array with 0 elements

  Scenario: Load configuration initialised with nulls
    When I run "LoadConfigurationNullsScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "12312312312312312312312312312312"
    And the exception "message" equals "LoadConfigurationNullsScenario"
    And the event "app.releaseStage" is not null
    And the event "metaData.test.foo" equals "bar"
    And the event "metaData.test.filter_me" equals "foobar"
    And the event "app.versionCode" equals 1
    And the event "app.buildUUID" is not null
    And the event "app.version" equals "1.1.14"
    And the event "app.type" is null
    And the event "user.id" is not null
    And the event "user.name" is null
    And the event "user.email" is null
    And the error payload field "events.0.threads" is a non-empty array

