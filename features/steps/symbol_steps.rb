# Steps for validating native stack trace contents.
# Depends on the the following binaries being available in $PATH:
#
# * c++filt
# * addr2line

# Sentinel value which can be used in stack frame matching to ignore location info
IGNORED_VALUE = '(ignore)'
# Location on disk of native symbol files
SYMBOL_DIR = ENV['TEST_FIXTURE_SYMBOL_DIR'] || 'build'

# Checks whether the first significant frames in an event match provided frames
#
# @param expected_values [Array] A table dictating the expected files and methods of the frames
#   The table is formatted as any of:
#       | method |
#   or
#       | method | binary filename  |
#   or
#       | method | source file name | line number |
#   or
#       | method | (ignore)         |
#   or
#       | method | (ignore)         | (ignore)    |
Then("the first significant stack frames match:") do |expected_values|
  body = Maze::Server.errors.current[:body]
  arch = Maze::Helper.read_key_path(body, "events.0.app.binaryArch")
  stacktrace = Maze::Helper.read_key_path(body, "events.0.exceptions.0.stacktrace")

  significant_frames = stacktrace.map { |frame| symbolicate(arch, frame) }.flatten.compact

  expected_values.raw.each_with_index do |expected_frame, index|
    raise "No matching significant frame at index #{index}" if significant_frames.length <= index

    test_frame = significant_frames[index]
    Maze.check.equal(
      expected_frame[0], test_frame[:method],
      "'#{test_frame[:method]}' in frame #{index} is not equal to '#{expected_frame[0]}'. Significant frames: #{significant_frames}"
    )
    if expected_frame.length > 1 && expected_frame[1] != IGNORED_VALUE
      Maze.check.true(
        test_frame[:file].end_with?(expected_frame[1]),
        "'#{test_frame[:file]}' in frame #{index} does not end with '#{expected_frame[1]}'. Significant frames: #{significant_frames}"
      )
    end
    if expected_frame.length > 2 && expected_frame[2] != IGNORED_VALUE
      Maze.check.equal(test_frame[:lineNumber], expected_frame[2],
        "line number #{test_frame[:lineNumber]} in frame #{index} does not equal #{expected_frame[2]}. Significant frames: #{significant_frames}"
      )
    end
  end
end

# skip step unless on specified architecture
Then(/^on (arm32|arm64|x86|x86_64), (.+)$/) do |arch, step_text, table|
  body = Maze::Server.errors.current[:body]
  actual_arch = Maze::Helper.read_key_path(body, "events.0.app.binaryArch")
  step(step_text, table) if arch == actual_arch
end

Then("the exception {string} demangles to {string}") do |keypath, expected_value|
  body = Maze::Server.errors.current[:body]
  actual_value = Maze::Helper.read_key_path(body, "events.0.exceptions.0.#{keypath}")
  demangled_value = demangle(actual_value)
  Maze.check.equal(demangled_value, expected_value,
                  "expected '#{actual_value}' to demangle to '#{expected_value}' but was '#{demangled_value}'")
end

def is_out_of_project? file, method
  # no binary was found to match the address
  file.nil? ||
    # in native functions from bugsnag-plugin-android-ndk
    method.start_with?("bsg_") || file.end_with?("libbugsnag-ndk.so") ||
    # c++ standard library + llvm hooks (__cxx_*, __cxa_*, etc)
    method.start_with?("std::") || method.start_with?("__cx") ||
    # gnu hooks
    method.start_with?("__gnu") ||
    # failed to resolve a symbol location
    method.start_with?("0x") ||
    # sneaky libc functions
    method.start_with?("str") || method.start_with?("abort") ||
    # android built-in libraries
    file.start_with?("/system/") || file.start_with?("/apex/") || file.start_with?("/system_root/")
end

def demangle symbol
  `c++filt --types --no-strip-underscore #{symbol}`.chomp
end

def lookup_address binary, address
  info = `addr2line --exe '#{binary}' --inlines --basenames --functions --demangle 0x#{address.to_s(16)}`.chomp
  return nil if info.start_with? '??' # failed to resolve
  # can return multiple if there are inlined frames
  frames = info.split("\n").each_slice(2).map do |function_name, location|
    file, line = location.split(':')
    { file: file, lineNumber: line, method: function_name }
  end.reject { |frame| is_out_of_project?(frame[:file], frame[:method]) }

  return nil if frames.length == 0

  frames
end

# Resolve file and method name for in-project contents, returning nil
# if the frame is not in project.
def symbolicate arch, frame
  method = demangle(frame["method"])
  binary_file = frame["file"]&.split('!')&.last

  return nil if is_out_of_project?(binary_file, method)

  symbol_file = File.join(SYMBOL_DIR, "#{File.basename(binary_file, '.so')}-#{arch}.so")

  if File.exist?(symbol_file) and sym_info = lookup_address(symbol_file, frame["lineNumber"])
    return sym_info
  end
  [{ :method => method, :file => binary_file }]
end
