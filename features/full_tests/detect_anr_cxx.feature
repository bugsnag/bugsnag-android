Feature: ANRs triggered in CXX code are captured

@skip_android_8_1
Scenario: ANR triggered in CXX code is captured
    When I run "CXXAnrScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the "Bugsnag-Stacktrace-Types" header equals "android,c"
    And the payload field "events.0.exceptions.0.type" equals "android"
    And the payload field "events.0.threads.0.type" equals "android"
    And the payload field "events.0.exceptions.0.stacktrace.0.type" equals "c"
    And the payload field "events.0.exceptions.0.stacktrace.1.type" equals "c"
    And the payload field "events.0.exceptions.0.stacktrace.19.type" is null
    And the payload field "events.0.threads.0.stacktrace.0.type" equals "c"
    And the payload field "events.0.threads.0.stacktrace.1.type" equals "c"
    And the payload field "events.0.threads.0.stacktrace.19.type" is null
    And the exception stacktrace matches the thread stacktrace

@skip_android_8_1
Scenario: ANR triggered in CXX code is captured even when NDK detection is disabled
    When I run "CXXAnrNdkDisabledScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the "Bugsnag-Stacktrace-Types" header equals "android"
    And the payload field "events.0.exceptions.0.type" equals "android"
    And the payload field "events.0.exceptions.0.stacktrace.0.type" is null
    And the payload field "events.0.threads.0.type" equals "android"
    And the payload field "events.0.threads.0.stacktrace.0.type" is null
