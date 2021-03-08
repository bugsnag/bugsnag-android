Feature: Native Breadcrumbs API

    Scenario: Leaving breadcrumbs in C followed by a Java crash
        When I run "CXXNativeBreadcrumbJavaCrashScenario" and relaunch the app
        And I configure Bugsnag for "CXXNativeBreadcrumbJavaCrashScenario"
        And I wait to receive an error
        And the error payload contains a completed handled native report
        And the exception "errorClass" equals "java.lang.ArrayIndexOutOfBoundsException"
        And the exception "message" equals "length=2; index=2"
        And the event has a "log" breadcrumb named "Lack of cheese detected"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in C followed by notifying in Java
        When I run "CXXNativeBreadcrumbJavaNotifyScenario"
        And I wait to receive an error
        And the error payload contains a completed handled native report
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Did not like"
        And the event "severity" equals "warning"
        And the event has a "process" breadcrumb named "Rerun field analysis"
        And the event "unhandled" is false
