# Configure app environment
RUNNING_CI = ENV['TRAVIS'] == 'true'

# Install latest versions of bugsnag-android(-ndk)
run_required_commands([
  ["./gradlew", "clean", "sdk:assembleRelease", "ndk:assembleRelease"],
  ["cp", "sdk/build/outputs/aar/bugsnag-android-release.aar",
   "features/fixtures/mazerunner/libs/bugsnag-android.aar"],
  ["cp", "ndk/build/outputs/aar/bugsnag-android-ndk-release.aar",
   "features/fixtures/mazerunner/libs/bugsnag-android-ndk.aar"],
])

# Build the harness app
Dir.chdir('features/fixtures/mazerunner') do
  run_required_commands([
    ["../../../gradlew", "clean", "assembleRelease"],
  ])
end

# Reset orientation after each scenario
at_exit do
  ENV['DEVICE_ORIENTATION'] = 'portrait'
  run_required_commands([["features/scripts/rotate-device.sh"]])
end
