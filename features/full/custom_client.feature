Feature: Using custom API clients for reporting errors

Scenario: Set a custom session client and flush a stored session
    When I configure the app to run in the "offline" state
    And I run "CustomClientSessionFlushScenario" and relaunch the app
    And I configure the app to run in the "online" state
    And I run "CustomClientSessionFlushScenario"
    And I wait to receive 2 requests
    Then the "Custom-Client" header equals "Hello World"

Scenario: Set a custom error API client and notify an error
    When I run "CustomClientErrorScenario"
    Then I wait to receive a request
    And the "Custom-Client" header equals "Hello World"
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

Scenario: Set a custom session API client and start a session
    When I run "CustomClientSessionScenario"
    Then I wait to receive a request
    And the "Custom-Client" header equals "Hello World"
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier

Scenario: Set a custom error client and flush a stored error
    When I configure the app to run in the "offline" state
    And I run "CustomClientErrorFlushScenario" and relaunch the app
    And I configure the app to run in the "online" state
    And I run "CustomClientErrorFlushScenario"
    Then I wait to receive a request
    And the "Custom-Client" header equals "Hello World"
