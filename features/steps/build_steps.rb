When("I run {string} with the defaults") do |event_type|
  step("I run #{event_type} against newnexus")
end

When(/^I run "([^"]+)"$/) do |event_type|
  step("I run \"#{event_type}\" against \"#{ENV['ANDROID_EMULATOR']}\"")
end

When(/^I run "([^"]+)" against "([^"]+)"$/) do |event_type, emulator|
  wait_time = RUNNING_CI ? '10' : '5'
  steps %Q{
    When I start Android emulator "#{emulator}"
    And I install the "com.bugsnag.android.mazerunner" Android app from "features/fixtures/mazerunner/build/outputs/apk/release/mazerunner-release.apk"
    And I clear the "com.bugsnag.android.mazerunner" Android app data
    And I set environment variable "BUGSNAG_API_KEY" to "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And I set environment variable "EVENT_TYPE" to "#{event_type}"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    And I wait for #{wait_time} seconds
  }
end

When("I relaunch the app") do
  wait_time = RUNNING_CI ? '20' : '9'
  steps %Q{
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    And I wait for #{wait_time} seconds
  }
end

When("I configure the app to run in the {string} state") do |event_metadata|
  step("I set environment variable \"EVENT_METADATA\" to \"#{event_metadata}\"")
end
