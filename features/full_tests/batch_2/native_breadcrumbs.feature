Feature: Native Breadcrumbs API

    Scenario: Leaving a breadcrumb followed by notifying in C
        When I run "CXXBreadcrumbScenario"
        And I wait to receive an error
        Then the request payload contains a completed handled native report
        And the event "severity" equals "info"
        And the exception "errorClass" equals "Bean temperature loss"
        And the exception "message" equals "100% more microwave required"
        And the event has a "log" breadcrumb named "Cold beans detected"
        And the event "unhandled" is false

    Scenario: Leaving a breadcrumb followed by a C crash
        When I run "CXXNativeBreadcrumbNativeCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXNativeBreadcrumbNativeCrashScenario"
        And I wait to receive an error
        Then the request payload contains a completed handled native report
        And the event has a "request" breadcrumb named "Substandard nacho error"
        And the exception "errorClass" equals one of:
            | SIGILL  |
            | SIGTRAP |
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in Java and C followed by a C crash
        When I run "CXXJavaBreadcrumbNativeBreadcrumbScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXJavaBreadcrumbNativeBreadcrumbScenario"
        And I wait to receive an error
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals one of:
            | SIGILL  |
            | SIGTRAP |
        And the event "severity" equals "error"
        And the event has a "log" breadcrumb named "Warm beer detected"
        And the event has a "manual" breadcrumb with the message "Reverse thrusters"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in Java and followed by notifying in C
        When I run "CXXJavaBreadcrumbNativeNotifyScenario"
        And I wait to receive an error
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals "Failed instantiation"
        And the exception "message" equals "Could not allocate"
        And the event "severity" equals "error"
        And the event has a "manual" breadcrumb with the message "Initiate lift"
        And the event has a "manual" breadcrumb with the message "Disable lift"
        And the event "unhandled" is false

    Scenario: Leaving breadcrumbs in Java followed by a C crash
        When I run "CXXJavaBreadcrumbNativeCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXJavaBreadcrumbNativeCrashScenario"
        And I wait to receive an error
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals one of:
            | SIGILL  |
            | SIGTRAP |
        And the event "severity" equals "error"
        And the event has a "manual" breadcrumb with the message "Bridge connector activated"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in C followed by a Java crash
        When I run "CXXNativeBreadcrumbJavaCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXNativeBreadcrumbJavaCrashScenario"
        And I wait to receive an error
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals "java.lang.ArrayIndexOutOfBoundsException"
        And the exception "message" equals "length=2; index=2"
        And the event has a "log" breadcrumb named "Lack of cheese detected"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in C followed by notifying in Java
        When I run "CXXNativeBreadcrumbJavaNotifyScenario"
        And I wait to receive an error
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Did not like"
        And the event "severity" equals "warning"
        And the event has a "process" breadcrumb named "Rerun field analysis"
        And the event "unhandled" is false
