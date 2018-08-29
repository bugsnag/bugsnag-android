#!/usr/bin/env bash

if [ -z "$ANDROID_EMULATOR" ]; then
    echo EMULATOR environment variable is not set
    exit 1
fi

if [ -z "$ANDROID_HOME" ]; then
    echo ANDROID_HOME environment variable is not set
    exit 1
fi

echo "Launching $ANDROID_EMULATOR emulator"
$ANDROID_HOME/emulator/emulator @$ANDROID_EMULATOR -no-boot-anim -noaudio -no-snapshot -no-window

