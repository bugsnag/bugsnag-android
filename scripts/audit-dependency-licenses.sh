#!/bin/bash

curl https://raw.githubusercontent.com/bugsnag/license-audit/master/config/decision_files/global.yml -o decisions.yml

license_finder --enabled-package-managers=gradle --decisions-file=decisions.yml
license_finder --project-path=bugsnag-plugin-android-ndk/src/main/jni/deps/parson --enabled-package-managers=npm --decisions-file=decisions.yml
