#!/usr/bin/env ruby
# Ensure that logs are immediately flushed to stdout and stderr
$stderr.sync = true
$stdout.sync = true

# Require 'open3' to enable running shell commands with access to stdin, stdout, and stderr streams
require 'open3'

# Runs a shell command and exits the script if the command fails
def run_cmd(cmd, error_message)
  puts "Running: #{cmd}"
  # Execute the command and return true if successful, false otherwise
  success = system(cmd)
  unless success
    STDERR.puts error_message
    exit 1
  end
end

# Uploads symbol and mapping files to the specified destination using the Bugsnag CLI
def upload_files(api_key, dest)
  Dir.chdir('features/fixtures/mazerunner') do
    upload = "bugsnag-cli upload"
    version_code = ENV['BUILDKITE_BUILD_NUMBER']
    options = "--api-key=#{api_key} --version-code=#{version_code}"
    puts "Uploading symbol files to #{dest}"
    run_cmd("#{upload} android-ndk #{options}", "Failed to upload android-ndk symbols to #{dest}")
    puts "Uploading mapping file to #{dest}"
    run_cmd("#{upload} android-proguard #{options} --application-id=com.bugsnag.android.mazerunner", "Failed to upload android-proguard mapping to #{dest}")
  end
end

# Check that exactly one argument (NDK version) is provided
if ARGV.length != 1
  puts 'Usage: ruby scripts/build-test-fixture.rb <NDK version>'
  exit 1
end
ndk_version = ARGV[0]

# Build the test fixture using the specified NDK version
puts 'Building test fixture'
run_cmd("bundle install", "Failed to install bundle")
run_cmd("make fixture-#{ndk_version}", "Failed to build fixture for #{ndk_version}")

upload_files(ENV['MAZE_REPEATER_API_KEY'], 'bugsnag.com') if ENV['MAZE_REPEATER_API_KEY']
upload_files(ENV['MAZE_HUB_REPEATER_API_KEY'], 'Insight Hub') if ENV['MAZE_HUB_REPEATER_API_KEY']

# Upload the built app to BrowserStack and BitBar for testing
puts 'Uploading to BrowserStack and BitBar'
run_cmd("bundle exec upload-app --farm=bb --app=./build/fixture-#{ndk_version}.apk --app-id-file=build/fixture-#{ndk_version}-url.txt", "Failed to upload app to BitBar")
run_cmd("bundle exec upload-app --farm=bs --app=./build/fixture-#{ndk_version}.apk --app-id-file=build/bs-fixture-#{ndk_version}-url.txt", "Failed to upload app to BrowserStack")
