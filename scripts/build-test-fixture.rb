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

puts 'Building test fixture'
puts `bundle install`
puts `make fixture-r21`
puts `make fixture-r28`

upload_files(ENV['MAZE_REPEATER_API_KEY'], 'bugsnag.com') if ENV['MAZE_REPEATER_API_KEY']
upload_files(ENV['MAZE_HUB_REPEATER_API_KEY'], 'Insight Hub') if ENV['MAZE_HUB_REPEATER_API_KEY']

puts 'Uploading to BrowserStack and BitBar'
puts `bundle exec upload-app --farm=bb --app=./build/fixture-r21.apk --app-id-file=build/fixture-r21-url.txt`
puts `bundle exec upload-app --farm=bs --app=./build/fixture-r21.apk --app-id-file=build/bs-fixture-r21-url.txt`
puts `bundle exec upload-app --farm=bb --app=./build/fixture-r28.apk --app-id-file=build/fixture-r28-url.txt`
puts `bundle exec upload-app --farm=bs --app=./build/fixture-r28.apk --app-id-file=build/bs-fixture-r28-url.txt`
