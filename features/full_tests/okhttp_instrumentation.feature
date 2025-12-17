Feature: Capturing network breadcrumbs

  Background:
    Given I clear all persistent data

  Scenario: Breadcrumbs are captured for OkHttp network requests (Legacy)
    When I run "NetworkBreadcrumbScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "NetworkBreadcrumbScenario"

    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.timestamp" is a timestamp
    And the event "breadcrumbs.0.name" equals "OkHttp call succeeded"
    And the event "breadcrumbs.0.type" equals "request"
    And the event "breadcrumbs.0.metaData.method" equals "GET"
    And the event "breadcrumbs.0.metaData.url" equals "https://google.com/"
    And the error payload field "events.0.breadcrumbs.0.metaData.duration" is a number
    And the error payload field "events.0.exceptions.0.stacktrace.0.type" is null
    And the event "breadcrumbs.0.metaData.urlParams.test" equals "true"
    And the error payload field "events.0.breadcrumbs.0.metaData.requestContentLength" is a number
    And the error payload field "events.0.breadcrumbs.0.metaData.responseContentLength" is a number
    And the error payload field "events.0.breadcrumbs.0.metaData.status" is a number

  Scenario: Failed POST requests send error reports when configured
    When I configure the app to run in the "POST 400" state
    And I run "OkHttpInstrumentationScenario"
    And I wait to receive a reflection
    Then I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "HTTPError"
    And the exception "message" matches "400: http://.+"
    And the event "context" matches "POST .+"

    And the reflection payload field "url" is stored as the value "expectedUrl"

    # Validate request fields
    And the event "request.httpMethod" equals "POST"
    And the event "request.httpVersion" is not null
    And the event "request.bodyLength" is greater than 64
    And the error payload field "events.0.request.body" equals "{\"padding\":\"this is a string, an"
    And the error payload field "events.0.request.url" equals the stored value "expectedUrl"
    And the error payload field "events.0.request.headers.Authorization" equals "[REDACTED]"
    And the error payload field "events.0.request.params.password" equals "[REDACTED]"

    # Validate response fields
    And the event "response.statusCode" equals 400
    And the event "response.bodyLength" is greater than 1
    And the event "response.body" is not null

  Scenario: Failed GET requests send error reports when configured
    When I configure the app to run in the "GET 500" state
    And I run "OkHttpInstrumentationScenario"
    And I wait to receive a reflection
    Then I wait to receive an error
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "HTTPError"
    And the exception "message" matches "500: http://.+"
    And the event "context" matches "GET .+"

    # Validate request fields
    And the event "request.httpMethod" equals "GET"
    And the event "request.httpVersion" is not null
    And the event "request.url" matches "^https?\:\/\/.+"
    And the error payload field "events.0.request.headers.Authorization" equals "[REDACTED]"
    And the error payload field "events.0.request.params.password" equals "[REDACTED]"

    # Validate response fields
    And the event "response.statusCode" equals 500
    And the event "response.bodyLength" is greater than 1
    And the event "response.body" is not null

  Scenario: Successful requests do not emit errors
    When I configure the app to run in the "POST 200" state
    And I run "OkHttpInstrumentationScenario"
    Then I wait to receive a reflection
    And I should receive no errors