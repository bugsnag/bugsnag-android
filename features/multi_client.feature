Feature: Multi-client support

    Multiple Bugsnag clients can be configured, each with its own API key and
    configuration options. A report which is captured by a given client should
    use the correct API key and configuration options.

Scenario: An unhandled error captured while offline is detected by two clients with different API keys
    When I run "MultiClientApiKeyScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "MultiClientApiKeyScenario"
    And I set environment variable "EVENT_METADATA" to "DeliverReport"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 2 requests

    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa" for request 0
    And the payload field "apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa" for request 0
    And the "Bugsnag-API-Key" header equals "abc123" for request 1
    And the payload field "apiKey" equals "abc123" for request 1

