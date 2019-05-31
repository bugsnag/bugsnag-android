When(/^I run "([^"]+)"$/) do |event_type|
  steps %Q{
    Given the element "scenarioText" is present
    And the element "startScenarioButton" is present
    And I send the keys "#{event_type}" to the element "scenarioText"
    And I click the element "startScenarioButton"
    And I clear any error dialogue
  }
end

When("I clear any error dialogue") do
  begin
    $driver.wait_for_element("android:id/aerr_close", 2)
  rescue Selenium::WebDriver::Error::TimeoutError
  else
    $driver.click_element("android:id/aerr_close")
  end
  begin
    $driver.wait_for_element("android:id/button1", 2)
  rescue Selenium::WebDriver::Error::TimeoutError
  else
    $driver.click_element("android:id/button1")
  end
end

When("I relaunch the app") do
  $driver.close_app
  $driver.launch_app
end

When("I configure the app to run in the {string} state") do |event_metadata|
  steps %Q{
    Given the element "scenarioMetaData" is present
    And I send the keys "#{event_metadata}" to the element "scenarioMetaData"
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

Then("the report contains the required fields") do
  steps %Q{
    And the payload field "notifier.name" is not null
    And the payload field "notifier.url" is not null
    And the payload field "notifier.version" is not null
    And the payload field "events" is a non-empty array
    Ahen the payload field "events.0.unhandled" is not null
    And the payload field "events.0.app.duration" is not null
    And the payload field "events.0.app.durationInForeground" is not null
    And the payload field "events.0.app.id" is not null
    And the payload field "events.0.app.inForeground" is not null
    And the payload field "events.0.app.releaseStage" is not null
    And the payload field "events.0.app.type" equals "android"
    And the payload field "events.0.app.version" is not null
    And the payload field "events.0.app.versionCode" equals 34
    And the payload field "events.0.device.id" is not null
    And the payload field "events.0.device.manufacturer" is not null
    And the payload field "events.0.device.model" is not null
    And the payload field "events.0.device.orientation" is not null
    And the payload field "events.0.device.osName" equals "android"
    And the payload field "events.0.device.totalMemory" is not null
    And the payload field "events.0.device.runtimeVersions.osBuild" is not null
    And the payload field "events.0.metaData.app.name" equals "MazeRunner"
    And the payload field "events.0.metaData.app.packageName" equals "com.bugsnag.android.mazerunner"
    And the payload field "events.0.metaData.app.versionName" is not null
    And the payload field "events.0.metaData.device.brand" is not null
    And the payload field "events.0.metaData.device.dpi" is not null
    And the payload field "events.0.metaData.device.emulator" is true
    And the payload field "events.0.metaData.device.locale" is not null
    And the payload field "events.0.metaData.device.locationStatus" is not null
    And the payload field "events.0.metaData.device.networkAccess" is not null
    And the payload field "events.0.metaData.device.screenDensity" is not null
    And the payload field "events.0.metaData.device.screenResolution" is not null
    And the payload field "events.0.metaData.device.time" is not null
    And the payload field "events.0.severity" is not null
    And the payload field "events.0.severityReason.type" is not null
    And the payload field "events.0.device.cpuAbi" is a non-empty array
  }
end

Then("the request payload contains a completed native report") do
  steps %Q{
    And the report contains the required fields
    And the stacktrace contains native frame information
  }
end

Then("the event contains session info") do
  steps %Q{
    Then the payload field "events.0.session.startedAt" is not null
    And the payload field "events.0.session.id" is not null
    And the payload field "events.0.session.events.handled" is not null
    And the payload field "events.0.session.events.unhandled" is not null
  }
end

Then("the stacktrace contains native frame information") do
  step("the payload field \"events.0.exceptions.0.stacktrace\" is a non-empty array")
  stack = read_key_path(Server.current_request[:body], "events.0.exceptions.0.stacktrace")
  stack.each_with_index do |frame, index|
    assert_not_nil(frame['method'], "The method of frame #{index} is nil")
    assert_not_nil(frame['lineNumber'], "The lineNumber of frame #{index} is nil")
  end
end

Then("the payload field {string} is greater than {int}") do |field_path, int_value|
  observed_value = read_key_path(Server.current_request[:body], field_path)
  assert(observed_value > int_value)
end