Feature: Initializing nultiple clients sends payloads with correct API key to each

Scenario: Unhandled error reported by two clients with different API keys
    When I run "MultiClientApiKeyScenario" with the defaults
    Then I should receive no requests

    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "MultiClientScenario"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 2 requests

    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa" for request 0
    And the payload field "apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa" for request 0
    And the "Bugsnag-API-Key" header equals "abc123" for request 1
    And the payload field "apiKey" equals "abc123" for request 1

