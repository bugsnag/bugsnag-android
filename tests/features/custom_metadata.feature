Feature: The NDK does not crash when custom metadata is set

Scenario: Set custom metadata and send an observable event to the NDK
    When I run "CustomMetaDataScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
