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
end

Before('@skip') do |scenario|
  skip_this_scenario("Skipping scenario")
end

Before('@skip_above_android_13') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 13
end

Before('@skip_above_android_11') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 11
end

Before('@skip_above_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version >= 9
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

Before('@skip_android_10') do |scenario|
  skip_this_scenario("Skipping scenario") if Maze.config.os_version.floor == 10
end
