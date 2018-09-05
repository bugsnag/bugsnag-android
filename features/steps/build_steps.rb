When("I run {string} with the defaults") do |event_type|
  step("I run #{event_type} against newnexus")
end

When(/^I run "([^"]+)"$/) do |event_type|
  step("I run \"#{event_type}\" against \"#{ENV['ANDROID_EMULATOR']}\"")
end

When(/^I run "([^"]+)" against "([^"]+)"$/) do |event_type, emulator|
  steps %Q{
    When I start emulator "#{emulator}"
    And I install the "com.bugsnag.android.mazerunner" Android app from "features/fixtures/mazerunner/build/outputs/apk/release/mazerunner-release.apk"
    And I clear the "com.bugsnag.android.mazerunner" Android app data
    And I set environment variable "BUGSNAG_API_KEY" to "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And I set environment variable "EVENT_TYPE" to "#{event_type}"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
  }
end

When("I start emulator {string}") do |emulator|
  steps %Q{
    When I set environment variable "ANDROID_EMULATOR" to "#{emulator}"
    And I run the script "features/scripts/launch_emulator.sh"
    And I run the script "await-android-emulator.sh" synchronously
  }
end

When("I relaunch the app") do
  step('I force stop the "com.bugsnag.android.mazerunner" Android app')
  step('I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity')
end

When("I configure the app to run in the {string} state") do |event_metadata|
  step("I set environment variable \"EVENT_METADATA\" to \"#{event_metadata}\"")
end
