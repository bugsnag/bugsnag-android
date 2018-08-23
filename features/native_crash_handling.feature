Feature: Native crash reporting

    Scenario: Read write-only page
        When I run "CXXReadWriteOnlyPageScenario" with the defaults
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGBUS"
