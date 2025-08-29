def upload_files(api_key, dest)
  Dir.chdir('features/fixtures/mazerunner') do
    upload = "bugsnag-cli upload"
    version_code = ENV['BUILDKITE_BUILD_NUMBER']
    options = "--api-key=#{api_key} --version-code=#{version_code}"
    puts "Uploading symbol files to #{dest}"
    puts `#{upload} android-ndk #{options}`
    puts "Uploading mapping file to #{dest}"
    puts `#{upload} android-proguard #{options} --application-id=com.bugsnag.android.mazerunner`
  end
end

if ARGV.length != 1
  puts 'Usage: ruby scripts/build-test-fixture.rb <NDK version>'
  exit 1
end
ndk_version = ARGV[0]

puts 'Building test fixture'
puts `bundle install`
puts `make fixture-#{ndk_version}`

upload_files(ENV['MAZE_REPEATER_API_KEY'], 'bugsnag.com') if ENV['MAZE_REPEATER_API_KEY']
upload_files(ENV['MAZE_HUB_REPEATER_API_KEY'], 'Insight Hub') if ENV['MAZE_HUB_REPEATER_API_KEY']

puts 'Uploading to BrowserStack and BitBar'
puts `bundle exec upload-app --farm=bb --app=./build/fixture-#{ndk_version}.apk --app-id-file=build/fixture-#{ndk_version}-url.txt`
puts `bundle exec upload-app --farm=bs --app=./build/fixture-#{ndk_version}.apk --app-id-file=build/bs-fixture-#{ndk_version}-url.txt`
