Feature: ANRs triggered in a fixture with only bugsnag-android-core are captured

@skip_android_8_1
Scenario: Triggering ANR does not crash the minimal app
    When I run "JvmAnrMinimalFixtureScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" starts with "JvmAnrMinimalFixtureScenario"
