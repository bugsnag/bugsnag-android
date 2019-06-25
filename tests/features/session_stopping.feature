Feature: Stopping and resuming sessions

Scenario: When a session is stopped the error has no session information
    When I run "StoppedSessionScenario"
    Then I wait to receive 3 requests
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session" is not null
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session" is null

Scenario: When a session is resumed the error uses the previous session information
    When I run "ResumedSessionScenario"
    Then I wait to receive 3 requests
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.events.handled" equals 1
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.events.handled" equals 2
    # Needs a step to ensure session Ids between all three match

# TODO: Uncomment once PLAT-3279 is done
#Scenario: When a new session is started the error uses different session information
#    When I run "NewSessionScenario"
#    Then I wait to receive 4 requests
#    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
#    And I discard the oldest request
#    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
#    And I discard the oldest request
#    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
#    And I discard the oldest request
#    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    # Needs a step to ensure session Ids between errors don't match
