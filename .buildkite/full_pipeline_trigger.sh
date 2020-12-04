#!/usr/bin/env sh

if [[ "$BUILDKITE_MESSAGE" == *"[quick ci]"* ]]; then
  PIPELINE='pipeline.smoke.yml'
elif [[ "$BUILDKITE_MESSAGE" == *"[full ci]"* ||
  "$BUILDKITE_BRANCH" == "next" ||
  "$BUILDKITE_BRANCH" == "master" ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "next" ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "master" ]]; then
  PIPELINE='pipeline.full.yml'
else
  PIPELINE='pipeline.smoke.yml'
fi

buildkite-agent pipeline upload .buildkite/$PIPELINE

