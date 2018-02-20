#!/usr/bin/env bash

if [ -z "$APP_BUNDLE" ]; then
    echo APP_BUNDLE environment variable is not set
    exit 1
fi

if [ -z "$APP_ACTIVITY" ]; then
    echo APP_ACTIVITY environment variable is not set
    exit 1
fi

echo "Killing any extant app process"
adb shell am force-stop "$APP_BUNDLE"

# TODO doesn't need to run for each step
# Clear any existing data
echo "Launching MainActivity"
adb shell clear "$APP_BUNDLE"


echo "Launching MainActivity with '$EVENT_TYPE'"
adb shell am start -n "$APP_BUNDLE/$APP_ACTIVITY" \
    --es EVENT_TYPE "$EVENT_TYPE" \
    --es BUGSNAG_PORT "$MOCK_API_PORT" \
    --es BUGSNAG_API_KEY "$BUGSNAG_API_KEY"

echo "Ran Android Test case"
