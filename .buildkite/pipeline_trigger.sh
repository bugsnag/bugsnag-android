#!/usr/bin/env sh

if [[ "$BUILDKITE_MESSAGE" == *"[full ci]"* ||
  "$BUILDKITE_BRANCH" == "master" ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "master" ||
  ! -z "$FULL_SCHEDULED_BUILD" ]]; then
  # Full build
  buildkite-agent pipeline upload .buildkite/pipeline.full.yml

else
  # Basic build, but allow a full build to be triggered
  buildkite-agent pipeline upload .buildkite/block.step.yml
fi

# Run BrowserStack steps unless instructed not to
if [[ "$BUILDKITE_MESSAGE" != *"[nobs]"* &&
      "$DEVICE_FARM" != *"NO_BS"* ]]; then
  buildkite-agent pipeline upload .buildkite/pipeline.bs.yml
fi

# Run BitBar steps if instructed to
if [[ "$BUILDKITE_MESSAGE" == *"[bb]"* ||
      "$DEVICE_FARM" == *"BB"* ]]; then
  buildkite-agent pipeline upload .buildkite/pipeline.bb.yml
fi
