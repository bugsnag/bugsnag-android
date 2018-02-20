#!/usr/bin/env bash

function poll_app() {
  # Detect whether app is still in the foreground (app kills its own process when completed)
  while [[ `adb shell dumpsys activity | grep "Proc # 0" | grep "com.bugsnag.android.mazerunner"` ]];
   do echo "Polling Android App"
   sleep 2
  done
}

echo "Killing any extant app process"
adb shell am force-stop com.bugsnag.android.mazerunner

# TODO doesn't need to run for each step
# Clear any existing data
echo "Launching MainActivity"
adb shell clear com.bugsnag.android.mazerunner


echo "Launching MainActivity with EVENT_TYPE: $EVENT_TYPE"
adb shell am start -n com.bugsnag.android.mazerunner/com.bugsnag.android.mazerunner.MainActivity --es EVENT_TYPE $EVENT_TYPE
poll_app

echo "Ran Android Test case"
