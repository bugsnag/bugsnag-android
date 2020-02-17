Feature: Loading values into the configuration



Scenario: Load configuration initialised with Kotlin
    When I configure the app to run in the "skipBugsnag" state
    And I run "LoadConfigurationKotlinScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "12312312312312312312312312312312"
    And the exception "message" equals "LoadConfigurationKotlinScenario"
    And the event "app.releaseStage" equals "testing"
    And the payload field "events.0.breadcrumbs" is an array with 1 elements
    And the event "metaData.test.foo" equals "bar"
    And the event "metaData.test.filter_me" equals "[REDACTED]"
    And the event "app.versionCode" equals 753
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.version" equals "7.5.3"
    And the event "app.type" equals "test"
    And the payload field "events.0.threads" is a non-empty array

