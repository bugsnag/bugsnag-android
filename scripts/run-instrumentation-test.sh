#!/bin/bash

timestamp() {
  date +"%T"
}

export APP_LOCATION=examples/sdk-app-example/app/build/outputs/apk/release/app-release.apk
export TEST_LOCATION=bugsnag-android-core/build/outputs/apk/androidTest/debug/bugsnag-android-core-debug-androidTest.apk

# First app.  This is not actually used, but must be present and different to the test app.
echo "Android Tests [$(timestamp)]: Starting instrumentation test run against devices: $INSTRUMENTATION_DEVICES"
echo "Android Tests [$(timestamp)]: Uploading first test app from $APP_LOCATION to BrowserStack"
app_response=$(curl -u "$BROWSER_STACK_USERNAME:$BROWSER_STACK_ACCESS_KEY" -X POST "https://api-cloud.browserstack.com/app-automate/upload" -F "file=@$APP_LOCATION")
app_url=$(echo "$app_response" | jq -r ".app_url")

if [ -z "$app_url" ]; then
    echo "Android Tests [$(timestamp)]: First app upload failed, exiting"
    echo "$app_response"
    exit 1
fi

echo "Android Tests [$(timestamp)]: First app upload successful, url: $app_url"

# Second app - the tests.
echo "Android Tests [$(timestamp)]: Uploading second test app from $TEST_LOCATION to BrowserStack"
test_response=$(curl -u "$BROWSER_STACK_USERNAME:$BROWSER_STACK_ACCESS_KEY" -X POST "https://api-cloud.browserstack.com/app-automate/espresso/test-suite" -F "file=@$TEST_LOCATION")
test_url=$(echo "$test_response" | jq -r ".test_url")

if [ -z "$test_url" ]; then
    echo "Android Tests [$(timestamp)]: Second app upload failed, exiting"
    echo "$test_response"
    exit 1
fi

echo "Android Tests [$(timestamp)]: Second app upload successful, url: $test_url"

echo "Android Tests [$(timestamp)]: Starting test run"
build_response=$(curl -X POST "https://api-cloud.browserstack.com/app-automate/espresso/build" -d \ "{\"devices\": $INSTRUMENTATION_DEVICES, \"app\": \"$app_url\", \"deviceLogs\" : true, \"testSuite\": \"$test_url\"}" -H "Content-Type: application/json" -u "$BROWSER_STACK_USERNAME:$BROWSER_STACK_ACCESS_KEY")

build_id=$(echo "$build_response" | jq -r ".build_id")

if [ -z "$build_id" ] || [ "$build_id" = "null" ]; then
    echo "Android Tests [$(timestamp)]: Test start failed, exiting"
    echo "$build_response"
    exit 1
fi

echo "Android Tests [$(timestamp)]: Test run creation successful, id: $build_id"

echo "Android Tests [$(timestamp)]: Waiting for test run to begin"
sleep 10 # Allow the tests to kick off

status_response=$(curl -s -u "$BROWSER_STACK_USERNAME:$BROWSER_STACK_ACCESS_KEY" -X GET https://api-cloud.browserstack.com/app-automate/espresso/builds/"$build_id")
status=$(echo "$status_response" | jq -r ".status")

WAIT_COUNT=0
until [ "$status" == "\"done\"" ] || [ "$status" == "\"error\"" ] || [ "$status" == "\"failed\"" ] || [ $WAIT_COUNT -eq 100 ]; do
    echo "Android Tests [$(timestamp)]: Current test status: $status, Time waited: $((WAIT_COUNT * 15))"
    ((WAIT_COUNT++))
    sleep 15
    status_response=$(curl -s -u "$BROWSER_STACK_USERNAME:$BROWSER_STACK_ACCESS_KEY" -X GET https://api-cloud.browserstack.com/app-automate/espresso/builds/"$build_id")
    status=$(echo "$status_response" | jq ".status")
done

if [ "$status" != "\"done\"" ]; then
    echo "Test error or timeout"
    exit 1
fi

echo "Android Tests [$(timestamp)]: Tests complete"

# Process results
NUMBER_OF_DEVICES=$(awk -F "," '{print NF}' <<< "$INSTRUMENTATION_DEVICES")
device_count=0
total_succeeded=0
total_failed=0
until [ $device_count -eq "$NUMBER_OF_DEVICES" ]; do
    device=$(echo "$status_response" | jq -r --arg dc $device_count '.input_capabilities.devices[$dc|tonumber]')
    device_status=$(echo "$status_response" | jq --arg dev "$device" '.devices[$dev]')
    failed=$(echo "$device_status" | jq '.test_status.FAILED')
    total_failed=$((total_failed + failed))
    succeeded=$(echo "$device_status" | jq '.test_status.SUCCESS')
    total_succeeded=$((total_succeeded + succeeded))
    if [ "$failed" -ne 0 ]; then
        echo "Android Tests [$(timestamp)]: $device had test failures, visit https://app-automate.browserstack.com/builds/$build_id for details"
    fi
    ((device_count++))
done

echo "Android Tests [$(timestamp)]: Total succeeded: $total_succeeded"
echo "Android Tests [$(timestamp)]: Total failed: $total_failed"
if [ $total_failed -ne 0 ];  then
  exit 1
fi
if [ $total_succeeded -eq 0 ];  then
  echo "No successful tests detected, this should not happen."
  exit 1
fi
