#!/bin/bash -e

mkdir -p build/$2

cp features/fixtures/mazerunner/app/build/outputs/apk/$1/$2.apk build/$2.apk

# copy the unstripped scenarios libs (for stack unwinding validation)
# grabs the newest files as the likely just built ones
LIB_CXX_BUGSNAG_64=$(dirname `ls -dt features/fixtures/mazerunner/cxx-scenarios-bugsnag/build/intermediates/cxx/*/*/obj/x86_64 | head -n 1`)
LIB_CXX_BUGSNAG_32=$(dirname `ls -dt features/fixtures/mazerunner/cxx-scenarios-bugsnag/build/intermediates/cxx/*/*/obj/armeabi-v7a | head -n 1`)
LIB_CXX_64=$(dirname `ls -dt features/fixtures/mazerunner/cxx-scenarios/build/intermediates/cxx/*/*/obj/x86_64 | head -n 1`)
LIB_CXX_32=$(dirname `ls -dt features/fixtures/mazerunner/cxx-scenarios/build/intermediates/cxx/*/*/obj/armeabi-v7a | head -n 1`)

cp $LIB_CXX_64/x86_64/libcxx-scenarios.so build/$2/libcxx-scenarios-x86_64.so
cp $LIB_CXX_32/x86/libcxx-scenarios.so build/$2/libcxx-scenarios-x86.so
cp $LIB_CXX_64/arm64-v8a/libcxx-scenarios.so build/$2/libcxx-scenarios-arm64.so
cp $LIB_CXX_32/armeabi-v7a/libcxx-scenarios.so build/$2/libcxx-scenarios-arm32.so
cp $LIB_CXX_BUGSNAG_64/x86_64/libcxx-scenarios-bugsnag.so build/$2/libcxx-scenarios-bugsnag-x86_64.so
cp $LIB_CXX_BUGSNAG_32/x86/libcxx-scenarios-bugsnag.so build/$2/libcxx-scenarios-bugsnag-x86.so
cp $LIB_CXX_BUGSNAG_64/arm64-v8a/libcxx-scenarios-bugsnag.so build/$2/libcxx-scenarios-bugsnag-arm64.so
cp $LIB_CXX_BUGSNAG_32/armeabi-v7a/libcxx-scenarios-bugsnag.so build/$2/libcxx-scenarios-bugsnag-arm32.so
