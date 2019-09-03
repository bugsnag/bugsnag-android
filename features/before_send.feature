Feature: Run callbacks before reports delivered

    Scenario: Change report before native crash report delivered
        When I run "NativeBeforeSendScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the event "context" equals "!important"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Change report before JVM crash report delivered
        When I run "BeforeSendScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the exception "errorClass" equals "java.lang.RuntimeException"
        And the exception "message" equals "Ruh-roh"
        And the event "context" equals "UNSET"
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Change report before native notify() report delivered
        When I run "NativeNotifyBeforeSendScenario"
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "Ad-hoc"
        And the exception "message" equals "Auto-generated issue"
        And the event "context" equals "hello"
        And the exception "type" equals "c"
        And the event "severity" equals "info"
        And the event "unhandled" is false

    Scenario: Change report before notify() report delivered
        When I run "NotifyBeforeSendScenario"
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Registration failure"
        And the event "context" equals "RESET"
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is false

    Scenario: Unset context
        When I run "NotifyBeforeSendUnsetContextScenario"
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Registration failure"
        And the event "context" is null
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is false
