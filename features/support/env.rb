# Configure app environment
bs_username = ENV['BROWSER_STACK_USERNAME']
bs_access_key = ENV['BROWSER_STACK_ACCESS_KEY']
bs_local_id = ENV['BROWSER_STACK_LOCAL_IDENTIFIER'] || 'maze_browser_stack_test_id'
bs_device = ENV['DEVICE_TYPE']
app_location = ENV['APP_LOCATION']

# Set this explicitly
$api_key = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345"

AfterConfiguration do |config|
  AppAutomateDriver.new(bs_username, bs_access_key, bs_local_id, bs_device, app_location)
  $driver.start_driver
end

After do |scenario|
  $driver.reset
  if scenario.failed?
    write_failed_requests_to_disk(scenario)
  end
end

def write_failed_requests_to_disk(scenario)
  Dir.chdir("/app/maze-output") do
    date = DateTime.now.strftime('%d%m%y%H%M%S%L')
    Server.stored_requests.each_with_index do |request, i|
      filename = "#{scenario.name}-request#{i}-#{date}.log"
      File.open(filename, 'w+') do |file|
        file.puts "URI: #{request[:request].request_uri}"
        file.puts "HEADERS:"
        request[:request].header.each do |key, values|
          file.puts "  #{key}: #{values.map {|v| "'#{v}'"}.join(' ')}"
        end
        file.puts
        file.puts "BODY:"
        file.puts JSON.pretty_generate(request[:body])
      end
    end
  end
end


at_exit do
  $driver.driver_quit
end