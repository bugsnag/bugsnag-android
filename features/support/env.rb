# Configure app environment
# Set this explicitly
$api_key = "a35a2a72bd230ac0aa0f52715bbdc6aa"

AfterConfiguration do |_config|
  Maze.config.receive_no_requests_wait = 10
  Maze.config.receive_requests_wait = 60
end

Before('@skip') do |scenario|
  skip_this_scenario("Skipping scenario")
end

Before('@skip_above_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 9
end

Before('@skip_above_android_7') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 8
end

Before('@skip_below_android_9') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version < 9
end

Before('@skip_below_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version < 8
end

Before('@skip_below_android_6') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version < 6
end

Before('@skip_below_android_5') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version < 5
end

Before('@skip_android_10') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version == 10
end

Before('@skip_android_11') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version == 11
end

Before('@skip_samsung') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.driver.capabilities['device']&.downcase&.include? 'samsung'
end

