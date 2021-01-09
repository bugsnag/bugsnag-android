# If adding scenarios to this feature, leave them in their original location, commented out.
Feature: Known flaky scenarios.

  Scenario: When a new session is started the error uses different session information
    When I run "NewSessionScenario"
    Then I wait to receive 4 requests
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions.0.id" is stored as the value "first_new_session_id"
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.id" equals the stored value "first_new_session_id"
    And I discard the oldest request
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions.0.id" is stored as the value "second_new_session_id"
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.id" equals the stored value "second_new_session_id"
    And the payload field "events.0.session.id" does not equal the stored value "first_new_session_id"

  Scenario: Test Bugsnag initializes correctly
    When I run "BugsnagInitScenario"
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "metaData.client.count" equals 1

  Scenario: All user fields set
    When I run "UserEnabledScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "UserEnabledScenario"
    And the event "user.id" equals "123"
    And the event "user.email" equals "user@example.com"
    And the event "user.name" equals "Joe Bloggs"

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

  # Skip due to an issue on later Android platforms - [PLAT-5464]
  @skip_android_11 @skip_android_10
  Scenario: Test handled exception in background
    When I run "InForegroundScenario"
    And I send the app to the background for 1 seconds
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "app.inForeground" is false

  Scenario: Load configuration initialised from the Manifest
    When I run "LoadConfigurationFromManifestScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier with the apiKey "abc12312312312312312312312312312"
    And the exception "message" equals "LoadConfigurationFromManifestScenario"
    And the event "app.releaseStage" equals "testing"
    And the payload field "events.0.breadcrumbs" is an array with 1 elements
    And the event "metaData.test.foo" equals "bar"
    And the event "metaData.test.filter_me" equals "[REDACTED]"
    And the event "app.versionCode" equals 753
    And the event "app.buildUUID" equals "test-7.5.3"
    And the event "app.version" equals "7.5.3"
    And the event "app.type" equals "test"
    And the payload field "events.0.threads" is a non-empty array

  Scenario: Test handled JVM error
    When I run "NaughtyStringScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "NaughtyStringScenario"
    And the payload field "events.0.metaData.custom" is not null
    And the payload field "events.0.metaData.custom.val_1" equals "ЁЂЃЄЅІЇЈЉЊЋЌЍЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюя"
    And the payload field "events.0.metaData.custom.val_2" equals "田中さんにあげて下さい"
    And the payload field "events.0.metaData.custom.val_3" equals "𐐜 𐐔𐐇𐐝𐐀𐐡𐐇𐐓 𐐙𐐊𐐡𐐝𐐓/𐐝𐐇𐐗𐐊𐐤𐐔 𐐒𐐋𐐗 𐐒𐐌 𐐜 𐐡𐐀𐐖𐐇𐐤𐐓𐐝 𐐱𐑂 𐑄 𐐔𐐇𐐝𐐀𐐡𐐇𐐓 𐐏𐐆𐐅𐐤𐐆𐐚𐐊𐐡𐐝𐐆𐐓𐐆"
    And the payload field "events.0.metaData.custom.val_4" equals "表ポあA鷗ŒéＢ逍Üßªąñ丂㐀𠀀"
    And the payload field "events.0.metaData.custom.val_5" equals ",。・:*:・゜’( ☻ ω ☻ )。・:*:・゜’"
    And the payload field "events.0.metaData.custom.val_6" equals "❤️ 💔 💌 💕 💞 💓 💗 💖 💘 💝 💟 💜 💛 💚 💙"
    And the payload field "events.0.metaData.custom.val_7" equals "✋🏿 💪🏿 👐🏿 🙌🏿 👏🏿 🙏🏿"
    And the payload field "events.0.metaData.custom.val_8" equals "👨‍👩‍👦 👨‍👩‍👧‍👦 👨‍👨‍👦 👩‍👩‍👧 👨‍👦 👨‍👧‍👦 👩‍👦 👩‍👧‍👦"
    And the payload field "events.0.metaData.custom.val_9" equals "🚾 🆒 🆓 🆕 🆖 🆗 🆙 🏧"
    And the payload field "events.0.metaData.custom.val_10" equals "１２３"
    And the payload field "events.0.metaData.custom.val_11" equals "الكل في المجمو عة (5)"
    And the payload field "events.0.metaData.custom.val_12" equals "˙ɐnbᴉlɐ ɐuƃɐɯ ǝɹolop ʇǝ ǝɹoqɐl ʇn ʇunpᴉpᴉɔuᴉ ɹodɯǝʇ poɯsnᴉǝ"
    And the payload field "events.0.metaData.custom.val_13" equals "𝓣𝓱𝓮 𝓺𝓾𝓲𝓬𝓴 𝓫𝓻𝓸𝔀𝓷 𝓯𝓸𝔁 𝓳𝓾𝓶𝓹𝓼 𝓸𝓿𝓮𝓻 𝓽𝓱𝓮 𝓵𝓪𝔃𝔂 𝓭𝓸𝓰"
    And the payload field "events.0.metaData.custom.val_14" equals "گچپژ"

  Scenario: Only 1 request sent if connectivity change occurs after launch
    When I run "AsyncErrorLaunchScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "context" equals "AsyncErrorLaunchScenario"

  Scenario: Only 1 request sent if multiple connectivity changes occur
    When I run "AsyncErrorDoubleFlushScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "context" equals "AsyncErrorDoubleFlushScenario"
