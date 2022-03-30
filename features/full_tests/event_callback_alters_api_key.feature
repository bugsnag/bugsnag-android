Feature: When the api key is altered in an Event the JSON payload reflects this

  Background:
    Given I clear all persistent data

  Scenario: Handled exception with altered API key
    When I run "HandledExceptionApiKeyChangeScenario"
    Then I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the exception "message" equals "HandledExceptionApiKeyChangeScenario"
    And the error payload field "apiKey" equals "0000111122223333aaaabbbbcccc9999"
    And the error "Bugsnag-Api-Key" header equals "0000111122223333aaaabbbbcccc9999"

  Scenario: Unhandled exception with altered API key
    When I run "UnhandledExceptionApiKeyChangeScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledExceptionApiKeyChangeScenario"
    And I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the exception "message" equals "UnhandledExceptionApiKeyChangeScenario"
    And the error payload field "apiKey" equals "0000111122223333aaaabbbbcccc9999"
    And the error "Bugsnag-Api-Key" header equals "0000111122223333aaaabbbbcccc9999"
