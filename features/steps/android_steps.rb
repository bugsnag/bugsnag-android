When('I clear all persistent data') do
  execute_command :clear_persistent_data
end

def execute_command(action, scenario_name = '')
  command = {
    action: action,
    scenario_name: scenario_name,
    scenario_mode: $scenario_mode,
    sessions_endpoint: $sessions_endpoint,
    notify_endpoint: $notify_endpoint,
    error_config_endpoint: $error_config_endpoint,
  }
  Maze::Server.commands.add command

  # Reset values to defaults
  $scenario_mode = ''
  if Maze.config.farm == :bb
    if Maze.config.aws_public_ip
      $sessions_endpoint = "http://#{Maze.public_address}/sessions"
      $notify_endpoint = "http://#{Maze.public_address}/notify"
      $error_config_endpoint = "http://#{Maze.public_address}"
    else
      $sessions_endpoint = "http://local:9339/sessions"
      $notify_endpoint = "http://local:9339/notify"
      $error_config_endpoint = "http://local:9339"
    end
  else
    $sessions_endpoint = 'http://bs-local.com:9339/sessions'
    $notify_endpoint = 'http://bs-local.com:9339/notify'
    $error_config_endpoint = 'http://bs-local.com:9339'
  end

  # Ensure fixture has read the command
  count = 600
  sleep 0.1 until Maze::Server.commands.size_remaining == 0 || (count -= 1) < 1

  raise 'Test fixture did not GET /command' unless Maze::Server.commands.size_remaining == 0
end

def press_at(x, y)
  touch_action = Appium::TouchAction.new
  touch_action.press({x: x, y: y}).wait(1).release
  begin
    touch_action.perform
  rescue Selenium::WebDriver::Error::ServerError
    # Just ignore it, the press still seems to work
  end
end

When("I clear any error dialogue") do
  # It can take multiple clicks to clear a dialog,
  # so keep pressing until nothing is pressed
  keep_clicking = true
  while keep_clicking
    keep_clicking = click_if_present('android:id/button1') ||
                    click_if_present('android:id/aerr_close') ||
                    click_if_present('android:id/aerr_restart')
  end
end

When('I run {string}') do |scenario_name|
  execute_command :run_scenario, scenario_name
end

When("I run {string} and relaunch the crashed app") do |event_type|
  steps %Q{
    When I run "#{event_type}"
    And I relaunch the app after a crash
  }
end

When("I configure Bugsnag for {string}") do |event_type|
  execute_command :start_bugsnag, event_type
end

When("I terminate the app") do
  Maze::Api::Appium::AppManager.new.terminate
end

When("I close and relaunch the app") do
  Maze::Api::Appium::AppManager.new.terminate
  Maze::Api::Appium::AppManager.new.activate
end

When("I close and relaunch the app after an ANR") do
  begin
    Maze::Api::Appium::AppManager.new.terminate
  rescue Selenium::WebDriver::Error::ServerError
    # Swallow any error, as Android may already have terminated the app
  end
  Maze::Api::Appium::AppManager.new.activate
end

When('I set the screen orientation to portrait') do
  Maze::Api::Appium::DeviceManager.new.set_rotation(:portrait)
end

# Waits for up to 10 seconds for the app to stop running.  It seems that Appium doesn't always
# get the state correct (e.g. when backgrounding the app, or on old Android versions), so we
# don't fail if it still says running after the time allowed.
def wait_for_app_state(expected_state)
  manager = Maze::Api::Appium::AppManager.new
  max_attempts = 20
  attempts = 0
  state = manager.state
  until (attempts >= max_attempts) || state == expected_state
    attempts += 1
    state = manager.state
    sleep 0.5
  end
  $logger.warn "App state #{state} instead of #{expected_state} after 10s" unless state == expected_state
  state
end

When('the app is not running') do
  wait_for_app_state(:not_running)
end

When("I relaunch the app after a crash") do
  manager = Maze::Api::Appium::AppManager.new
  state = wait_for_app_state :not_running
  if state != :not_running
    manager.terminate
  end
  manager.activate
end

When('I cause the ANR dialog to appear') do
  step 'I tap the screen 3 times'
  step 'I press the back button'
end

When("I tap the screen {int} times") do |count|
  (1..count).each { |i|
    begin
      press_at 500, 300
    rescue Selenium::WebDriver::Error::ElementNotInteractableError, Selenium::WebDriver::Error::InvalidElementStateError
      # Ignore it
    end
    sleep(1)
  }
end

When('I press the back button') do
  Maze::Api::Appium::DeviceManager.new.back
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
    And the error payload field "events.0.app.versionCode" equals 1
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

def click_if_present(element)
  return false unless Maze.driver.wait_for_element(element, 1)

  Maze.driver.click_element_if_present(element)
rescue Selenium::WebDriver::Error::UnknownError
  # Ignore Appium errors (e.g. during an ANR)
  return false
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

Then("the event stacktrace has valid addresses") do
  trace = Maze::Helper.read_key_path(Maze::Server.errors.current[:body], "events.0.exceptions.0.stacktrace")
  trace.each_with_index do |frame, index|
    loadAddress = frame['loadAddress']
    frameAddress = frame['frameAddress']
    relPC = frame['lineNumber'].to_i

    Maze.check.match(/^0x[0-9a-fA-F]+$/, loadAddress, "Frame #{index} loadAddress is not a valid hex value")
    Maze.check.match(/^0x[0-9a-fA-F]+$/, frameAddress, "Frame #{index} frameAddress is not a valid hex value")

    loadAddressInt = loadAddress.slice(2, loadAddress.length).to_i(16)
    frameAddressInt = frameAddress.slice(2, frameAddress.length).to_i(16)

    Maze.check.equal(relPC, frameAddressInt - loadAddressInt,
      "lineNumber(#{relPC}) of frame #{index} does not match the frameAddress(#{frameAddress}) - loadAddress(#{loadAddress}) = #{frameAddressInt - loadAddressInt}"
    )
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

Then("the error is correct for {string} or I allow a retry") do |scenario|
  error = Maze::Server.errors.current[:body]
  message = Maze::Helper.read_key_path(error, 'events.0.exceptions.0.message')
  case scenario
  when 'MultiThreadedStartupScenario'
    Maze.dynamic_retry = true if message == 'You must call Bugsnag.start before any other Bugsnag methods'
    Maze.check.equal 'Scenario complete', message
  when 'InForegroundScenario', 'CXXBackgroundNotifyScenario', 'CXXDelayedCrashScenario'
    begin
      step 'the event "app.inForeground" is false'
    rescue Test::Unit::AssertionFailedError
      Maze.dynamic_retry = true
      raise
    end
  end
end

Then("the event has less than {int} breadcrumb(s)") do |expected|
  breadcrumbs = Maze::Server.errors.current[:body]['events'].first['breadcrumbs']
  Maze.check.operator(
    breadcrumbs&.length || 0, :<, expected,
    "Expected event to have less '#{expected}' breadcrumbs, but got: #{breadcrumbs}"
  )
end

Then("the event last breadcrumb has a message that matches the regex {string}") do |pattern|
  lastBreadcrumbName = Maze::Server.errors.current[:body]['events'].first['breadcrumbs'].last['name']
  regex = Regexp.new pattern
  Maze.check.match regex, lastBreadcrumbName
end

def expected_app_version
  ENV['BUILDKITE_BUILD_NUMBER'] || '1.1.14'
end

Then("the session app version matches the built app version") do
  step "the session payload field \"app.version\" equals \"#{expected_app_version}\""
end

Then("the event app version matches the built app version") do
  step "the event \"app.version\" equals \"#{expected_app_version}\""
end
