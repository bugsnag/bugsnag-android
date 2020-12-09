#!/usr/bin/env sh

if [[ "$BUILDKITE_MESSAGE" == *"[quick ci]"* ]]; then
  echo "Running quick build due to commit message\n"
elif [[ "$BUILDKITE_MESSAGE" == *"[full ci]"* ||
  "$BUILDKITE_BRANCH" == "next" ||
  "$BUILDKITE_BRANCH" == "master" ]]; then
  echo "Running full build"
  buildkite-agent pipeline upload .buildkite/pipeline.full.yml
elif [[ "$BUILDKITE_MESSAGE" == *"[integration ci]"* ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "next" ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "master" ]]; then
  echo "Running integration build"
  buildkite-agent pipeline upload .buildkite/block.step.yml .buildkite/pipeline.full.yml
else
  echo "Running quick build"
fi

