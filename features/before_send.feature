Feature: Run callbacks before reports delivered

# Issues!

    Scenario: Change report before native crash report delivered
        When I run "NativeBeforeSendScenario"
        And I relaunch the app
        And I wait to receive a request
        Then the request payload contains a completed native report
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the event "context" equals "!important"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Change report before JVM crash report delivered
        When I run "BeforeSendScenario"
        And I wait to receive a request
        Then the request payload contains a completed native report
        And the exception "errorClass" equals "java.lang.RuntimeException"
        And the exception "message" equals "Ruh-roh"
        And the event "context" equals "UNSET"
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Change report before native notify() report delivered
        When I run "NativeNotifyBeforeSendScenario"
        And I wait to receive a request
        Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "errorClass" equals "Ad-hoc"
        And the exception "message" equals "Auto-generated issue"
        And the event "context" equals "hello"
        And the exception "type" equals "c"
        And the event "severity" equals "info"
        And the event "unhandled" is false

    Scenario: Change report before notify() report delivered
        When I run "NotifyBeforeSendScenario"
        And I wait to receive a request
        Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Registration failure"
        And the event "context" equals "RESET"
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is false

    Scenario: Unset context
        When I run "NotifyBeforeSendUnsetContextScenario"
        And I wait to receive a request
        Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Registration failure"
        And the event "context" is null
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is false
