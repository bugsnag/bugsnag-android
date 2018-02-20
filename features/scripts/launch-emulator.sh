#!/usr/bin/env bash

if [ -z "$EMULATOR" ]; then
    echo EMULATOR environment variable is not set
    exit 1
fi

if [ -z "$ANDROID_HOME" ]; then
    echo ANDROID_HOME environment variable is not set
    exit 1
fi

echo "Launching $EMULATOR emulator"
$ANDROID_HOME/tools/emulator @$EMULATOR -no-boot-anim -noaudio -no-snapshot
