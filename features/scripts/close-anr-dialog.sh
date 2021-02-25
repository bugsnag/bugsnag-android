#!/usr/bin/env bash

tmp_file=/tmp/emulator_view.xml
adb pull $(adb shell uiautomator dump | awk '{ print $NF }') $tmp_file
if grep -q aerr_close "$tmp_file"; then
    # Find element with close button resource ID
    coords=$(perl -ne 'printf "%d %d\n", ($1+$3)/2, ($2+$4)/2 if /resource-id="android:id\/aerr_close"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/' "$tmp_file")
    adb shell input tap $coords
fi
