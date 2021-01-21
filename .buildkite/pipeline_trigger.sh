#!/usr/bin/env sh

barebones_description() {
  echo "Running barebones build due to commit messages"
  echo "Unit and static tests will be run"
  echo "Minimal instrumentation tests will be run"
  echo "End-to-end smoke tests will be run on minimum and maximum supported Android versions"
}

if [[ "$BUILDKITE_MESSAGE" == *"[barebones ci]"* ]]; then
  barebones_description
elif [[ "$BUILDKITE_MESSAGE" == *"[full ci]"* ||
  "$BUILDKITE_BRANCH" == "next" ||
  "$BUILDKITE_BRANCH" == "master" ||
  ! -z "$FULL_SCHEDULED_BUILD" ]]; then
  echo "Running full build"
  echo "Unit and static tests will be run"
  echo "All instrumentation tests will be run"
  echo "All end-to-end tests will be run on all supported Android versions"
  # Add files in reverse as BK insert them in place - leading to them reversing in the resulting pipeline
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
  # Add files in reverse as BK insert them in place - leading to them reversing in the resulting pipeline
  buildkite-agent pipeline upload .buildkite/block.step.yml
  buildkite-agent pipeline upload .buildkite/pipeline.quick.yml
elif [[ "$BUILDKITE_MESSAGE" == *"[quick ci]"* ]]; then
  echo "Running quick build"
  echo "Unit and static tests will be run"
  echo "Minimal instrumentation tests will be run"
  echo "End-to-end smoke tests will be run on all supported Android versions"
  echo "All end-to-end tests will be run on a single supported Android versions"
  buildkite-agent pipeline upload .buildkite/pipeline.quick.yml
else
  barebones_description
fi

