#!/usr/bin/env bash
./gradlew clean && infer --disable-issue-type "NULL_DEREFERENCE" \
--disable-issue-type "RESOURCE_LEAK" -- ./gradlew sdk:build
