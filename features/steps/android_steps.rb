When("I run {string}") do |event_type|
  steps %Q{
    Given the element "scenarioText" is present
    When I send the keys "#{event_type}" to the element "scenarioText"
    And I click the element "startScenarioButton"
  }
end

When("I run {string} and relaunch the app") do |event_type|
  steps %Q{
    When I run "#{event_type}"
    And I clear any error dialogue
    And I relaunch the app
  }
end

When("I clear any error dialogue") do
  sleep(3)
  MazeRunner.driver.click_element("android:id/button1") if MazeRunner.driver.wait_for_element("android:id/button1", 1)
  MazeRunner.driver.click_element("android:id/aerr_close") if MazeRunner.driver.wait_for_element("android:id/aerr_close", 1)
  MazeRunner.driver.click_element("android:id/aerr_restart") if MazeRunner.driver.wait_for_element("android:id/aerr_restart", 1)
end

When("I configure Bugsnag for {string}") do |event_type|
  steps %Q{
    Given the element "scenarioText" is present
    When I send the keys "#{event_type}" to the element "scenarioText"
    And I click the element "startBugsnagButton"
  }
end

When("I relaunch the app") do
  MazeRunner.driver.close_app
  MazeRunner.driver.launch_app
end

When("I tap the screen {int} times") do |count|
  for i in 1..count do
    touch_action = Appium::TouchAction.new
    touch_action.tap({:x => 500, :y => 300})
    touch_action.perform
    sleep(1)
  end
end

When("I configure the app to run in the {string} state") do |event_metadata|
  steps %Q{
    Given the element "scenarioMetaData" is present
    And I send the keys "#{event_metadata}" to the element "scenarioMetaData"
  }
end

Then("the exception reflects a signal was raised") do
  value = read_key_path(Server.current_request[:body], "events.0.exceptions.0")
  error_class = value["errorClass"]
  assert_block("The errorClass was not from a signal: #{error_class}") do
    ["SIGFPE","SIGILL","SIGSEGV","SIGABRT","SIGTRAP","SIGBUS"].include? error_class
  end
end
Then("the event {string} string is empty") do |keypath|
  value = read_key_path(Server.current_request[:body], keypath)
  assert_block("The #{keypath} is not empty: '#{value}'") do
    value.nil? or value.length == 0
  end
end

Then("the event {string} is greater than {int}") do |keypath, int|
  value = read_key_path(Server.current_request[:body], "events.0.#{keypath}")
  assert_false(value.nil?, "The event #{keypath} is nil")
  assert_true(value > int)
end

Then("the exception {string} equals one of:") do |keypath, possible_values|
  value = read_key_path(Server.current_request[:body], "events.0.exceptions.0.#{keypath}")
  assert_includes(possible_values.raw.flatten, value)
end

# Checks whether the first  significant frames match several given frames
#
# @param expected_values [Array] A table dictating the expected files and methods of the frames
#   The first two entries are methods (enabling flexibility across SDKs), the third is the file name
Then("the first significant stack frame methods and files should match:") do |expected_values|
  stacktrace = read_key_path(Server.current_request[:body], "events.0.exceptions.0.stacktrace")
  expected_frame_values = expected_values.raw
  significant_frames = stacktrace.each_with_index.map do |frame, index|
    method = `c++filt -_ _#{frame["method"]}`.chomp
    method = frame["method"] if method == "_#{frame["method"]}"
    insignificant = method.start_with?("bsg_") ||
      method.start_with?("std::") ||
      method.start_with?("__cxx") ||
      frame["file"].start_with?("/system/") ||
      frame["file"].end_with?("libbugsnag-ndk.so")
    { :index => index, :method => method, :file => frame["file"] } unless insignificant
  end
  significant_frames.select! { |frame| frame }
  expected_frame_values.each_with_index do |expected_frame, index|
    test_frame = significant_frames[index]
    method_match_a = expected_frame[0] == test_frame[:method]
    method_match_b = expected_frame[1] == test_frame[:method]
    assert(method_match_a || method_match_b, "'#{test_frame[:method]}' in frame #{test_frame[:index]} is not equal to '#{expected_frame[0]}' or '#{expected_frame[1]}'")
    assert(test_frame[:file].end_with?(expected_frame[2]), "'#{test_frame[:file]}' in frame #{test_frame[:index]} does not end with '#{expected_frame[2]}'")
  end
end

Then("the report contains the required fields") do
  steps %Q{
    And the payload field "notifier.name" is not null
    And the payload field "notifier.url" is not null
    And the payload field "notifier.version" is not null
    And the payload field "events" is a non-empty array
    And the payload field "events.0.unhandled" is not null
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

Then("the request payload contains a completed handled native report") do
  steps %Q{
      And the report contains the required fields
      And the stacktrace contains native frame information
  }
end

Then("the request payload contains a completed unhandled native report") do
  steps %Q{
      And the report contains the required fields
      And the stacktrace contains native frame information
  }
  stack = read_key_path(Server.current_request[:body], "events.0.exceptions.0.stacktrace")
    stack.each_with_index do |frame, index|
      assert_not_nil(frame['symbolAddress'], "The symbolAddress of frame #{index} is nil")
      assert_not_nil(frame['frameAddress'], "The frameAddress of frame #{index} is nil")
      assert_not_nil(frame['loadAddress'], "The loadAddress of frame #{index} is nil")
    end
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
