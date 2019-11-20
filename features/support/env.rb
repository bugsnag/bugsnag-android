require 'open3'

# Configure app environment
RUNNING_CI = ENV['TRAVIS'] == 'true'

# Install latest versions of bugsnag-android
  run_required_commands([
    [
      "./gradlew", "assembleRelease", "publishToMavenLocal"
    ],
  ])

# Build the harness app
Dir.chdir('features/fixtures/mazerunner') do
  run_required_commands([
    [
     "../../../gradlew", "assembleRelease"
    ],
  ])
end

# Close any lingering ANR dialogs
Before('@anr') do
  run_required_commands([['features/scripts/close-anr-dialog.sh']])
end
After('@anr') do
  sleep(5)
  run_required_commands([['features/scripts/close-anr-dialog.sh']])
end

# Reset orientation after each scenario
at_exit do
  ENV['DEVICE_ORIENTATION'] = 'portrait'
  run_required_commands([["features/scripts/rotate-device.sh"]])
end

Before('@skip_below_android_8') do |scenario|
  skip_this_scenario("Skipping scenario") if get_api_level() < 26
end

Before('@skip_above_android_7') do |scenario|
  skip_this_scenario("Skipping scenario") if get_api_level() >= 26
end

def get_api_level
  stdout, stderr, status = Open3.capture3("adb shell getprop ro.build.version.sdk")
  assert_true(status.success?)
  return stdout.to_i
end

