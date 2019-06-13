#!/usr/bin/env bash

# disable accelerometer-based rotation
adb shell content insert --uri content://settings/system --bind name:s:accelerometer_rotation --bind value:i:0

if [ -z "$DEVICE_ORIENTATION" ]; then
    echo No orientation set
    exit 1
fi

if [ "$DEVICE_ORIENTATION" = "portrait" ]; then
    adb shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:0
elif [ "$DEVICE_ORIENTATION" = "landscape-left" ]; then
    adb shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:1
elif [ "$DEVICE_ORIENTATION" = "landscape-right" ]; then
    adb shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:3
elif [ "$DEVICE_ORIENTATION" = "upside-down" ]; then
    adb shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:2
fi
