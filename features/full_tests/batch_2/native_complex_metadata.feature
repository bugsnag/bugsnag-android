Feature: Bugsnag can deal with complicated metadata values

    Scenario: CXX error handles large breadcrumbs/metadata
        When I run "CXXComplexMetadataScenario" and relaunch the app
        When I run "CXXComplexMetadataScenario" and relaunch the app
        And I wait to receive 2 errors

        Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "errorClass" equals "SIGABRT"
        And the exception "message" equals "Abort program"
        And the event contains complex metadata

        Then I discard the oldest error
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the error payload field "events" is an array with 1 elements
        And the exception "errorClass" equals "java.lang.RuntimeException"
        And the exception "message" equals "CXXComplexMetadataScenario"
        And the event contains complex metadata
