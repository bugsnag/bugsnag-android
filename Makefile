all: build

.PHONY: build test clean bump release

build:
	./gradlew build -PABI_FILTERS=arm64-v8a,armeabi-v7a,x86,x86_64

clean:
	./gradlew clean

test:
	./gradlew connectedCheck

remote-test:
ifeq ($(BROWSER_STACK_USERNAME),)
	@$(error BROWSER_STACK_USERNAME is not defined)
endif
ifeq ($(BROWSER_STACK_ACCESS_KEY),)
	@$(error BROWSER_STACK_ACCESS_KEY is not defined)
endif
	@APP_LOCATION=/app/bugsnag-android-core/build/outputs/apk/androidTest/debug/bugsnag-android-core-debug-androidTest.apk \
	 INSTRUMENTATION_DEVICES='["Google Nexus 5-4.4", "Google Pixel-7.1", "Google Pixel 3-9.0"]' \
	 docker-compose up --build android-instrumentation-tests

BUILD_TASK ?= assembleRelease
MINIMAL_FIXTURE ?= false
TEST_FIXTURE_NDK_VERSION ?= 16.1.4479499
TEST_FIXTURE_NAME ?= fixture
USE_LEGACY_OKHTTP ?= false

notifier:
	# Build the notifier
	@./gradlew -PVERSION_NAME=9.9.9 assembleRelease publishToMavenLocal -x check

fixture-r19: notifier
	# Build the r19 test fixture
	@cd ./features/fixtures/mazerunner && ./gradlew -PTEST_FIXTURE_NDK_VERSION=19.2.5345600 \
               -PTEST_FIXTURE_NAME=fixture-r19.apk \
                assembleRelease -x check
	@ruby scripts/copy-build-files.rb release r19

fixture-r21: notifier
	# Build the r21 test fixture
	@cd ./features/fixtures/mazerunner && ./gradlew -PTEST_FIXTURE_NDK_VERSION=21.4.7075529 \
               -PTEST_FIXTURE_NAME=fixture-r21.apk \
               assembleRelease -x check
	@ruby scripts/copy-build-files.rb release r21

fixture-minimal: notifier
	# Build the minimal test fixture
	@cd ./features/fixtures/mazerunner && ./gradlew -PMINIMAL_FIXTURE=true \
	           -PTEST_FIXTURE_NDK_VERSION=17.2.4988734 \
               -PTEST_FIXTURE_NAME=fixture-minimal.apk \
               assembleRelease -x check
	@ruby scripts/copy-build-files.rb release minimal

fixture-debug: notifier
	# Build the minimal test fixture
	@cd ./features/fixtures/mazerunner && ./gradlew -PTEST_FIXTURE_NDK_VERSION=17.2.4988734 \
               -PTEST_FIXTURE_NAME=fixture-debug.apk \
               assembleDebug -x check
	@ruby scripts/copy-build-files.rb debug debug

example-app:
	@./gradlew assembleRelease publishToMavenLocal -x check
	# Build Android example app
	@cd ./examples/sdk-app-example/ && ./gradlew clean assembleRelease

bump:
ifneq ($(shell git diff --staged),)
	@git diff --staged
	@$(error You have uncommitted changes. Push or discard them to continue)
endif
ifeq ($(VERSION),)
	@$(error VERSION is not defined. Run with `make VERSION=number bump`)
endif
	@echo Bumping the version number to $(VERSION)
	@sed -i '' "s/bugsnag-android:.*\"/bugsnag-android:$(VERSION)\"/" examples/sdk-app-example/app/build.gradle
	@sed -i '' "s/bugsnag-plugin-android-okhttp:.*\"/bugsnag-plugin-android-okhttp:$(VERSION)\"/" examples/sdk-app-example/app/build.gradle
	@sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$(VERSION)/" gradle.properties
	@sed -i '' "s/var version: String = .*/var version: String = \"$(VERSION)\",/"\
	 bugsnag-android-core/src/main/java/com/bugsnag/android/Notifier.kt
	@sed -i '' "s/## TBD/## $(VERSION) ($(shell date '+%Y-%m-%d'))/" CHANGELOG.md

.PHONY: check
check:
	@./gradlew lint detekt ktlintCheck checkstyle
	@./scripts/run-cpp-check.sh
	@./scripts/run-clang-format-ci-check.sh