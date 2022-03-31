Feature: Flushing errors does not send duplicates

# Flaky tests as they race with Bugsnag sending errors during initialization.
# Disabled until PLAT-5869 can be actioned to convert these to unit tests.

#Scenario: Only 1 request sent if connectivity change occurs before launch
#    When I run "AsyncErrorConnectivityScenario" and relaunch the crashed app
#    And I configure Bugsnag for "AsyncErrorConnectivityScenario"
#    Then I wait to receive an error
#    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
#    And the event "context" equals "AsyncErrorConnectivityScenario"
#
#Scenario: Only 1 request sent if multiple connectivity changes occur
#    When I run "AsyncErrorDoubleFlushScenario"
#    Then I wait to receive an error
#    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
#    And the event "context" equals "AsyncErrorDoubleFlushScenario"
#
#Scenario: Only 1 request sent if connectivity change occurs after launch
#    When I run "AsyncErrorLaunchScenario"
#    Then I wait to receive an error
#    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
#    And the event "context" equals "AsyncErrorLaunchScenario"
