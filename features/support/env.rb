# Configure app environment

run_required_commands([
  ["./gradlew", "clean", "assembleRelease"],
  ["cp", "sdk/build/outputs/aar/bugsnag-android-release.aar",
   "features/fixtures/mazerunner/libs/bugsnag-android.aar"],
])

Dir.chdir('features/fixtures/mazerunner') do
  run_required_commands([
    ["../../../gradlew", "clean", "assembleRelease"],
  ])
end
