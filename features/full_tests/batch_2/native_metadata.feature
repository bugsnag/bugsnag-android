Feature: Native Metadata API

    Scenario: Add custom metadata followed by notifying in C
        When I run "CXXCustomMetadataNativeNotifyScenario"
        And I wait to receive an error
        And the error payload contains a completed handled native report
        And the exception "errorClass" equals "Twitter Overdose"
        And the exception "message" equals "Turn off the internet and go outside"
        And the event "severity" equals "info"
        And the event "metaData.fruit.orange" equals "meyer"
        And the event "metaData.fruit.ripe" is false
        And the event "metaData.fruit.counters" equals 302
        And the event "unhandled" is false

    Scenario: Add custom metadata followed by a C crash
        When I run "CXXCustomMetadataNativeCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXCustomMetadataNativeCrashScenario"
        And I wait to receive an error
        And the error payload contains a completed handled native report
        And the exception "errorClass" equals one of:
            | SIGILL |
            | SIGTRAP |
        And the event "severity" equals "error"
        And the event "metaData.Riker Ipsum.examples" equals "I'll be sure to note that in my log. You enjoyed that. They wer"
        And the event "metaData.fruit.apple" equals "gala"
        And the event "metaData.fruit.ripe" is true
        And the event "metaData.fruit.counters" equals 47
        And the event "unhandled" is true

    Scenario: Add custom metadata to configuration followed by a C crash
        When I run "CXXConfigurationMetadataNativeCrashScenario" and relaunch the app
        And I configure the app to run in the "non-metadata" state
        And I configure Bugsnag for "CXXConfigurationMetadataNativeCrashScenario"
        And I wait to receive an error
        And the error payload contains a completed handled native report
        And the exception "errorClass" equals one of:
            | SIGILL |
            | SIGTRAP |
        And the event "severity" equals "error"
        And the event "metaData.fruit.apple" equals "gala"
        And the event "metaData.fruit.ripe" is true
        And the event "metaData.fruit.counters" equals 47
        And the event "unhandled" is true

    Scenario: Remove MetaData from the NDK layer
        When I run "CXXRemoveDataScenario"
        And I wait to receive 2 errors
        Then the error payload contains a completed handled native report
        And the event "unhandled" is false
        And the exception "errorClass" equals "RemoveDataScenario"
        And the exception "message" equals "oh no"
        And the event "metaData.persist.keep" equals "foo"
        And the event "metaData.persist.remove" equals "bar"
        And the event "metaData.remove.foo" equals "bar"
        Then I discard the oldest error
        And the error payload contains a completed handled native report
        And the event "unhandled" is false
        And the exception "errorClass" equals "RemoveDataScenario"
        And the exception "message" equals "oh no"
        And the event "metaData.persist.keep" equals "foo"
        And the event "metaData.persist.remove" is null
        And the event "metaData.remove" is null

    Scenario: Get Java data in the Native layer
        When I run "CXXGetJavaDataScenario" and relaunch the app
        And I configure Bugsnag for "CXXGetJavaDataScenario"
        And I wait to receive an error
        Then the error payload contains a completed unhandled native report
        And the event "unhandled" is true
        And the event "metaData.data.context" equals "passContext"
        And the event "metaData.data.appVersion" equals "passAppVersion"
        And the event "metaData.data.userName" equals "passUserName"
        And the event "metaData.data.userEmail" equals "passUserEmail"
        And the event "metaData.data.userId" equals "passUserId"
        And the event "metaData.data.metadata" equals "passMetaData"
        And the event "metaData.data.device" is not null
