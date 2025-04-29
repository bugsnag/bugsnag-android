#!/usr/bin/env ruby
require 'pty'
require 'open3'

# Ensure API_LEVEL is set
raise('API_LEVEL environment variable must be set') unless ENV['API_LEVEL']
target_api_level = ENV['API_LEVEL']

# Fetch target API level once
target_api_level = ENV['API_LEVEL']
avd_name = "test-sdk-#{target_api_level}"

# Check if the AVD exists
avd_exists = `avdmanager list avd -c | grep #{avd_name}`.strip

if avd_exists.empty?
  puts "AVD #{avd_name} does not exist, creating it now"

  # Detect system architecture
  sys_arch = `uname -m`.strip
  sys_arch = 'arm64-v8a' if sys_arch == 'arm64'

  # Define SDK path string
  sdk_path = "system-images;android-#{target_api_level};google_apis;#{sys_arch}"

  # Check if SDK is installed
  sdk_installed = `sdkmanager --list_installed | grep "#{sdk_path}"`.strip

  if sdk_installed.empty?
    puts "System image for API level #{target_api_level} is not installed, installing it now"
    system("sdkmanager \"#{sdk_path}\"")
  end

  # Create the AVD
  system("avdmanager -s create avd -n #{avd_name} -k \"#{sdk_path}\"")
else
  puts "AVD #{avd_name} already exists, skipping creation"
end

begin
  emulator_pid = nil
  emulator_lines = []
  emulator_thread = Thread.new do
    PTY.spawn('emulator', '-avd', "test-sdk-#{target_api_level}", '-no-window', '-gpu', 'swiftshader_indirect', '-noaudio', '-no-boot-anim', '-camera-back', 'none', '-no-snapshot-load') do |stdout, _stdin, pid|
      emulator_pid = pid
      stdout.each do |line|
        emulator_lines << line
        puts line
      end
    end
  end

  # Wait for the emulator to boot
  start_time = Time.now
  until emulator_lines.any? { |line| line.include?('Boot completed') }
    if Time.now - start_time > 60
      raise 'Emulator did not boot in 60 seconds'
    end
  end

  puts 'Emulator booted successfully'

  # Run the connectedCheck tests
  exit_status = nil
  Open3.popen2e('./gradlew connectedCheck -x :bugsnag-benchmarks:connectedCheck') do |_stdin, stdout_stderr, wait_thr|
    stdout_stderr.each { |line| puts line }
    exit_status = wait_thr.value
  end
ensure
  # Stop the emulator
  puts 'Stopping emulator process'
  Process.kill('INT', emulator_pid) if emulator_pid
  emulator_thread.join
end

unless exit_status.success?
  exit(exit_status.exitstatus)
end
