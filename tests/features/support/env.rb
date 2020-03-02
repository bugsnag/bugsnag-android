# Configure app environment
bs_username = ENV['BROWSER_STACK_USERNAME']
bs_access_key = ENV['BROWSER_STACK_ACCESS_KEY']
bs_local_id = ENV['BROWSER_STACK_LOCAL_IDENTIFIER'] || 'maze_browser_stack_test_id'
bs_device = ENV['DEVICE_TYPE']
app_location = ENV['APP_LOCATION']

# Set this explicitly
$api_key = "a35a2a72bd230ac0aa0f52715bbdc6aa"

After do |scenario|
  $driver.reset
end

Before('@skip_above_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if %w[ANDROID_9_0 ANDROID_10_0].include? bs_device
end

Before('@skip_above_android_7') do |scenario|
  skip_this_scenario("Skipping scenario") if %w[ANDROID_8_0 ANDROID_8_1 ANDROID_9_0 ANDROID_10_0].include? bs_device
end

Before('@skip_below_android_9') do |scenario|
  skip_this_scenario("Skipping scenario") unless %w[ANDROID_9_0 ANDROID_10_0].include? bs_device
end

Before('@skip_below_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") unless %w[ANDROID_8_0 ANDROID_8_1 ANDROID_9_0 ANDROID_10_0].include? bs_device
end

Before('@skip_android_8_1') do |scenario|
  skip_this_scenario("Skipping scenario") unless %w[ANDORID_8_1].include? bs_device
end

AfterConfiguration do |config|
  AppAutomateDriver.new(bs_username, bs_access_key, bs_local_id, bs_device, app_location)
  $driver.start_driver
end

at_exit do
  $driver.driver_quit
end
