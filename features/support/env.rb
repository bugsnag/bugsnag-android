# Configure app environment

RUNNING_CI = ENV['TRAVIS'] == 'true'

run_required_commands([
  ["./gradlew", "clean", "mazerunner:assembleRelease"],
])
