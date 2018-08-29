# Configure app environment

AVD_SPECS = [
  ["ARMv7a_API_14", "armeabi-v7a", "system-images;android-14;default;armeabi-v7a"],
  ["ARMv7a_API_18", "armeabi-v7a", "system-images;android-18;default;armeabi-v7a"],
  ["x86_API_19", "x86", "system-images;android-19;google_apis;x86"],
  ["x86_API_24", "x86", "system-images;android-24;default;x86"],
  ["x86_64_API_24", "x86_64", "system-images;android-24;default;x86_64"],
  ["ARM64v8a_API_24", "arm64-v8a", "system-images;android-24;default;arm64-v8a"],
  ["x86_64_API_27", "x86_64", "system-images;android-27;default;x86_64"],
]

# Install latest versions of bugsnag-android(-ndk)
run_required_commands([
  ["./gradlew", "clean", "sdk:assembleRelease", "ndk:assembleRelease"],
  ["cp", "sdk/build/outputs/aar/bugsnag-android-release.aar",
   "features/fixtures/mazerunner/libs/bugsnag-android.aar"],
  ["cp", "ndk/build/outputs/aar/bugsnag-android-ndk-release.aar",
   "features/fixtures/mazerunner/libs/bugsnag-android-ndk.aar"],
])
## Generate AVDs
#run_required_commands(AVD_SPECS.map {|spec|
  #[ENV['ANDROID_HOME'] + '/tools/bin/sdkmanager', "\"#{spec[2]}\""]
#})
#run_required_commands(AVD_SPECS.map {|spec|
  #["echo no", "|", ENV['ANDROID_HOME'] + '/tools/bin/avdmanager','create avd --name ', spec[0], '--force', ' -b ', spec[1], ' -k ', "\"#{spec[2]}\""]
#})

Dir.chdir('features/fixtures/mazerunner') do
  run_required_commands([
    ["../../../gradlew", "clean", "assembleRelease"],
  ])
end

#at_exit do
  #run_required_commands(AVD_SPECS.map{|spec|
    #[ENV['ANDROID_HOME'] + '/tools/bin/avdmanager', 'delete avd --name', spec.first]
  #})
#end
