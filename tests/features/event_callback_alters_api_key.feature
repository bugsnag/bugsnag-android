Feature: When the api key is altered in an Event the JSON payload reflects this

Scenario: Handled exception with altered API key
    When I run "HandledExceptionApiKeyChangeScenario"
    Then I wait to receive a request
    And the payload field "events" is an array with 1 elements
    And the exception "message" equals "HandledExceptionApiKeyChangeScenario"
    And the payload field "apiKey" equals "0000111122223333aaaabbbbcccc9999"
    And the "Bugsnag-Api-Key" header equals "0000111122223333aaaabbbbcccc9999"

Scenario: Unhandled exception with altered API key
    When I run "UnhandledExceptionApiKeyChangeScenario" and relaunch the app
    And I configure Bugsnag for "UnhandledExceptionApiKeyChangeScenario"
    And I wait to receive a request
    And the payload field "events" is an array with 1 elements
    And the exception "message" equals "UnhandledExceptionApiKeyChangeScenario"
    And the payload field "apiKey" equals "0000111122223333aaaabbbbcccc9999"
    And the "Bugsnag-Api-Key" header equals "0000111122223333aaaabbbbcccc9999"
