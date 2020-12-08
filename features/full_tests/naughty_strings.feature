Feature: The notifier handles user data containing unusual strings

Scenario: Test handled JVM error
    When I run "NaughtyStringScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "NaughtyStringScenario"
    And the payload field "events.0.metaData.custom" is not null

Scenario: Test unhandled NDK error
    When I run "CXXNaughtyStringsScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I configure Bugsnag for "CXXNaughtyStringsScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGABRT"
    And the exception "message" equals "Abort program"
    And the payload field "events.0.metaData.custom" is not null
