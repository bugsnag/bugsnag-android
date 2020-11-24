#!/usr/bin/env sh

if [[ "$BUILDKITE_MESSAGE" == *"[QUICK CI]"* ]]; then
  PIPELINE='pipeline.smoke.yml'
elif [[ "$BUILDKITE_MESSAGE" == *"[FULL CI]"* ||
  "$BUILDKITE_BRANCH" == "next" ||
  "$BUILDKITE_BRANCH" == "master" ]]; then
  PIPELINE='pipeline.full.yml'
else
  PIPELINE='pipeline.smoke.yml'
fi

echo $PIPELINE

