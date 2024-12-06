BeforeAll do
  $api_key = "a35a2a72bd230ac0aa0f52715bbdc6aa"
  Maze.config.receive_no_requests_wait = 10
  Maze.config.receive_requests_wait = 60
end

Before do
  $scenario_mode = ''
  if Maze.config.farm == :bb
    if Maze.config.aws_public_ip
      $sessions_endpoint = "http://#{Maze.public_address}/sessions"
      $notify_endpoint = "http://#{Maze.public_address}/notify"
    else
      $sessions_endpoint = "http://local:9339/sessions"
      $notify_endpoint = "http://local:9339/notify"
    end
  else
    $sessions_endpoint = 'http://bs-local.com:9339/sessions'
    $notify_endpoint = 'http://bs-local.com:9339/notify'
  end
end

Before('@skip') do |scenario|
  skip_this_scenario("Skipping scenario")
end

Before('@skip_above_android_11') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 11
end

Before('@skip_above_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 9
end

Before('@skip_above_android_7') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 8
end

Before('@skip_below_android_11') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version < 11
end

Before('@skip_below_android_12') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version < 12
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

Before('@skip_android_13') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version.floor == 13
end

Before('@skip_android_10') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version.floor == 10
end

Before('@skip_android_7') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version.floor == 7
end

Before('@skip_android_6') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version.floor == 6
end

Before('@skip_samsung') do |scenario|
  caps = Maze.driver.capabilities
  options_cap = 'bitbar:options'
  device_cap = 'device'

  device = if caps.has_key?(options_cap)
             caps[options_cap][device_cap]
           else
             caps[device_cap]
           end

  skip_this_scenario("Skipping scenario") if device&.downcase&.include? 'samsung'
end

