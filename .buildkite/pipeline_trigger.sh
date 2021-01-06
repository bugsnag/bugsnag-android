#!/usr/bin/env sh

if [[ "$BUILDKITE_MESSAGE" == *"[barebones ci]"* ]]; then
  echo "Running barebones build due to commit message\n"
  echo "Unit and static tests will be run\n"
  echo "Minimal instrumentation tests will be run\n"
  echo "End-to-end smoke tests will be run on minimum and maximum supported Android versions\n"
elif [[ "$BUILDKITE_MESSAGE" == *"[full ci]"* ||
  "$BUILDKITE_BRANCH" == "next" ||
  "$BUILDKITE_BRANCH" == "master" ]]; then
  echo "Running full build\n"
  echo "Unit and static tests will be run\n"
  echo "All instrumentation tests will be run\n"
  echo "All end-to-end tests will be run on all supported Android versions\n"
  buildkite-agent pipeline upload .buildkite/pipeline.full.yml
elif [[ "$BUILDKITE_MESSAGE" == *"[gated-full ci]"* ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "next" ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "master" ]]; then
  echo "Running gated-full build\n"
  echo "Unit and static tests will be run\n"
  echo "Minimal instrumentation tests will be run by default\n"
  echo "End-to-end smoke tests will be run on all supported Android versions by default\n"
  echo "If the full build is triggered this will:\n"
  echo "  Run the instrumentation tests against all supported Android versions\n"
  echo "  Run the full end-to-end tests on all supported Android versions\n"
  buildkite-agent pipeline upload .buildkite/pipeline.quick.yml .buildkite/block.step.yml
elif [[ "$BUILDKITE_MESSAGE" == *"[quick ci]"* ]]; then
  echo "Running quick build\n"
  echo "Unit and static tests will be run\n"
  echo "Minimal instrumentation tests will be run\n"
  echo "End-to-end smoke tests will be run on all supported Android versions\n"
  buildkite-agent pipeline upload .buildkite/pipeline.quick.yml
else
  echo "Running quick build\n"
fi

