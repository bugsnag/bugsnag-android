cd tests/features/fixtures/minimalapp

cp base.template build.gradle
cp app/base.template app/build.gradle

./gradlew clean

./gradlew assembleRelease

apksize=$(stat --printf="%s" app/build/outputs/apk/release/app-release-unsigned.apk)

./gradlew bundleRelease

java -jar bundletool.jar build-apks \
    --bundle=app/build/outputs/bundle/release/app.aab \
    --output=app/build/outputs/bundle/release/app.apks \
    --ks=app/fakekeys.jks \
    --ks-pass=pass:password \
    --ks-key-alias=password \
    --key-pass=pass:password

unzip -qq app/build/outputs/bundle/release/app.apks -d app/build/outputs/bundle/release

aabsize=$(stat --printf "%s" app/build/outputs/bundle/release/standalones/standalone-hdpi.apk)

./gradlew clean

cp bugsnag.template build.gradle
cp app/bugsnag.template app/build.gradle

./gradlew assembleRelease

apkbugsnagsize=$(stat --printf="%s" app/build/outputs/apk/release/app-release-unsigned.apk)

./gradlew bundleRelease

java -jar bundletool.jar build-apks \
    --bundle=app/build/outputs/bundle/release/app.aab \
    --output=app/build/outputs/bundle/release/app.apks \
    --ks=app/fakekeys.jks \
    --ks-pass=pass:password \
    --ks-key-alias=password \
    --key-pass=pass:password

unzip -qq app/build/outputs/bundle/release/app.apks -d app/build/outputs/bundle/release

aabbugsnagsize=$(stat --printf "%s" app/build/outputs/bundle/release/standalones/standalone-arm64_v8a_hdpi.apk)

echo "APK Bugsnag size $((apkbugsnagsize - apksize))"
echo "ARM64 v8a APK size $((aabbugsnagsize - aabsize))"