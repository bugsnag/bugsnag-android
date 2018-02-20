#!/usr/bin/env bash

echo "Uninstalling '$APP_BUNDLE'"
adb uninstall "$APP_BUNDLE"
echo "Installing '$APK_PATH'"
adb install "$APK_PATH"
