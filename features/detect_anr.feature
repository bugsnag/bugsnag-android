Feature: Reporting App Not Responding events

@skip_android_8_1
Scenario: Forever loop on the main thread with pending touch events when autoDetectAnrs = true
    When I run "AppNotRespondingScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the exception "type" equals "android"

@skip_android_8_1
Scenario: Forever loop in native code on the main thread with pending touch events when autoDetectAnrs = true
    When I run "CXXAnrScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the exception "type" equals "c"

@skip_android_8_1
Scenario: Forever loop on the main thread with pending touch events when autoDetectAnrs = true and autoDetectNdkCrashes = false
    When I run "AppNotRespondingDisabledNdkScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the exception "type" equals "android"

@skip_android_8_1
Scenario: Forever loop in native code on the main thread with pending touch events when autoDetectAnrs = true and autoDetectNdkCrashes = false
    When I run "CXXAnr_NdkDisabledScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the exception "type" equals "android"

@skip_android_8_1
Scenario: Forever loop on the main thread with pending touch events
    When I run "AppNotRespondingDisabledScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "AppNotRespondingDisabledScenario"
    And the payload field "events.0.device.cpuAbi" is a non-empty array

@skip_android_8_1
Scenario: Forever loop in native code on the main thread with pending touch events with ANR disabled
    When I run "CXXAnr_AnrDisabledScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "AppNotRespondingDisabledScenario"
    And the payload field "events.0.device.cpuAbi" is a non-empty array

@skip_android_8_1
Scenario: Forever loop on the main thread with pending touch events after the release stage settings change to disable reporting
    When I run "AppNotRespondingOutsideReleaseStagesScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I should receive no requests
