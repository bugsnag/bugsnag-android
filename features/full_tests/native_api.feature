Feature: Native API

    Scenario: Adding user information in C followed by notifying in C
        When I run "CXXUserInfoScenario"
        And I wait to receive a request
        Then the request payload contains a completed handled native report
        And the exception "errorClass" equals "Connection lost"
        And the exception "message" equals "No antenna detected"
        And the event "severity" equals "info"
        And the event "user.name" equals "Jack Mill"
        And the event "user.id" equals "324523"
        And the event "user.email" is null
        And the event "unhandled" is false
        And the event "app.binaryArch" is not null
        And the payload field "events.0.device.cpuAbi" is a non-empty array

    Scenario: Adding user information in Java followed by a C crash
        And I run "CXXJavaUserInfoNativeCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXJavaUserInfoNativeCrashScenario"
        And I wait to receive a request
        Then the request payload contains a completed handled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "severity" equals "error"
        And the event "user.name" equals "Strulyegha  Ghaumon  Rabelban  Snefkal  Angengtai  Samperris  D"
        And the event "user.id" equals "9816734"
        And the event "user.email" equals "j@example.com"
        And the event "unhandled" is true

    Scenario: Notifying in C
        When I run "CXXNotifyScenario"
        And I wait to receive a request
        Then the request payload contains a completed handled native report
        And the event "severity" equals "error"
        And the exception "errorClass" equals "Vitamin C deficiency"
        And the exception "message" equals "9 out of 10 adults do not get their 5-a-day"
        And the event "unhandled" is false

    Scenario: Changing intents followed by notifying in C
        When I run "CXXAutoContextScenario"
        And I wait to receive a request
        Then the request payload contains a completed handled native report
        And the event "severity" equals "info"
        And the event "context" equals "SecondActivity"
        And the exception "errorClass" equals "Hello hello"
        And the exception "message" equals "This is a new world"
        And the event "unhandled" is false

    Scenario: Update context in Java followed by crashing in C
        When I run "CXXUpdateContextCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXUpdateContextCrashScenario"
        And I wait to receive a request
        Then the request payload contains a completed handled native report
        And the event "severity" equals "error"
        And the event "context" equals "Everest"
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "unhandled" is true

    Scenario: Leaving a breadcrumb followed by notifying in C
        When I run "CXXBreadcrumbScenario"
        And I wait to receive a request
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
        And I wait to receive a request
        Then the request payload contains a completed handled native report
        And the event has a "request" breadcrumb named "Substandard nacho error"
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Starting a session, notifying, followed by a C crash
        When I run "CXXSessionInfoCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSessionInfoCrashScenario"
        And I wait to receive 4 requests
        And I discard the oldest request
        And I discard the oldest request
        And I discard the oldest request
        Then the request payload contains a completed handled native report
        And the event contains session info
        And the payload field "events.0.session.events.unhandled" equals 1
        And the payload field "events.0.session.events.handled" equals 2

    Scenario: Leaving breadcrumbs in Java and C followed by a C crash
        When I run "CXXJavaBreadcrumbNativeBreadcrumbScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXJavaBreadcrumbNativeBreadcrumbScenario"
        And I wait to receive a request
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "severity" equals "error"
        And the event has a "log" breadcrumb named "Warm beer detected"
        And the event has a "manual" breadcrumb with the message "Reverse thrusters"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in Java and followed by notifying in C
        When I run "CXXJavaBreadcrumbNativeNotifyScenario"
        And I wait to receive a request
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
        And I wait to receive a request
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "severity" equals "error"
        And the event has a "manual" breadcrumb with the message "Bridge connector activated"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in C followed by a Java crash
        When I run "CXXNativeBreadcrumbJavaCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXNativeBreadcrumbJavaCrashScenario"
        And I wait to receive a request
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals "java.lang.ArrayIndexOutOfBoundsException"
        And the exception "message" equals "length=2; index=2"
        And the event has a "log" breadcrumb named "Lack of cheese detected"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Leaving breadcrumbs in C followed by notifying in Java
        When I run "CXXNativeBreadcrumbJavaNotifyScenario"
        And I wait to receive a request
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals "java.lang.Exception"
        And the exception "message" equals "Did not like"
        And the event "severity" equals "warning"
        And the event has a "process" breadcrumb named "Rerun field analysis"
        And the event "unhandled" is false

    Scenario: Set extraordinarily long app information
        When I run "CXXExtraordinaryLongStringScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXExtraordinaryLongStringScenario"
        And I wait to receive a request
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "app.version" equals "22.312.749.78.300.810.24.167.32"
        And the event "context" equals "ObservableSessionInitializerStringParserStringSessionProxyGloba"
        And the event "unhandled" is true

    Scenario: Add custom metadata followed by notifying in C
        When I run "CXXCustomMetadataNativeNotifyScenario"
        And I wait to receive a request
        And the request payload contains a completed handled native report
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
        And I wait to receive a request
        And the request payload contains a completed handled native report
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
        And I wait to receive a request
        And the request payload contains a completed handled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "severity" equals "error"
        And the event "metaData.fruit.apple" equals "gala"
        And the event "metaData.fruit.ripe" is true
        And the event "metaData.fruit.counters" equals 47
        And the event "unhandled" is true

    Scenario: Use the NDK methods without "env" after calling "bugsnag_start"
        When I run "CXXStartScenario"
        And I wait to receive a request
        Then the request payload contains a completed handled native report
        And the event "unhandled" is false
        And the exception "errorClass" equals "Start scenario"
        And the exception "message" equals "Testing env"
        And the event "severity" equals "info"
        And the event has a "log" breadcrumb named "Start scenario crumb"

    Scenario: Remove MetaData from the NDK layer
        When I run "CXXRemoveDataScenario"
        And I wait to receive 2 requests
        Then the request payload contains a completed handled native report
        And the event "unhandled" is false
        And the exception "errorClass" equals "RemoveDataScenario"
        And the exception "message" equals "oh no"
        And the event "metaData.persist.keep" equals "foo"
        And the event "metaData.persist.remove" equals "bar"
        And the event "metaData.remove.foo" equals "bar"
        Then I discard the oldest request
        And the request payload contains a completed handled native report
        And the event "unhandled" is false
        And the exception "errorClass" equals "RemoveDataScenario"
        And the exception "message" equals "oh no"
        And the event "metaData.persist.keep" equals "foo"
        And the event "metaData.persist.remove" is null
        And the event "metaData.remove" is null

    Scenario: Set user in Native layer followed by a Java crash
        When I run "CXXNativeUserInfoJavaCrashScenario" and relaunch the app
        And I configure Bugsnag for "CXXNativeUserInfoJavaCrashScenario"
        And I wait to receive a request
        Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the payload field "events" is an array with 1 elements
        And the event "user.id" equals "24601"
        And the event "user.email" equals "test@test.test"
        And the event "user.name" equals "test user"

    Scenario: Get Java data in the Native layer
        When I run "CXXGetJavaDataScenario" and relaunch the app
        And I configure Bugsnag for "CXXGetJavaDataScenario"
        And I wait to receive a request
        Then the request payload contains a completed unhandled native report
        And the event "unhandled" is true
        And the event "metaData.data.context" equals "passContext"
        And the event "metaData.data.appVersion" equals "passAppVersion"
        And the event "metaData.data.userName" equals "passUserName"
        And the event "metaData.data.userEmail" equals "passUserEmail"
        And the event "metaData.data.userId" equals "passUserId"
        And the event "metaData.data.metadata" equals "passMetaData"
        And the event "metaData.data.device" is not null
