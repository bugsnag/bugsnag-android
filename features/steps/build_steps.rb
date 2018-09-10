When("I wait a bit") do
  wait_time = RUNNING_CI ? '30' : '9'
  step("I wait for #{wait_time} seconds")
end

When(/^I run "([^"]+)"$/) do |event_type|
  step("I run \"#{event_type}\" against \"#{ENV['ANDROID_EMULATOR']}\"")
end

When(/^I run "([^"]+)" against "([^"]+)"$/) do |event_type, emulator|
  assert(emulator && emulator.length > 0, "ANDROID_EMULATOR variable is not set")
  assert(ENV['ANDROID_HOME'] && ENV['ANDROID_HOME'].length > 0, "ANDROID_HOME variable is not set")
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
  wait_time = RUNNING_CI ? '30' : '9'
  steps %Q{
    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    And I wait for #{wait_time} seconds
  }
end

When("I configure the app to run in the {string} state") do |event_metadata|
  step("I set environment variable \"EVENT_METADATA\" to \"#{event_metadata}\"")
end

When("I press the home button") do
  steps %Q{
    And I run the script "features/scripts/show-home-screen.sh" synchronously
  }
end

When("I bring the app to the foreground") do
  step 'I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity'
end

When("I rotate the device to {string}") do |orientation|
  steps %Q{
    When I set environment variable "DEVICE_ORIENTATION" to "#{orientation}"
    And I run the script "features/scripts/rotate-device.sh" synchronously
  }
end
Then("the exception reflects a signal was raised") do
  value = read_key_path(find_request(0)[:body], "events.0.exceptions.0")
  error_class = value["errorClass"]
  assert_block("The errorClass was not from a signal: #{error_class}") do
    ["SIGFPE","SIGILL","SIGSEGV","SIGABRT","SIGTRAP","SIGBUS"].include? error_class
  end
end
Then("the event {string} string is empty") do |keypath|
  value = read_key_path(find_request(0)[:body], keypath)
  assert_block("The #{keypath} is not empty: '#{value}'") do
    value.nil? or value.length == 0
  end
end
