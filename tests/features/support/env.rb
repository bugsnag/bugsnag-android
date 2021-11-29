# Set this explicitly
$api_key = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"

MazeRunner.config.enforce_bugsnag_integrity = false
MazeRunner.config.receive_requests_wait = 60

Before('@skip_android_9') do |scenario|
  skip_this_scenario("Skipping scenario") if MazeRunner.config.os_version = 9
end

Before('@skip_below_android_9') do |scenario|
  skip_this_scenario("Skipping scenario") if MazeRunner.config.os_version < 9
end

Before('@skip_below_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if MazeRunner.config.os_version < 8
end

Before('@skip_above_android_7') do |scenario|
  skip_this_scenario("Skipping scenario") if MazeRunner.config.os_version > 7
end
