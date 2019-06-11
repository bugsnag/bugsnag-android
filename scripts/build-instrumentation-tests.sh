./scripts/install-ndk.sh

./gradlew
./gradlew sdk:assembleAndroidTest
./gradlew ndk:assembleAndroidTest
