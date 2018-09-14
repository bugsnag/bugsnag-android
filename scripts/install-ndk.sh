#!/usr/bin/env bash

if [ -z "$NDK_VERSION" ]; then
    echo "No NDK version set. Using default (r16b)."
    export NDK_VERSION=r16b
fi

curl --silent -L https://dl.google.com/android/repository/android-ndk-$NDK_VERSION-linux-x86_64.zip -O

unzip -qq android-ndk-$NDK_VERSION-linux-x86_64.zip > /dev/null

mv android-ndk-$NDK_VERSION $ANDROID_HOME/ndk-bundle
rm android-ndk-$NDK_VERSION-linux-x86_64.zip

export ANDROID_NDK_HOME=$ANDROID_HOME/ndk-bundle
export LOCAL_ANDROID_NDK_HOST_PLATFORM="linux-x86_64"
export PATH=${PATH}:${ANDROID_NDK_HOME}
