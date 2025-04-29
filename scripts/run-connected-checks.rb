#!/usr/bin/env ruby

require 'pty'
require 'open3'
require 'timeout'

# === CONFIGURATION ===
raise('❌ API_LEVEL environment variable must be set') unless ENV['API_LEVEL']

target_api_level = ENV['API_LEVEL']
agent_name = ENV['BUILDKITE_AGENT_NAME'] || 'default'
avd_name = "test-sdk-#{target_api_level}-#{agent_name}"
avd_port = ENV['AVD_PORT'] || '5037' # Note: 5554 is default emulator port; 5037 is ADB server

# === DETERMINE ARCHITECTURE ===
sys_arch = `uname -m`.strip
sys_arch = 'arm64-v8a' if sys_arch == 'arm64'

# === CHECK AVD EXISTS ===
avd_list = `avdmanager list avd -c`
if avd_list.lines.none? { |line| line.strip == avd_name }
  puts "🔧 AVD '#{avd_name}' not found, creating..."

  # Check SDK system image
  sdk_path = "system-images;android-#{target_api_level};google_apis;#{sys_arch}"
  unless `sdkmanager --list_installed`.include?(sdk_path)
    puts "⬇️  Installing missing system image: #{sdk_path}"
    system("sdkmanager '#{sdk_path}'") or raise("❌ Failed to install system image")
  end

  # Create AVD
  system("avdmanager -s create avd -n #{avd_name} -k '#{sdk_path}'") or raise("❌ Failed to create AVD")
else
  puts "✅ AVD '#{avd_name}' already exists"
end

# === START EMULATOR ===
emulator_pid = nil
emulator_lines = Queue.new

puts "🚀 Starting emulator: #{avd_name}"

emulator_thread = Thread.new do
  PTY.spawn('emulator', '-avd', avd_name,
            '-no-window', '-gpu', 'swiftshader_indirect',
            '-noaudio', '-no-boot-anim',
            '-camera-back', 'none', '-no-snapshot-load',
            '-port', avd_port) do |stdout, _stdin, pid|
    emulator_pid = pid
    stdout.each do |line|
      puts line
      emulator_lines << line
    end
  end
end

# === WAIT FOR BOOT ===
puts "⏳ Waiting for emulator to boot..."

booted = false
begin
  Timeout.timeout(90) do
    until booted
      line = emulator_lines.pop
      booted = line.include?('Boot completed')
    end
  end
rescue Timeout::Error
  raise '❌ Emulator failed to boot within 90 seconds'
end

puts '✅ Emulator booted successfully'

# === RUN TESTS ===
exit_status = nil
puts "🧪 Running connectedCheck tests..."

Open3.popen2e('./gradlew connectedCheck -x :bugsnag-benchmarks:connectedCheck') do |_stdin, output, wait_thr|
  output.each { |line| puts line }
  exit_status = wait_thr.value
end

# === CLEANUP ===
puts '🛑 Stopping emulator...'
if emulator_pid
  Process.kill('INT', emulator_pid)
  emulator_thread.join
end

# === EXIT WITH TEST STATUS ===
exit(exit_status.exitstatus) unless exit_status.success?
puts '✅ Tests completed successfully'
