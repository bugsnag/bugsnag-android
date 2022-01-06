#!/usr/bin/env bash

echo $KEY > ~/temp_key
base64 --decode ~/temp_key > /publishKey.gpg
echo "signing.keyId=$KEY_ID" >> ~/.gradle/gradle.properties
echo "signing.password=$KEY_PASS" >> ~/.gradle/gradle.properties
echo "signing.secretKeyRingFile=/publishKey.gpg" >> ~/.gradle/gradle.properties
echo "NEXUS_USERNAME=$PUBLISH_USER" >> ~/.gradle/gradle.properties
echo "NEXUS_PASSWORD=$PUBLISH_PASS" >> ~/.gradle/gradle.properties
echo "nexusUsername=$PUBLISH_USER" >> ~/.gradle/gradle.properties
echo "nexusPassword=$PUBLISH_PASS" >> ~/.gradle/gradle.properties

/app/gradlew assembleRelease publish --no-daemon --max-workers=1 && \
 echo "Go to https://oss.sonatype.org/ to release the final artefact. For the full release instructions, please read https://github.com/bugsnag/bugsnag-android/blob/next/docs/RELEASING.md"
