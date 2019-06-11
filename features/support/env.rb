# Configure app environment
bs_username = ENV['BROWSER_STACK_USERNAME']
bs_access_key = ENV['BROWSER_STACK_ACCESS_KEY']
bs_local_id = ENV['BROWSER_STACK_LOCAL_IDENTIFIER'] || 'maze_browser_stack_test_id'
bs_device = ENV['DEVICE_TYPE']
app_location = ENV['APP_LOCATION']

# Set this explicitly
$api_key = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"

After do |scenario|
  $driver.reset
end

AfterConfiguration do |config|
  AppAutomateDriver.new(bs_username, bs_access_key, bs_local_id, bs_device, app_location)
  $driver.start_driver
end

at_exit do
  $driver.driver_quit
end



FAILED_SCENARIO_OUTPUT_PATH = "/app/maze-output"

After do |scenario|
  if scenario.failed?
    write_failed_requests_to_disk(scenario)
  end
end

def write_failed_requests_to_disk(scenario)
  pp Server.current_request[:body]
end
