When('I clear all persistent data') do
  step 'I click the element "clear_persistent_data"'
end

# Waits 5s for an element to be present.  If it isn't assume a system error dialog is
# blocking its view and dismiss it before trying once more.
#
# @step_input element_id [String] The element to wait for
When('any dialog is cleared and the element {string} is present') do |element_id|
  count = 0
  present = false
  timeout = 3
  until present || count > 5
    present = Maze.driver.wait_for_element(element_id, timeout = timeout)
    break if present
    count += 1
    clicked = click_if_present('android:id/button1') ||
              click_if_present('android:id/aerr_close') ||
              click_if_present('android:id/aerr_restart')
    $logger.info "System dialog cleared, reattempting wait_for_element" if clicked
  end

  Maze.check.true(present, "The element #{element_id} could not be found")
end

def execute_command(action, scenario_name)
  command = { action: action, scenario_name: scenario_name, scenario_mode: $scenario_mode }
  Maze::Server.commands.add command

  # Tapping saves a lot of time finding and clicking elements with Appium
  tap_at 100, 100
  $scenario_mode = ''
  $reset_data = false

  # Ensure fixture has read the command
  count = 100
  sleep 0.1 until Maze::Server.commands.remaining.empty? || (count -= 1) < 1
  raise 'Test fixture did not GET /command' unless Maze::Server.commands.remaining.empty?
end

def tap_at(x, y)
  touch_action = Appium::TouchAction.new
  touch_action.tap({:x => x, :y => y})
  touch_action.perform
end

When("I clear any error dialogue") do
  click_if_present 'android:id/button1'
  click_if_present 'android:id/aerr_close'
  click_if_present 'android:id/aerr_restart'
end

When('I run {string}') do |scenario_name|
  step 'I clear any error dialogue'
  execute_command :run_scenario, scenario_name
end

When("I run {string} and relaunch the crashed app") do |event_type|
  steps %Q{
    When I run "#{event_type}"
    And I relaunch the app after a crash
  }
end

When("I configure Bugsnag for {string}") do |event_type|
  step 'I clear any error dialogue'
  execute_command :start_bugsnag, event_type
end

When("I close and relaunch the app") do
  Maze.driver.close_app
  Maze.driver.launch_app
end

When('I set the screen orientation to portrait') do
  Maze.driver.set_rotation(:portrait)
end

When("I relaunch the app after a crash") do
  # This step should only be used when the app has crashed, but the notifier needs a little
  # time to write the crash report before being forced to reopen.  From trials, 2s was not enough.
  # TODO Consider checking when the app has closed using Appium app_state
  sleep(5)
  Maze.driver.launch_app
end

When("I tap the screen {int} times") do |count|
  (1..count).each { |i|
    begin
      tap_at 500, 300
      touch_action.perform
    rescue Selenium::WebDriver::Error::ElementNotInteractableError
      # Ignore it§
    end
    sleep(1)
  }
end

When("I configure the app to run in the {string} state") do |scenario_mode|
  $scenario_mode = scenario_mode
end

Then("the exception reflects a signal was raised") do
  value = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.exceptions.0")
  error_class = value["errorClass"]
  Maze.check.include(%w[SIGFPE SIGILL SIGSEGV SIGABRT SIGTRAP SIGBUS], error_class)
end

Then("the exception {string} equals one of:") do |keypath, possible_values|
  value = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.exceptions.0.#{keypath}")
  Maze.check.include(possible_values.raw.flatten, value)
end

Then("the report contains the required fields") do
  steps %Q{
    And the error payload field "notifier.name" is not null
    And the error payload field "notifier.url" is not null
    And the error payload field "notifier.version" is not null
    And the error payload field "events" is a non-empty array
    And the error payload field "events.0.unhandled" is not null
    And the error payload field "events.0.app.duration" is not null
    And the error payload field "events.0.app.durationInForeground" is not null
    And the error payload field "events.0.app.id" equals "com.bugsnag.android.mazerunner"
    And the error payload field "events.0.app.inForeground" is not null
    And the error payload field "events.0.app.releaseStage" is not null
    And the error payload field "events.0.app.type" equals "android"
    And the error payload field "events.0.app.version" is not null
    And the error payload field "events.0.app.versionCode" equals 34
    And the error payload field "events.0.device.id" is not null
    And the error payload field "events.0.device.locale" is not null
    And the error payload field "events.0.device.manufacturer" is not null
    And the error payload field "events.0.device.model" is not null
    And the error payload field "events.0.device.orientation" is not null
    And the error payload field "events.0.device.osName" equals "android"
    And the error payload field "events.0.device.time" is not null
    And the error payload field "events.0.device.totalMemory" is not null
    And the error payload field "events.0.device.runtimeVersions.osBuild" is not null
    And the error payload field "events.0.metaData.app.name" equals "MazeRunner"
    And the error payload field "events.0.metaData.device.brand" is not null
    And the error payload field "events.0.metaData.device.dpi" is not null
    And the error payload field "events.0.metaData.device.locationStatus" is not null
    And the error payload field "events.0.metaData.device.networkAccess" is not null
    And the error payload field "events.0.metaData.device.screenDensity" is not null
    And the error payload field "events.0.metaData.device.screenResolution" is not null
    And the error payload field "events.0.severity" is not null
    And the error payload field "events.0.severityReason.type" is not null
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
  }
end

Then("the error payload contains a completed handled native report") do
  steps %Q{
      And the report contains the required fields
      And the stacktrace contains native frame information
  }
end

Then("the error payload contains a completed unhandled native report") do
  steps %Q{
      And the report contains the required fields
      And the stacktrace contains native frame information
  }
  stack = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.exceptions.0.stacktrace")
    stack.each_with_index do |frame, index|
      Maze.check.not_nil(frame['symbolAddress'], "The symbolAddress of frame #{index} is nil")
      Maze.check.not_nil(frame['frameAddress'], "The frameAddress of frame #{index} is nil")
      Maze.check.not_nil(frame['loadAddress'], "The loadAddress of frame #{index} is nil")
    end
end

Then("the event contains session info") do
  steps %Q{
    Then the error payload field "events.0.session.startedAt" is not null
    And the error payload field "events.0.session.id" is not null
    And the error payload field "events.0.session.events.handled" is not null
    And the error payload field "events.0.session.events.unhandled" is not null
  }
end

Then("the stacktrace contains native frame information") do
  step("the error payload field \"events.0.exceptions.0.stacktrace\" is a non-empty array")
  stack = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.exceptions.0.stacktrace")
  stack.each_with_index do |frame, index|
    Maze.check.not_nil(frame['method'], "The method of frame #{index} is nil")
    Maze.check.not_nil(frame['lineNumber'], "The lineNumber of frame #{index} is nil")
  end
end

Then("the event has {int} breadcrumbs") do |expected_count|
  value = Maze::Server.errors.current[:body]["events"].first["breadcrumbs"]
  fail("Incorrect number of breadcrumbs found: #{value.length()}, expected: #{expected_count}") if value.length() != expected_count.to_i
end

Then("the event has a {string} breadcrumb with the message {string}") do |type, message|
  value = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.breadcrumbs")
  found = false
  value.each do |crumb|
    if crumb["type"] == type and crumb["name"] == message
      found = true
    end
  end
  fail("No breadcrumb matched: #{value}") unless found
end

Then("the exception stacktrace matches the thread stacktrace") do
  exc_trace = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.exceptions.0.stacktrace")
  thread_trace = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.threads.0.stacktrace")
  Maze.check.equal(exc_trace.length(),
                   thread_trace.length(),
                   "Exception and thread stacktraces are different lengths.")

  thread_trace.each_with_index do |thread_frame, index|
    exc_frame = exc_trace[index]
    Maze.check.equal(exc_frame, thread_frame)
  end
end

def click_if_present(element)
  return false unless Maze.driver.wait_for_element(element, 1)

  Maze.driver.click_element_if_present(element)
rescue Selenium::WebDriver::Error::UnknownError
  # Ignore Appium errors (e.g. during an ANR)
  return false
end

Then("I sort the errors by {string}") do |comparator|
  Maze::Server.errors.remaining.sort_by { |request|
    Maze::Helper.read_key_path(request[:body], comparator)
  }
end

Then("the exception stacktrace matches the thread stacktrace") do
  exc_trace = read_key_path(Server.current_request[:body], "events.0.exceptions.0.stacktrace")
  thread_trace = read_key_path(Server.current_request[:body], "events.0.threads.0.stacktrace")
  Maze.check.equal(exc_trace.length(),
                   thread_trace.length(),
                   "Exception and thread stacktraces are different lengths.")

  thread_trace.each_with_index do |thread_frame, index|
    exc_frame = exc_trace[index]
    Maze.check.equal(exc_frame, thread_frame)
  end
end

Then("the event binary arch field is valid") do
  arch = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.app.binaryArch")
  Maze.check.include(%w[x86 x86_64 arm32 arm64], arch)
end

Then("the event stacktrace identifies the program counter") do
  trace = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.exceptions.0.stacktrace")
  trace.each_with_index do |frame, index|
    if index == 0
      Maze.check.equal(frame["isPC"], true, "The first frame should be the program counter")
    else
      Maze.check.equal(frame["isPC"], nil, "The #{index} frame should not be the program counter")
    end
  end
end

# EventStore flushes multiple times on launch with access controlled via a semaphore,
# which results in multiple similar log messages
Then("Bugsnag confirms it has no errors to send") do
  steps %Q{
    And I wait to receive 2 logs
    Then the "debug" level log message equals "No startupcrash events to flush to Bugsnag."
    And I discard the oldest log
    Then the "debug" level log message equals "No regular events to flush to Bugsnag."
  }
end
