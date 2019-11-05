cd tests/features/fixtures/minimalapp

cp base.template build.gradle
cp app/base.template app/build.gradle

./gradlew clean

./gradlew assembleRelease

basesize=$($ANDROID_HOME/tools/bin/apkanalyzer apk file-size app/build/outputs/apk/release/app-release-unsigned.apk)

./gradlew clean

cp bugsnag.template build.gradle
cp app/bugsnag.template app/build.gradle

./gradlew assembleRelease

totalsize=$($ANDROID_HOME/tools/bin/apkanalyzer apk file-size app/build/outputs/apk/release/app-release-unsigned.apk)

echo $basesize
echo $totalsize