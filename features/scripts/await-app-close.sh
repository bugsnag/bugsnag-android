#!/usr/bin/env bash

# Detect whether app is still in the foreground and wait
while [[ `adb shell dumpsys activity | grep "Proc # 0" | grep "$APP_BUNDLE"` ]]; do
    echo "Polling Android App"
    sleep 2
done
