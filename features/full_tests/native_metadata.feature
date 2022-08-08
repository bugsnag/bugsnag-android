Feature: Native Metadata API

  Background:
    Given I clear all persistent data

  Scenario: Add custom metadata to configuration followed by a C crash
    When I run "CXXConfigurationMetadataNativeCrashScenario" and relaunch the crashed app
    And I configure the app to run in the "non-metadata" state
    And I configure Bugsnag for "CXXConfigurationMetadataNativeCrashScenario"
    And I wait to receive an error
    And the error payload contains a completed handled native report
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And the event "severity" equals "error"
    And the event "metaData.fruit.apple" equals "gala"
    And the event "metaData.fruit.ripe" is true
    And the event "metaData.fruit.counters" equals 47
    And the event "metaData.complex.message" equals "That might've been one of the shortest assignments in the history of Starfleet. The Enterprise computer system is controlled by three primary main processor cores, cross-linked with a redundant melacortz ramistat, fourteen kiloquad interface modules."
    And the event "metaData.complex.maps.location" equals "you are here"
    And the event "metaData.complex.maps.inventory.0" equals "lots of string"
    And the error payload field "events.0.metaData.complex.maps.inventory.1" is a number
    And the error payload field "events.0.metaData.complex.maps.inventory.2" is true
    And the event "metaData.complex.list.0" equals "summer"
    And the event "metaData.complex.list.1" equals "winter"
    And the event "metaData.complex.list.2" equals "spring"
    And the event "metaData.complex.list.3" equals "autumn"
    And the event "metaData.complex.set.0" equals "value1"
    And the event "metaData.complex.set.1" equals "2value"
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
    When I run "CXXGetJavaDataScenario" and relaunch the crashed app
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
        # TODO: Skipped pending PLAT-6176
        # And the event "metaData.data.password" equals "[REDACTED]"
