Feature: In foreground field populates correctly

# Skip due to an issue on later Android platforms - [PLAT-5464]
@skip_android_11 @skip_android_10
Scenario: Test handled exception in background
    When I run "InForegroundScenario"
    And I send the app to the background for 1 seconds
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "app.inForeground" is false
    # Appium 1.9.1 - 1.20.2 changes the orientation to landscape when foregrounding.
    # Return it to portrait to avoid impacting other scenarios.
    And I set the screen orientation to portrait
