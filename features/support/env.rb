# Configure app environment
RUNNING_CI = ENV['TRAVIS'] == 'true'

# Install latest versions of bugsnag-android
run_required_commands([
  [
    "./gradlew", "sdk:assembleRelease", "-PreleaseNdkArtefact=true",
    "-x", "lintVitalRelease",
    "-x", "countReleaseDexMethods"
  ],
  ["cp", "sdk/build/outputs/aar/bugsnag-android-*.aar",
   "features/fixtures/mazerunner/libs/bugsnag-android-ndk.aar"],
])

# Build the harness app
Dir.chdir('features/fixtures/mazerunner') do
  run_required_commands([
    [
      "../../../gradlew", "assembleRelease",
      "-x", "lintVitalRelease"
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
