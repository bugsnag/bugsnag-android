Feature: Bugsnag can deal with a large amount of breadcrumbs/metadata

    Scenario: JVM error handles large breadcrumbs/metadata
        When I run "JvmLargePayloadScenario"
        And I wait to receive an error
        Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the error payload field "events" is an array with 1 elements
        And the exception "errorClass" equals "java.lang.RuntimeException"
        And the exception "message" equals "JvmLargePayloadScenario"

        # Breadcrumbs
        And the event "breadcrumbs.0.name" equals "Breadcrumb 950"
        And the event "breadcrumbs.49.name" equals "Breadcrumb 999"
        And the event "breadcrumbs.50" is null

        # MetaData
        And the event "metaData.test.key_0" equals "0"
        And the event "metaData.test.key_100" equals "100"
        And the event "metaData.test.key_500" equals "500"
        And the event "metaData.test.key_999" equals "999"
        And the event "metaData.test.key_1000" is null

    Scenario: CXX error handles large breadcrumbs/metadata
        When I run "CXXLargePayloadScenario" and relaunch the app
        And I configure Bugsnag for "CXXLargePayloadScenario"
        And I wait to receive an error
        Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

        # Breadcrumbs
        And the event "breadcrumbs.0.name" equals "Breadcrumb 250"
        And the event "breadcrumbs.49.name" equals "Breadcrumb 299"
        And the event "breadcrumbs.50" is null

        # MetaData
        And the event "metaData.test.key_0" equals "0"
        And the event "metaData.test.key_100" equals "100"
        And the event "metaData.test.key_200" equals "200"
        And the event "metaData.test.key_299" equals "299"
        And the event "metaData.test.key_300" is null
