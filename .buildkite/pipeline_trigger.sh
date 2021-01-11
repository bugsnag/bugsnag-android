#!/usr/bin/env sh

if [[ "$BUILDKITE_MESSAGE" == *"[barebones ci]"* ]]; then
  echo "Running barebones build due to commit messages"
  echo "Unit and static tests will be run"
  echo "Minimal instrumentation tests will be run"
  echo "End-to-end smoke tests will be run on minimum and maximum supported Android versions"
elif [[ "$BUILDKITE_MESSAGE" == *"[full ci]"* ||
  "$BUILDKITE_BRANCH" == "next" ||
  "$BUILDKITE_BRANCH" == "master" ]]; then
  echo "Running full build"
  echo "Unit and static tests will be run"
  echo "All instrumentation tests will be run"
  echo "All end-to-end tests will be run on all supported Android versions"
  buildkite-agent pipeline upload .buildkite/pipeline.full.yml
  buildkite-agent pipeline upload .buildkite/pipeline.quick.yml
elif [[ "$BUILDKITE_MESSAGE" == *"[gated-full ci]"* ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "next" ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "master" ]]; then
  echo "Running gated-full build"
  echo "Unit and static tests will be run"
  echo "Minimal instrumentation tests will be run by default"
  echo "End-to-end smoke tests will be run on all supported Android versions by default"
  echo "If the full build is triggered this will:"
  echo "  Run the instrumentation tests against all supported Android versions"
  echo "  Run the full end-to-end tests on all supported Android versions"
  buildkite-agent pipeline upload .buildkite/block.step.yml
  buildkite-agent pipeline upload .buildkite/pipeline.quick.yml
elif [[ "$BUILDKITE_MESSAGE" == *"[quick ci]"* ]]; then
  echo "Running quick build"
  echo "Unit and static tests will be run"
  echo "Minimal instrumentation tests will be run"
  echo "End-to-end smoke tests will be run on all supported Android versions"
  buildkite-agent pipeline upload .buildkite/pipeline.quick.yml
else
  echo "Running barebones build by default"
  echo "Unit and static tests will be run"
  echo "Minimal instrumentation tests will be run"
  echo "End-to-end smoke tests will be run on minimum and maximum supported Android versions"
fi

