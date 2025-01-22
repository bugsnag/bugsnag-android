require 'fileutils'

raise 'Expected 2 arguments' if ARGV.size != 2
build_mode = ARGV[0]
ndk_version = ARGV[1]

destination = "build/fixture-#{ndk_version}"

FileUtils.mkdir_p destination
FileUtils.cp "features/fixtures/mazerunner/app/build/outputs/apk/#{build_mode}/fixture-#{ndk_version}.apk", "build/fixture-#{ndk_version}.apk"
mapping_txt = "features/fixtures/mazerunner/app/build/outputs/mapping/#{build_mode}/mapping.txt"
FileUtils.cp mapping_txt, "#{destination}/mapping.txt" if File.exist? mapping_txt

fixture_dir = 'features/fixtures/mazerunner'
cxx_base = "#{fixture_dir}/cxx-scenarios/build/intermediates"
cxx_bsg_base = "#{fixture_dir}/cxx-scenarios-bugsnag/build/intermediates"

cxx_dir = `ls -dt #{cxx_base}/cxx/*/*/obj | head -n 1`.strip
cxx_bsg_dir = `ls -dt #{cxx_bsg_base}/cxx/*/*/obj | head -n 1`.strip

puts "Copying cxx-scenarios objects from: #{cxx_dir}"
puts "Copying cxx-scenarios-bugsnag objects from: #{cxx_bsg_dir}"

FileUtils.cp "#{cxx_dir}/x86_64/libcxx-scenarios.so", "#{destination}/libcxx-scenarios-x86_64.so"
FileUtils.cp "#{cxx_dir}/x86/libcxx-scenarios.so", "#{destination}/libcxx-scenarios-x86.so"
FileUtils.cp "#{cxx_dir}/arm64-v8a/libcxx-scenarios.so", "#{destination}/libcxx-scenarios-arm64.so"
FileUtils.cp "#{cxx_dir}/armeabi-v7a/libcxx-scenarios.so", "#{destination}/libcxx-scenarios-arm32.so"

FileUtils.cp "#{cxx_bsg_dir}/x86_64/libcxx-scenarios-bugsnag.so", "#{destination}/libcxx-scenarios-bugsnag-x86_64.so"
FileUtils.cp "#{cxx_bsg_dir}/x86/libcxx-scenarios-bugsnag.so", "#{destination}/libcxx-scenarios-bugsnag-x86.so"
FileUtils.cp "#{cxx_bsg_dir}/arm64-v8a/libcxx-scenarios-bugsnag.so", "#{destination}/libcxx-scenarios-bugsnag-arm64.so"
FileUtils.cp "#{cxx_bsg_dir}/armeabi-v7a/libcxx-scenarios-bugsnag.so", "#{destination}/libcxx-scenarios-bugsnag-arm32.so"
