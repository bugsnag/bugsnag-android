#!/bin/bash -e

cd features/fixtures/minimalapp
rm -rf .git
ln -s ../../../.git
bundle install
bundle exec danger
rm -f .git
