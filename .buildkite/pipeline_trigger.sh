#!/usr/bin/env sh

if [[ "$BUILDKITE_MESSAGE" == *"[full ci]"* ||
  "$BUILDKITE_BRANCH" == "main" ||
  "$BUILDKITE_PULL_REQUEST_BASE_BRANCH" == "main" ||
  ! -z "$FULL_SCHEDULED_BUILD" ]]; then
  # Full build
  buildkite-agent pipeline upload .buildkite/pipeline.full.yml

else
  # Basic build, but allow a full build to be triggered
  buildkite-agent pipeline upload .buildkite/block.step.yml
fi
