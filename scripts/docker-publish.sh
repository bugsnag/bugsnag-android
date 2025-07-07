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

/app/gradlew assembleRelease publish --no-daemon --max-workers=1

# === Close Staging Repository ===
echo "--- Closing staging repository"
echo "Fetching staging repositories..."

REPOS_JSON=$(curl -s -u "$PUBLISH_USER:$PUBLISH_PASS" \
  "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories")

if [[ -z "$REPOS_JSON" ]]; then
  echo "Failed to retrieve repository list. Check your credentials or network." >&2
  exit 1
fi

REPO_KEYS=($(echo "$REPOS_JSON" | jq -r '.repositories[] | select(.state == "open") | .key'))

if [[ "${#REPO_KEYS[@]}" -eq 0 ]]; then
  echo "No open repositories found."
  exit 1
elif [[ "${#REPO_KEYS[@]}" -gt 1 ]]; then
  echo "Multiple open repositories found. Please specify which one to close:"
  printf '%s\n' "${REPO_KEYS[@]}"
  exit 1
fi

REPO_KEY="${REPO_KEYS[0]}"
echo "Closing repository $REPO_KEY..."

URL="https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/$REPO_KEY?publishing_type=user_managed"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST -u "$PUBLISH_USER:$PUBLISH_PASS" "$URL")

BODY=$(echo "$RESPONSE" | sed -n '/^HTTP_STATUS:/!p')
STATUS=$(echo "$RESPONSE" | sed -n 's/^HTTP_STATUS://p')

if [[ "$STATUS" != "200" ]]; then
  echo "Failed to close repository. HTTP Status: $STATUS"
  echo "$BODY" | jq -r
  exit 1
fi

echo "Repository $REPO_KEY closed successfully."

echo "Go to https://oss.sonatype.org/ to release the final artefact."
echo "For full release instructions, visit:"
echo "https://github.com/bugsnag/bugsnag-android/blob/next/docs/RELEASING.md"
