Feature: In foreground field populates correctly

Scenario: Test handled exception after delay
    When I run "HandledExceptionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "app.inForeground" is true

# Skip due to an issue on later Android platforms - [PLAT-5464]
@skip_android_11 @skip_android_10
Scenario: Test handled exception in background
    When I run "InForegroundScenario"
    And I send the app to the background for 1 seconds
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "app.inForeground" is false
