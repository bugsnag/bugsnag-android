Feature: Flushing errors does not send duplicates

# Issues!

Scenario: Only 1 request sent if connectivity change occurs before launch
    When I run "AsyncErrorConnectivityScenario"
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

Scenario: Only 1 request sent if multiple connectivity changes occur
    When I run "AsyncErrorDoubleFlushScenario"
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

Scenario: Only 1 request sent if connectivity change occurs after launch
    When I run "AsyncErrorLaunchScenario"
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
