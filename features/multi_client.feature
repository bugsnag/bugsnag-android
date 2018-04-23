Feature: Multi-client support

    Multiple Bugsnag clients can be configured, each with its own API key and
    configuration options. A report which is captured by a given client should
    use the correct API key and configuration options.

Scenario: Multiple clients send errors stored in the old directory
    When I run "MultiClientOldDirScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "MultiClientOldDirScenario"
    And I set environment variable "EVENT_METADATA" to "DeliverReport"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 2 requests

Scenario: A handled error captured while offline is only sent by the original client
    When I run "MultiClientNotifyScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "MultiClientNotifyScenario"
    And I set environment variable "EVENT_METADATA" to "DeliverReport"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 1 request
    And the "Bugsnag-API-Key" header equals "abc123" for request 0
    And the payload field "apiKey" equals "abc123" for request 0

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

Scenario: An unhandled error captured while offline is detected by two clients with different endpoints
    When I run "MultiClientEndpointScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_METADATA" to "DeliverReport"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 1 request

Scenario: An unhandled error captured while online is detected by two clients with different API keys
    When I run "MultiClientOnlineScenario" with the defaults
    Then I should receive 2 requests
    And the "Bugsnag-API-Key" header equals "abc123" for request 0
    And the payload field "apiKey" equals "abc123" for request 0
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa" for request 1
    And the payload field "apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa" for request 1

Scenario: Sessions while captured offline is detected by two clients with different API keys
    When I run "MultiClientSessionScenario" with the defaults
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "MultiClientSessionScenario"
    And I set environment variable "EVENT_METADATA" to "DeliverSessions"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 2 requests

    And the "Bugsnag-API-Key" header equals "abc123" for request 1
    And the payload field "sessions.0.user.name" equals "Alice" for request 1
    And the payload field "sessions" is an array with 1 element for request 1

    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa" for request 0
    And the payload field "sessions.0.user.name" equals "Bob" for request 0
    And the payload field "sessions" is an array with 1 element for request 0
