Feature: Using custom API clients for reporting errors

Scenario: Set a custom session client and flush a stored session
    When I configure the app to run in the "offline" state
    And I run "CustomClientSessionFlushScenario"
    And I configure the app to run in the "online" state
    And I relaunch the app
    Then I should receive 2 requests
    And the "Custom-Client" header equals "Hello World" for request 0
    And the "Custom-Client" header equals "Hello World" for request 1

Scenario: Set a custom error API client and notify an error
    When I run "CustomClientErrorScenario"
    Then I should receive 1 request
    And the "Custom-Client" header equals "Hello World"
    And the request is a valid for the error reporting API

Scenario: Set a custom session API client and start a session
    When I run "CustomClientSessionScenario"
    Then I should receive 1 request
    And the "Custom-Client" header equals "Hello World"
    And the request is a valid for the session tracking API

Scenario: Set a custom error client and flush a stored error
    When I configure the app to run in the "offline" state
    And I run "CustomClientErrorFlushScenario"
    And I configure the app to run in the "online" state
    And I relaunch the app
    Then I should receive 1 request
    And the "Custom-Client" header equals "Hello World"
