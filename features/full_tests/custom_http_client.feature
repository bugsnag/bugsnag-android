Feature: Using custom API clients for reporting errors

  Background:
    Given I clear all persistent data

  Scenario: Set a custom HTTP client and flush a stored error + session
    When I configure the app to run in the "offline" state
    And I run "CustomHttpClientFlushScenario" and relaunch the crashed app
    And I configure Bugsnag for "CustomHttpClientFlushScenario"

    # error received
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error "Custom-Client" header equals "Hello World"

    # session received
    And I wait to receive a session
    And the session "Custom-Client" header equals "Hello World"
    And the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier

  Scenario: Set a custom HTTP client and send an error + session
    When I run "CustomHttpClientScenario"

    # error received
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error "Custom-Client" header equals "Hello World"

    # session received
    And I wait to receive a session
    And the session "Custom-Client" header equals "Hello World"
    And the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
