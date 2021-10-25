  # Build the example app as the Espresso "target" (which is not actually used)
pushd examples/sdk-app-example
  ./gradlew clean assembleRelease
popd

# Build the test app
./gradlew assembleAndroidTest --stacktrace
