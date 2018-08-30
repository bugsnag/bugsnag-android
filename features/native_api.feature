Feature: Native API

    Scenario: Adding user information in C followed by notifying in C
        When I run "CXXUserInfoScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "Oh no"
        And the exception "message" equals "The mill is down"
        And the event "severity" equals "info"
        And the event "user.name" equals "Jack Mill"
        And the event "user.id" equals "324523"
        And the event "user.email" is null

    Scenario: Adding user information in Java followed by a C crash
        When I run "CXXJavaUserInfoNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Notifying in C
        When I run "CXXNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Leaving a breadcrumb followed by notifying in C
        When I run "CXXBreadcrumbScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Leaving a breadcrumb followed by a C crash
        When I run "CXXNativeBreadcrumbNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Leaving breadcrumbs in Java and followed by notifying in C
        When I run "CXXJavaBreadcrumbNativeNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Leaving breadcrumbs in Java followed by a C crash
        When I run "CXXJavaUserInfoNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Leaving breadcrumbs in C followed by a Java crash
        When I run "CXXNativeBreadcrumbJavaCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Leaving breadcrumbs in C followed by notifying in Java
        When I run "CXXNativeBreadcrumbJavaNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Add custom metadata followed by notifying in C
        When I run "CXXCustomMetadataNativeNotifyScenario"
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API

    Scenario: Add custom metadata followed by a C crash
        When I run "CXXCustomMetadataNativeCrashScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the event "metaData.fruit.apple" equals "gala"
        And the event "metaData.fruit.counter" equals 47
