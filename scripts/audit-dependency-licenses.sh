#!/bin/bash

if test -f "decisions.yml"; then
    decision="--decisions-file=decisions.yml"
else
    decision=""
fi

license_finder --enabled-package-managers=gradle $decision
license_finder --project-path=bugsnag-plugin-android-ndk/src/main/jni/deps/parson --enabled-package-managers=npm $decision
