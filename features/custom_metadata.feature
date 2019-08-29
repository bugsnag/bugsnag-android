Feature: The NDK does not crash when custom metadata is set

Scenario: Set a custom error API client and notify an error
    When I run "CustomMetaDataScenario"
    Then I should receive 1 request
    And the request is a valid for the error reporting API
