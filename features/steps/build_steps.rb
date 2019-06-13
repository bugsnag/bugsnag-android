wait_time = ENV['MAZE_WAIT_TIME'] || (RUNNING_CI ? '20' : '5')
When("I wait a bit") do
  step("I wait for #{wait_time} seconds")
end

When(/^I run "([^"]+)"( and press the home button)?$/) do |event_type, pressed_home|
  step("I run \"#{event_type}\" against \"#{ENV['ANDROID_EMULATOR']}\"")
  step("I press the home button") if pressed_home
  step("I wait for #{wait_time} seconds")
end

When(/^I run "([^"]+)" in "([^"]+)" orientation$/) do |event_type, orientation|
  step("I rotate the device to \"#{orientation}\"")
  step("I run \"#{event_type}\"")
end

When(/^I run "([^"]+)" against "([^"]+)"$/) do |event_type, emulator|
  assert(emulator && emulator.length > 0, "ANDROID_EMULATOR variable is not set")
  assert(ENV['ANDROID_HOME'] && ENV['ANDROID_HOME'].length > 0, "ANDROID_HOME variable is not set")
  step("I start Android emulator \"#{emulator}\"") unless RUNNING_CI # emulator is prebooted on CI
  steps %Q{
    When I install the "com.bugsnag.android.mazerunner" Android app from "features/fixtures/mazerunner/build/outputs/apk/release/mazerunner-release.apk"
    And I clear the "com.bugsnag.android.mazerunner" Android app data
    And I set environment variable "BUGSNAG_API_KEY" to "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And I set environment variable "EVENT_TYPE" to "#{event_type}"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
  }
end

When("I relaunch the app") do
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
When("I tap the screen") do
  step('I run the script "features/scripts/tap-screen.sh" synchronously')
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

Then("the event {string} is greater than {int}") do |keypath, int|
  value = read_key_path(find_request(0)[:body], "events.0.#{keypath}")
  assert_false(value.nil?, "The event #{keypath} is nil")
  assert_true(value > int)
end

Then("the exception {string} equals one of:") do |keypath, possible_values|
  value = read_key_path(find_request(0)[:body], "events.0.exceptions.0.#{keypath}")
  assert_includes(possible_values.raw.flatten, value)
end
Then("the first significant stack frame methods and files should match:") do |expected_values|
  stacktrace = read_key_path(find_request(0)[:body], "events.0.exceptions.0.stacktrace")
  expected_frame_values = expected_values.raw
  expected_index = 0
  stacktrace.each_with_index do |item, index|
    next if expected_index >= expected_frame_values.length
    expected_frame = expected_frame_values[expected_index]
    method = `c++filt -_ _#{item["method"]}`.chomp
    method = item["method"] if method == "_#{item["method"]}"
    next if method.start_with? "bsg_" or
            method.start_with? "std::" or
            method.start_with? "__cxx" or
            item["file"].start_with? "/system/" or
            item["file"].end_with? "libbugsnag-ndk.so"

    assert_equal(expected_frame[0], method)
    assert(item["file"].end_with?(expected_frame[1]), "'#{item["file"]}' in frame #{index} does not end with '#{expected_frame[1]}'")
    expected_index += 1
  end
end

Then("the report in request {int} contains the required fields") do |index|
  steps %Q{
    Then the "Bugsnag-API-Key" header is not null for request #{index}
    And the "Content-Type" header equals "application/json" for request #{index}
    And the "Bugsnag-Payload-Version" header for request #{index} equals one of:
      | 4   |
      | 4.0 |
    And the "Bugsnag-Sent-At" header is a timestamp for request #{index}

    And the payload field "notifier.name" is not null for request #{index}
    And the payload field "notifier.url" is not null for request #{index}
    And the payload field "notifier.version" is not null for request #{index}
    And the payload field "events" is a non-empty array for request #{index}

    Then the payload field "events.0.unhandled" is not null for request #{index}
    And the payload field "events.0.app.duration" is not null for request #{index}
    And the payload field "events.0.app.durationInForeground" is not null for request #{index}
    And the payload field "events.0.app.id" is not null for request #{index}
    And the payload field "events.0.app.inForeground" is not null for request #{index}
    And the payload field "events.0.app.releaseStage" is not null for request #{index}
    And the payload field "events.0.app.type" equals "android" for request #{index}
    And the payload field "events.0.app.version" is not null for request #{index}
    And the payload field "events.0.app.versionCode" equals 34 for request #{index}
    And the payload field "events.0.device.id" is not null for request #{index}
    And the payload field "events.0.device.manufacturer" is not null for request #{index}
    And the payload field "events.0.device.model" is not null for request #{index}
    And the payload field "events.0.device.orientation" is not null for request #{index}
    And the payload field "events.0.device.osName" equals "android" for request #{index}
    And the payload field "events.0.device.totalMemory" is not null for request #{index}
    And the payload field "events.0.device.runtimeVersions.osBuild" is not null for request #{index}
    And the payload field "events.0.metaData.app.name" equals "MazeRunner" for request #{index}
    And the payload field "events.0.metaData.app.packageName" equals "com.bugsnag.android.mazerunner" for request #{index}
    And the payload field "events.0.metaData.app.versionName" is not null for request #{index}
    And the payload field "events.0.metaData.device.brand" is not null for request #{index}
    And the payload field "events.0.metaData.device.dpi" is not null for request #{index}
    And the payload field "events.0.metaData.device.emulator" is true for request #{index}
    And the payload field "events.0.metaData.device.locale" is not null for request #{index}
    And the payload field "events.0.metaData.device.locationStatus" is not null for request #{index}
    And the payload field "events.0.metaData.device.networkAccess" is not null for request #{index}
    And the payload field "events.0.metaData.device.screenDensity" is not null for request #{index}
    And the payload field "events.0.metaData.device.screenResolution" is not null for request #{index}
    And the payload field "events.0.metaData.device.time" is not null for request #{index}
    And the payload field "events.0.severity" is not null for request #{index}
    And the payload field "events.0.severityReason.type" is not null for request #{index}
    And the payload field "events.0.device.cpuAbi" is a non-empty array for request #{index}
  }
end

Then("the report contains the required fields") do
  step("the report in request 0 contains the required fields")
end

Then("the request payload contains a completed native report") do
  step("the payload in request 0 contains a completed native report")
end

Then("the payload in request {int} contains a completed native report") do |index|
  steps %Q{
    And the report in request #{index} contains the required fields
    And the stacktrace in request #{index} contains native frame information
  }
end

Then("the event in request {int} contains session info") do |index|
  steps %Q{
    Then the payload field "events.0.session.startedAt" is not null for request #{index}
    And the payload field "events.0.session.id" is not null for request #{index}
    And the payload field "events.0.session.events.handled" is not null for request #{index}
    And the payload field "events.0.session.events.unhandled" is not null for request #{index}
  }
end

Then("the stacktrace in request {int} contains native frame information") do |request_index|
  step("the payload field \"events.0.exceptions.0.stacktrace\" is a non-empty array for request #{request_index}")
  stack = read_key_path(find_request(request_index)[:body], "events.0.exceptions.0.stacktrace")
  stack.each_with_index do |frame, index|
    assert_not_nil(frame['method'], "The method of frame #{index} is nil")
    assert_not_nil(frame['lineNumber'], "The lineNumber of frame #{index} is nil")
  end
end

Then(/^the payload field "(.+)" is greater than (\d+)(?: for request (\d+))?$/) do |field_path, int_value, request_index|
  observed_value = read_key_path(find_request(request_index)[:body], field_path)
  assert(observed_value > int_value)
end
