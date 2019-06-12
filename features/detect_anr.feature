Feature: Reporting App Not Responding events

# Figure these later

#Scenario: Sleeping the main thread with pending touch events when detectAnrs = true
#    When I run "AppNotRespondingScenario"
#    And I click the element "scenarioText"
#    Then I wait to receive a request
#    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
#    And the exception "errorClass" equals "ANR"
#    And the exception "message" equals "Application did not respond to UI input"

#Scenario: Sleeping the main thread with pending touch events when detectAnrs = true and detectNdkCrashes = false
#    When I run "AppNotRespondingDisabledNdkScenario"
#    And I click the element "scenarioText"
#    Then I wait to receive a request
#    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
#    And the exception "errorClass" equals "ANR"
#    And the exception "message" equals "Application did not respond to UI input"

#Scenario: Sleeping the main thread with pending touch events
#    When I run "AppNotRespondingDisabledScenario"
#    And I click the element "scenarioText"
#    And I wait for 2 seconds
#    Then I should receive no requests
