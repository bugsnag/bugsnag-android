#!/usr/bin/env ruby

# Wait for device to finish booting
`adb wait-for-device`

while `adb shell getprop sys.boot_completed`.strip != "1"
  puts "Awaiting boot completion..."
  sleep 1
end

puts "Device Ready!"
