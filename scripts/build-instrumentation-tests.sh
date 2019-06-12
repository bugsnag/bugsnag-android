./scripts/install-ndk.sh

./gradlew --stacktrace
./gradlew sdk:assembleAndroidTest --stacktrace
