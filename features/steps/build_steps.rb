# Possibly generic steps to upstream

When("I run {string} with the defaults") do |eventType|
  steps %Q{
    When I start Android emulator "newnexus"
    And I install the "com.bugsnag.android.mazerunner" app from "mazerunner/build/outputs/apk/release/mazerunner-release.apk"
    puts ENV
    And I clear the "com.bugsnag.android.mazerunner" app data
    And I set environment variable "BUGSNAG_API_KEY" to "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And I set environment variable "EVENT_TYPE" to "#{eventType}"
    And I start the "com.bugsnag.android.mazerunner" app using the "com.bugsnag.android.mazerunner.MainActivity" activity
  }
end

When("I start Android emulator {string}") do |emulator|
  steps %Q{
    When I set environment variable "EMULATOR" to "#{emulator}"
    And I run the script "features/scripts/launch-emulator.sh"
    And I run the script "features/scripts/await-emulator.sh" synchronously
  }
end
When("I install the {string} app from {string}") do |bundle, filepath|
  steps %Q{
    When I set environment variable "APP_BUNDLE" to "#{bundle}"
    And I set environment variable "APK_PATH" to "#{filepath}"
    And I run the script "features/scripts/install-app.sh" synchronously
  }
end
When("I start the {string} app using the {string} activity") do |app, activity|
  steps %Q{
    When I set environment variable "APP_BUNDLE" to "#{app}"
    When I set environment variable "APP_ACTIVITY" to "#{activity}"
    And I run the script "features/scripts/launch-app.sh" synchronously
    And I wait for 4 seconds
  }
end
When("I wait for the {string} app to close") do |app|
  step('I run the script "features/scripts/await-app-close.sh" synchronously')
end
When("I clear the {string} app data") do |app|
  step('I run the script "features/scripts/clear-app-data.sh" synchronously')
end

# Project-specific steps
When("I build the app") do
  steps %Q{
    When I run the script "features/scripts/build-app.sh"
    And I wait for 8 seconds
  }
end
When("I launch the app") do
  steps %Q{
    When I run the script "features/scripts/launch-app.sh"
    And I wait for 5 seconds
  }
end
When("I configure the app to trigger {string}") do |event_type|
end
