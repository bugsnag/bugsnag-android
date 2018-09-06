Feature: Native API

    Scenario: Adding user information in C followed by notifying in C
        When I run "CXXUserInfoScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "Connection lost"
        And the exception "message" equals "No antenna detected"
        And the event "severity" equals "info"
        And the event "user.name" equals "Jack Mill"
        And the event "user.id" equals "324523"
        And the event "user.email" is null
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Adding user information in Java followed by a C crash
        When I run "CXXJavaUserInfoNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGILL"
        And the event "severity" equals "error"
        And the event "user.name" equals "J"
        And the event "user.id" equals "9816734"
        And the event "user.email" equals "j@example.com"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Notifying in C
        When I run "CXXNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the event "severity" equals "error"
        And the exception "errorClass" equals "Vitamin C deficiency"
        And the exception "message" equals "9 out of 10 adults do not get their 5-a-day"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Leaving a breadcrumb followed by notifying in C
        When I run "CXXBreadcrumbScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the event "severity" equals "info"
        And the exception "errorClass" equals "Bean temperature loss"
        And the exception "message" equals "100% more microwave required"
        And the event has a "log" breadcrumb named "Cold beans detected"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Leaving a breadcrumb followed by a C crash
        When I run "CXXNativeBreadcrumbNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the event has a "request" breadcrumb named "Substandard nacho error"
        And the exception "errorClass" equals "SIGILL"
        And the event "severity" equals "error"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Leaving breadcrumbs in Java and C followed by a C crash
        When I run "CXXJavaBreadcrumbNativeBreadcrumbScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGILL"
        And the event "severity" equals "error"
        And the event has a "log" breadcrumb named "Warm beer detected"
        And the event has a "manual" breadcrumb with message "Reverse thrusters"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Leaving breadcrumbs in Java and followed by notifying in C
        When I run "CXXJavaBreadcrumbNativeNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "Failed instantiation"
        And the exception "message" equals "Could not allocate"
        And the event "severity" equals "error"
        And the event has a "manual" breadcrumb with message "Initiate lift"
        And the event has a "manual" breadcrumb with message "Disable lift"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Leaving breadcrumbs in Java followed by a C crash
        When I run "CXXJavaUserInfoNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGILL"
        And the event "severity" equals "error"
        And the event has a "manual" breadcrumb with message "Bridge connector activated"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Leaving breadcrumbs in C followed by a Java crash
        When I run "CXXNativeBreadcrumbJavaCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "java.lang.ArrayIndexOutOfBoundsException"
        And the exception "message" equals "length=2; index=2"
        And the event has a "log" breadcrumb named "Lack of cheese detected"
        And the event "severity" equals "error"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Leaving breadcrumbs in C followed by notifying in Java
        When I run "CXXNativeBreadcrumbJavaNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Did not like"
        And the event "severity" equals "warning"
        And the event has a "process" breadcrumb named "Rerun field analysis"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Add custom metadata followed by notifying in C
        When I run "CXXCustomMetadataNativeNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "Twitter Overdose"
        And the exception "message" equals "Turn off the internet and go outside"
        And the event "severity" equals "info"
        And the event "metaData.fruit.orange" equals "meyer"
        And the event "metaData.fruit.ripe" is false
        And the event "metaData.fruit.counters" equals 302
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    Scenario: Add custom metadata followed by a C crash
        When I run "CXXCustomMetadataNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGILL"
        And the event "severity" equals "error"
        And the event "metaData.fruit.apple" equals "gala"
        And the event "metaData.fruit.ripe" is true
        And the event "metaData.fruit.counters" equals 47
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
